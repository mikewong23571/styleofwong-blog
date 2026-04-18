# Content-First Blog System Design

## Result

The blog system should be owned by a ClojureScript control plane and treat Astro as a managed rendering runtime.

Author workflow target:

- Open editor
- Write or edit Markdown under `content/`
- `git push`

Everything else is system work: metadata derivation, validation, route generation, Astro runtime management, build, and deployment.

This design was not left at the paper stage. It was iterated through small spikes until the critical uncertainty points were either verified or explicitly constrained.

## Design Goals

- Keep authoring centered on plain Markdown, not Astro.
- Keep blog rules defined once in one place.
- Let ClojureScript manage Astro versioning, feature flags, and generated site artifacts.
- Preserve a stable truth model:
  - Markdown is the primary source of truth.
  - Derived metadata is stored separately and can be overridden.
  - Human overrides always win over regenerated metadata.
- Keep local filesystem organization boring and stable.

## Non-Goals

- Building a generic static site framework.
- Letting Astro define content taxonomy or lifecycle rules.
- Hiding all complexity inside implicit runtime magic.
- Dynamic LLM generation during page render.

## Architecture

### 1. Content Layer

This is the only layer the author touches directly.

Structure:

```text
content/
  posts/
    2026/
      hello-world/
        index.md
        diagram.svg
  notes/
  pages/
```

Rules:

- One content item per directory.
- Directory name is the stable slug.
- Year bucket is for storage only, not URL semantics.
- Assets live beside the Markdown source.
- File paths do not encode tags, lifecycle state, or series membership.

### 2. Metadata Layer

This stores derived and overrideable metadata separately from author Markdown.

Examples:

- tags
- summary
- related-post hints
- LLM suggestions
- human overrides

Shape:

```text
metadata/
  posts.edn
```

Rules:

- Markdown remains primary truth.
- LLM output is materialized, never ephemeral.
- Human overrides are separate and non-destructive.
- Build never silently replaces human overrides.

### 3. Domain Pipeline

Owned by ClojureScript.

Responsibilities:

- Parse source Markdown and minimal frontmatter.
- Normalize content into domain objects.
- Merge derived metadata and overrides.
- Compute stable URLs independent of storage path.
- Transform author-oriented syntax into target-ready content when required.
- Emit domain IR and publish IR.

The current spike used `shadow-cljs` targeting Node to prove that this control plane can own filesystem traversal, metadata merging, asset copying, and target generation.

### 4. Publish IR

Two IRs are useful.

`domain-ir`
- Content-centric.
- Expresses `post`, `note`, `page`, dates, status, tags, summary, source path, and canonical URL.
- Does not know Astro internals.

`publish-ir`
- Renderer-centric.
- Expresses pages, indexes, site metadata, runtime feature flags, and asset mappings.
- Stable input contract for rendering runtimes.

Current emitted artifacts from the spike:

- `system/generated/domain-ir.edn`
- `system/generated/publish-ir.edn`
- managed Astro content pages
- managed runtime config JSON

### 5. Managed Astro Runtime

Astro stays in the system, but only as a rendering runtime.

Astro responsibilities:

- Render generated MDX/pages.
- Apply markdown plugins and theme/layout logic.
- Produce static site output.
- Handle content presentation features such as math, Mermaid, and SVG display.

ClojureScript responsibilities over Astro:

- Lock Astro and integration versions as a tested runtime set.
- Emit runtime feature flags.
- Generate Astro-consumable pages/data.
- Control what parts of the Astro tree are managed and reproducible.

Astro must not become the owner of:

- content taxonomy
- lifecycle rules
- tag semantics
- source-of-truth metadata

## Managed Boundary

The correct boundary is not “generate the whole Astro project every time.”

The correct boundary is:

- Keep a thin checked-in Astro runtime skeleton.
- Let ClojureScript generate only managed artifacts:
  - pages
  - data files
  - runtime config
  - copied public assets

This keeps Astro replaceable while avoiding the complexity of generating an entire frontend codebase.

## Spike Loop Summary

### Loop 1: End-to-End Feasibility

Goal:

- Prove `Markdown -> CLJS pipeline -> Astro -> static HTML`.

Spike:

- Added a minimal source post containing math, Mermaid, and a local SVG.
- Added a `shadow-cljs` Node-targeted pipeline.
- Added a minimal Astro site consuming generated artifacts.

Result:

- End-to-end build succeeded.

Decision:

- The architecture is feasible with ClojureScript as the control plane and Astro as a thin runtime.

