import { defineConfig } from "astro/config";
import mdx from "@astrojs/mdx";
import remarkMath from "remark-math";
import rehypeKatex from "rehype-katex";
import fs from "node:fs";
import path from "node:path";

const runtimeConfigPath = path.resolve("./spikes/astro-adapter/site/managed/runtime-config.json");
const runtimeConfig = JSON.parse(fs.readFileSync(runtimeConfigPath, "utf8"));
const mathEnabled = runtimeConfig.astro.features.math;

export default defineConfig({
  integrations: [mdx()],
  markdown: {
    remarkPlugins: mathEnabled ? [remarkMath] : [],
    rehypePlugins: mathEnabled ? [rehypeKatex] : []
  }
});
