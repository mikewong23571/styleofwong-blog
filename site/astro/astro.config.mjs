import { defineConfig } from "astro/config";
import mdx from "@astrojs/mdx";
import fs from "node:fs";
import path from "node:path";
import rehypeAutolinkHeadings from "rehype-autolink-headings";
import rehypeKatex from "rehype-katex";
import rehypeSlug from "rehype-slug";
import remarkGfm from "remark-gfm";
import remarkMath from "remark-math";
import { remarkMermaidCode } from "./plugins/remark-mermaid-code.mjs";

const runtime = JSON.parse(
  fs.readFileSync(path.resolve("managed/runtime-config.json"), "utf8")
);

const remarkPlugins = [
  remarkGfm,
  runtime.astro.features.math ? remarkMath : null,
  runtime.astro.features.mermaid ? remarkMermaidCode : null
].filter(Boolean);

const rehypePlugins = [
  runtime.astro.features.math ? rehypeKatex : null,
  rehypeSlug,
  [
    rehypeAutolinkHeadings,
    {
      behavior: "append",
      properties: {
        className: ["heading-anchor"],
        ariaLabel: "Jump to section"
      },
      content: {
        type: "text",
        value: "#"
      }
    }
  ]
].filter(Boolean);

export default defineConfig({
  integrations: [
    mdx({
      gfm: true,
      syntaxHighlight: "shiki",
      shikiConfig: {
        themes: {
          light: "github-light",
          dark: "github-dark"
        }
      },
      remarkPlugins,
      rehypePlugins
    })
  ],
  markdown: {
    gfm: true,
    syntaxHighlight: "shiki",
    shikiConfig: {
      themes: {
        light: "github-light",
        dark: "github-dark"
      }
    },
    remarkPlugins,
    rehypePlugins
  }
});