### Loop 2: Runtime Version Management

Goal:

- Verify whether Astro ecosystem packages can be treated loosely.

Spike:

- Attempted install with an incorrect `@astrojs/mdx` range.

Result:

- Installation failed immediately because the integration version was invalid.

Decision:

- Astro runtime dependencies must be managed as a tested version matrix, not loose assumptions.
- Version governance belongs in the control plane.

### Loop 3: Node Module Boundary

Goal:

- Verify whether the CLJS control plane can coexist with an ESM-oriented Node workspace.

Spike:

- Ran `shadow-cljs` output inside an ESM package.

Result:

- The build output failed because `shadow-cljs` node-script output expected CommonJS semantics.

Decision:

- The CLJS control plane needs an explicit module boundary.
- In the spike, emitting `.cjs` fixed the contract.
- Final design should keep runtime boundaries explicit instead of assuming universal ESM compatibility.

### Loop 4: Generated Content Correctness

Goal:

- Verify that the content transform stage is structurally reliable.

Spike:

- Generated MDX from Markdown and inspected IR/output.

Result:

- A parameter-order bug caused content to collapse to the slug while still producing superficially valid output.

Decision:

- The transform pipeline must be staged explicitly:
  - source parse
  - normalized document
  - target adaptation
- “Just a few regex replacements” is not a reliable long-term design.

### Loop 5: Runtime Control by Generated Config

Goal:

- Verify that Astro runtime behavior can be controlled by ClojureScript-generated config instead of hand-edited Astro config.

Spike:

- Added `managed/runtime-config.json` generated by the CLJS pipeline.
- Made `astro.config.mjs` read feature flags from that generated config.

Result:

- Astro build consumed generated config successfully.

Decision:

- Runtime flags belong in the control plane and are passed into Astro through generated config, not duplicated manual edits.

### Loop 6: Mermaid Rendering Strategy

Goal:

- Verify whether Mermaid can be treated like normal SSR output in Astro static build.

Spike:

- Tried server-side Mermaid rendering from an Astro component.

Result:

- Build failed because Mermaid expected browser DOM APIs.

Decision:

- Mermaid support is feasible, but not as naive SSR.
- The default strategy should be explicit:
  - client-side Mermaid rendering by component, or
  - separate pre-render strategy with a DOM-capable renderer.
- For low-maintenance default behavior, client-side Mermaid is acceptable.

### Loop 7: Bundle Cost Signal

Goal:

- Observe runtime impact of Mermaid support.

Spike:

- Rebuilt using client-side Mermaid.

Result:

- Build succeeded, but Vite warned about large chunks caused by Mermaid packaging.

Decision:

- Mermaid must be treated as an opt-in page capability, not a free global default.
- The runtime should lazy-load or narrowly scope Mermaid support.

## Final Design Decisions

### Source of Truth

- Markdown content is the primary truth.
- Derived metadata is stored separately.
- Human overrides beat LLM output.

### Filesystem Policy

- Use year buckets for storage only.
- Keep content types in separate roots.
- Do not encode taxonomy or lifecycle into file paths.

### Control Plane

- Use ClojureScript as the system owner.
- Use a Node-targeted CLJS build for local/CI generation tasks.
- Treat Astro runtime dependencies as governed data, not ad hoc package choices.

### Renderer Contract

- Publish IR is the stable contract between system and renderer.
- Astro consumes managed artifacts produced from IR.
- Astro is allowed to render, not to define content rules.

### Governance Model

Use ClojureScript as an executable governance layer, not just as a build script.

That governance should operate at two levels.

`project governance`
- Own the approved Astro runtime matrix.
- Enforce the boundary between handwritten runtime code and generated artifacts.
- Ensure generated files are reproducible from source plus metadata.
- Fail builds when managed outputs are stale or runtime config drifts from the declared manifest.

`content governance`
- Enforce source layout invariants.
- Enforce minimal frontmatter schema.
- Enforce slug uniqueness and canonical URL stability.
- Enforce tag registry, alias rules, and deprecation rules.
- Enforce lifecycle invariants such as `published_at` only for published posts.
- Enforce asset locality and broken local reference checks.
- Enforce that LLM-derived metadata is materialized and that human overrides are not overwritten.

The governance engine should classify rules explicitly:

- `error`: hard fail in CI and deployment build
- `warn`: visible report, does not block build
- `fixable`: machine-rewritable issue with deterministic autofix

The key design choice is that governance runs over normalized IR, not raw ad hoc file scans. Source files are parsed into domain IR first, then governance rules evaluate that IR and emit a machine-readable report.

