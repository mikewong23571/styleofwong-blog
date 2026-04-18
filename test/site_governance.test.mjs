import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const testDir = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(testDir, "..");
const astroRoot = path.join(repoRoot, "site", "astro");
const astroSrcRoot = path.join(astroRoot, "src");
const runtimeReachabilityAllowlistFile = path.join(
  astroRoot,
  "governance",
  "runtime-reachability-allowlist.json"
);
const shadowConfig = fs.readFileSync(path.join(repoRoot, "shadow-cljs.edn"), "utf8");

const css = fs.readFileSync(
  path.join(repoRoot, "site", "astro", "src", "styles", "site.css"),
  "utf8"
);
const indexTemplate = fs.readFileSync(
  path.join(repoRoot, "site", "astro", "src", "pages", "index.astro"),
  "utf8"
);
const postTemplate = fs.readFileSync(
  path.join(repoRoot, "site", "astro", "src", "layouts", "PostLayout.astro"),
  "utf8"
);

function walkFiles(rootDir) {
  const entries = fs.readdirSync(rootDir, { withFileTypes: true });
  const files = [];

  for (const entry of entries) {
    const fullPath = path.join(rootDir, entry.name);

    if (entry.isDirectory()) {
      files.push(...walkFiles(fullPath));
      continue;
    }

    if (!entry.isFile() || entry.name.startsWith(".")) {
      continue;
    }

    files.push(fullPath);
  }

  return files;
}

function readRelativeReferences(file) {
  const source = fs.readFileSync(file, "utf8");
  const refs = new Set();
  const importPattern = /import\s+(?:[^"'`]+?\s+from\s+)?["']([^"'`]+)["']/g;

  for (const match of source.matchAll(importPattern)) {
    const ref = match[1];

    if (ref.startsWith(".")) {
      refs.add(ref);
    }
  }

  const frontmatterMatch = source.match(/^---\n([\s\S]*?)\n---/);

  if (frontmatterMatch) {
    const layoutMatch = frontmatterMatch[1].match(/^\s*layout:\s*["']([^"']+)["']/m);

    if (layoutMatch && layoutMatch[1].startsWith(".")) {
      refs.add(layoutMatch[1]);
    }
  }

  return [...refs];
}

function resolveLocalReference(fromFile, ref) {
  const candidate = path.resolve(path.dirname(fromFile), ref);
  const extensions = ["", ".astro", ".mdx", ".mjs", ".js", ".json", ".css"];

  for (const extension of extensions) {
    const resolved = `${candidate}${extension}`;

    if (fs.existsSync(resolved) && fs.statSync(resolved).isFile()) {
      return resolved;
    }
  }

  return null;
}

function runtimeFiles() {
  const runtimeDirs = [astroSrcRoot, path.join(astroRoot, "plugins")];

  return runtimeDirs.flatMap((dir) =>
    walkFiles(dir).filter((file) => /\.(astro|mdx|mjs|js|json|css)$/.test(file))
  );
}

function runtimeEntryFiles() {
  return [
    path.join(astroRoot, "astro.config.mjs"),
    ...walkFiles(path.join(astroSrcRoot, "pages")).filter((file) => /\.(astro|mdx)$/.test(file))
  ];
}

function readRuntimeReachabilityAllowlist() {
  const config = JSON.parse(fs.readFileSync(runtimeReachabilityAllowlistFile, "utf8"));
  return config.runtimeReachabilityAllowlist.map((relativePath) => path.join(repoRoot, relativePath));
}

function reachableRuntimeFiles() {
  const seen = new Set();
  const queue = [...runtimeEntryFiles()];

  while (queue.length > 0) {
    const file = queue.shift();

    if (seen.has(file)) {
      continue;
    }

    seen.add(file);

    for (const ref of readRelativeReferences(file)) {
      const resolved = resolveLocalReference(file, ref);

      if (resolved) {
        queue.push(resolved);
      }
    }
  }

  return seen;
}

test("design system exposes semantic token tiers instead of ad-hoc values", () => {
  assert.match(css, /--color-surface-page:/);
  assert.match(css, /--color-text-primary:/);
  assert.match(css, /--space-4:/);
  assert.match(css, /--radius-panel:/);
  assert.match(css, /--shadow-panel:/);
  assert.match(css, /--font-size-display:/);
  assert.match(css, /--layout-content-width:/);
});

test("shared frontend primitives exist for repetition and alignment", () => {
  assert.match(css, /\.panel\b/);
  assert.match(css, /\.stack-md\b/);
  assert.match(css, /\.cluster\b/);
  assert.match(css, /\.meta-eyebrow\b/);
});

test("palette stays in a restrained editorial range", () => {
  assert.match(css, /--color-paper-50:\s*#fbfbfa;/);
  assert.match(css, /--color-paper-100:\s*#f3f4f1;/);
  assert.match(css, /--color-highlight-100:\s*#e8ecef;/);
  assert.match(css, /--color-highlight-200:\s*#e3e8f0;/);
});

test("post taxonomy chips use centered inline-flex alignment", () => {
  assert.match(
    css,
    /\.post-taxonomy li,\s*\.tag-cloud li\s*\{[\s\S]*display:\s*inline-flex;[\s\S]*align-items:\s*center;/
  );
  assert.match(
    css,
    /\.prose\s+\.post-taxonomy li \+ li,\s*\.prose\s+\.tag-cloud li \+ li\s*\{[\s\S]*margin-top:\s*0;/
  );
});

test("home page keeps archive and tag dimensions in first-class sections", () => {
  assert.match(indexTemplate, /<section class="[^"]*\bhome-panel\b[^"]*\bhome-panel--primary\b[^"]*">/);
  assert.match(indexTemplate, /<section class="[^"]*\bhome-panel\b[^"]*\bhome-panel--archive\b[^"]*">/);
  assert.match(indexTemplate, /<section class="[^"]*\bhome-panel\b[^"]*\bhome-panel--taxonomy\b[^"]*">/);
});

test("post layout reserves a dedicated metadata header above prose", () => {
  assert.match(postTemplate, /<header class="post-header stack-md">/);
  assert.match(postTemplate, /class="meta-eyebrow"/);
  assert.match(postTemplate, /class="post-taxonomy cluster"/);
});

test("repository build config does not keep experimental spike entrypoints alive", () => {
  assert.doesNotMatch(shadowConfig, /:spike\b/);
  assert.doesNotMatch(shadowConfig, /spike\.build\/main/);
  assert.equal(fs.existsSync(path.join(repoRoot, "src", "spike")), false);
});

test("runtime reachability allowlist exists and only names real runtime files", () => {
  const allowlisted = readRuntimeReachabilityAllowlist();
  const normalized = allowlisted.map((file) => path.relative(repoRoot, file));

  assert.deepEqual(normalized, [...new Set(normalized)].sort());

  for (const file of allowlisted) {
    const isRuntimeFile = runtimeFiles().includes(file);
    assert.equal(isRuntimeFile, true, `${path.relative(repoRoot, file)} is not a managed runtime file`);
  }
});

test("astro runtime source files stay reachable from managed entrypoints", () => {
  const reachable = reachableRuntimeFiles();
  const allowlisted = new Set(readRuntimeReachabilityAllowlist());
  const orphaned = runtimeFiles()
    .filter((file) => !reachable.has(file) && !allowlisted.has(file))
    .map((file) => path.relative(repoRoot, file))
    .sort();

  assert.deepEqual(orphaned, []);
});
