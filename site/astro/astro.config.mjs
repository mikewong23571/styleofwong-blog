import { defineConfig } from "astro/config";
import mdx from "@astrojs/mdx";
import fs from "node:fs";
import path from "node:path";
import rehypeKatex from "rehype-katex";
import remarkMath from "remark-math";

const runtime = JSON.parse(
  fs.readFileSync(path.resolve("managed/runtime-config.json"), "utf8")
);

export default defineConfig({
  integrations: [mdx()],
  markdown: {
    remarkPlugins: runtime.astro.features.math ? [remarkMath] : [],
    rehypePlugins: runtime.astro.features.math ? [rehypeKatex] : []
  }
});