### Feature Policy

- Math: good default fit in Astro via markdown plugin chain.
- SVG: straightforward via copied/public asset mapping.
- Mermaid: supported, but explicitly costly and non-SSR by default.

## Recommended Repository Shape

```text
content/
  posts/
  notes/
  pages/

metadata/
  posts.edn

system/
  config/
  generated/
  reports/
  src/
    blog/
      governance/

site/
  astro/
    managed/
    src/
    public/

docs/
  superpowers/
    specs/
```

For the spike, this structure lives under `spikes/` to keep experiments disposable.

## Build Flow

```text
author edits Markdown
-> git push
-> CI or local build runs CLJS pipeline
-> pipeline emits domain IR + publish IR + managed Astro artifacts
-> Astro builds static site
-> Cloudflare deploys
```

The author does not manually run tagging, indexing, or Astro page generation steps.

## Governance Execution Flow

```text
content + metadata + system config
-> parse source
-> build domain IR
-> run governance rules against IR
-> emit governance report
-> fail or continue
-> build publish IR
-> emit managed Astro artifacts
-> Astro build
```

The important point is order: governance runs before managed artifact emission is treated as successful output. A blog post that violates schema or tag policy should fail before Astro becomes relevant.

## Governance Responsibilities For ClojureScript

### Project Governance

Recommended rule set:

- runtime matrix rule
  - `system/config/blog.edn` is the only source of approved Astro and integration versions
  - `site/astro/package.json` must match generated runtime manifest
- managed boundary rule
  - files under `site/astro/managed/` and generated page/data directories are machine-owned
  - handwritten edits to those files are rejected
- reproducibility rule
  - running the CLJS pipeline on a clean checkout must recreate the same managed artifacts
- config completeness rule
  - every enabled runtime feature must map to a known Astro-side implementation path

### Content Governance

Recommended rule set:

- path rule
  - content must live under approved roots
  - each item must be `.../<year>/<slug>/index.md` for posts/notes or `pages/<slug>/index.md`
- frontmatter rule
  - only approved keys are allowed in author frontmatter
  - required fields are present
- slug rule
  - slug comes from directory name
  - duplicate canonical URLs are errors
- lifecycle rule
  - `status=draft` cannot have `published_at`
  - `status=published` must have `published_at`
- taxonomy rule
  - tags must exist in the registry
  - deprecated tags must be rewritten through aliases or fail
- asset rule
  - relative asset links must resolve inside the content item directory
- metadata provenance rule
  - derived metadata must record origin such as `:llm`, `:manual`, or `:imported`
  - manual overrides must dominate all generated values

### LLM Governance

LLM output should be governed like any other imported data.

- It is never authoritative by default.
- It must be materialized into metadata files before build use.
- It must carry provenance and generation time.
- It can produce warnings or suggestions, but not mutate author truth during build.
- If accepted by a human, that acceptance becomes durable metadata, not a hidden rerun of the model.

## Implementation Constraints To Preserve

- Keep generated Astro artifacts clearly segregated from handwritten Astro runtime code.
- Do not let source Markdown drift toward Astro-specific frontmatter.
- Keep metadata override format inspectable and diff-friendly.
- Keep the CLJS pipeline deterministic without hidden online generation during build.
- Keep Mermaid loading scoped so non-Mermaid pages do not pay the full runtime cost.
- Keep governance rules explicit, typed, and reportable instead of embedding checks across unrelated build code.

## What Is Verified vs Deferred

Verified:

- CLJS can own the filesystem-driven content pipeline.
- Markdown plus separate metadata can compile into Astro-managed artifacts.
- Source storage path can differ from public URL.
- Astro runtime can be driven by generated config.
- Math, Mermaid, and SVG can coexist in the target rendering model.

Deferred:

- Full taxonomy model (`series`, `stale-review`, `evergreen`, etc.).
- Search indexing.
- RSS/sitemap generation.
- Theme packaging and upgrade policy.
- Cloudflare deployment automation details.
- LLM-assisted metadata generation workflow and governance.

These are deferred because they are additive and no longer block the core architecture.

## Recommended Next Step

Move from spike to implementation planning with these first slices:

1. Replace spike-only paths with real repository structure.
2. Formalize source/frontmatter schema and metadata override schema.
3. Separate transform stages into explicit modules instead of inline regex-heavy flow.
4. Add validation and failure reporting before page emission.
5. Add a minimal CI build path that matches the intended `git push` workflow.
