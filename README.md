# Mike Wong Blog

Source repository for [blog.styleofwong.com](https://blog.styleofwong.com).

This repo exists to keep writing, publishing, and site operations in one place without turning day-to-day blog maintenance into framework work. The authoring target is simple: write Markdown, commit, push.

## What This Repository Is For

- Publishing Mike Wong's personal writing and notes.
- Keeping content, metadata, and site presentation under version control.
- Providing a stable local workflow for preview, verification, and deployment.
- Enforcing a few explicit governance rules so the project stays maintainable as it grows.

## Operating Model

- `content/` is the primary authoring surface.
- `metadata/` stores structured metadata that should not be mixed into prose.
- `system/` owns the publishing pipeline and managed outputs.
- `site/astro/` is the presentation runtime, not the source of truth for content.

The intended workflow is boring by design:

1. Edit content.
2. Preview locally.
3. Run checks.
4. Push to `main`.

## Maintenance Principles

- Content first: prose and assets should stay easy to author and easy to move.
- Thin runtime: presentation should serve content, not redefine content rules.
- Explicit metadata: tags, summaries, and other structured fields should remain inspectable and reviewable.
- Predictable growth: prefer small, reviewable changes over clever abstractions.
- Executable governance: if a rule matters repeatedly, enforce it with code or CI instead of tribal knowledge.

## Common Commands

```sh
bb dev
bb preview
bb test
bb deploy
```

Equivalent npm entry points also exist, but `bb` is the intended top-level interface for routine maintenance.

## Deployment

Production deploys target Cloudflare Pages through GitHub Actions.

Local Cloudflare credentials are expected in `.envrc` for maintenance tasks that need them. Repository and deployment secrets should be treated as operational configuration, not committed documentation.
