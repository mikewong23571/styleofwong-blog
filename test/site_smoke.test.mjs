import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const testDir = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(testDir, "..");
const astroRoot = path.join(repoRoot, "site", "astro");

test("astro build emits stylesheet links for index and post pages", () => {
  execFileSync("npm", ["run", "build:system"], { cwd: repoRoot, stdio: "inherit" });
  execFileSync("npm", ["run", "build"], { cwd: astroRoot, stdio: "inherit" });

  const indexHtml = fs.readFileSync(path.join(astroRoot, "dist", "index.html"), "utf8");
  const postHtml = fs.readFileSync(
    path.join(astroRoot, "dist", "posts", "hello-world", "index.html"),
    "utf8"
  );

  assert.match(indexHtml, /<link[^>]+href="\/_astro\/[^"]+\.css"/);
  assert.match(postHtml, /<link[^>]+href="\/_astro\/[^"]+\.css"/);
  assert.match(indexHtml, /Content-first publishing runtime/);
  assert.match(indexHtml, /Archive/);
  assert.match(indexHtml, /Tags/);
  assert.match(postHtml, /<pre class="mermaid">/);
  assert.match(postHtml, /graph TD/);
  assert.match(postHtml, /CLJS Pipeline/);
  assert.match(postHtml, /<script type="module" src="\/_astro\/[^"]+\.js"><\/script>/);
  assert.doesNotMatch(postHtml, /aria-roledescription="flowchart/);
  assert.match(postHtml, /class="heading-anchor"/);
  assert.match(postHtml, /<table>/);
  assert.doesNotMatch(postHtml, /data-language="mermaid"/);
});

test("mermaid runtime stays scoped to mermaid pages", () => {
  execFileSync("npm", ["run", "build:system"], { cwd: repoRoot, stdio: "inherit" });
  execFileSync("npm", ["run", "build"], { cwd: astroRoot, stdio: "inherit" });

  const indexHtml = fs.readFileSync(path.join(astroRoot, "dist", "index.html"), "utf8");
  const postHtml = fs.readFileSync(
    path.join(astroRoot, "dist", "posts", "hello-world", "index.html"),
    "utf8"
  );

  assert.doesNotMatch(indexHtml, /<script type="module" src="\/_astro\/[^"]+\.js"><\/script>/);
  assert.match(postHtml, /<script type="module" src="\/_astro\/[^"]+\.js"><\/script>/);
});
