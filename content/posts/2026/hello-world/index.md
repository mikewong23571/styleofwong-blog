---
title: Hello Content System
created_at: 2026-04-19
status: published
published_at: 2026-04-19
---

This post is authored as plain Markdown.

Inline math works: $e^{i\pi} + 1 = 0$.

## Why this shape works

- Markdown stays author-owned.
- Metadata stays overrideable.
- Rendering stays replaceable.

```mermaid
graph TD
  Author[Markdown] --> Pipeline[CLJS Pipeline]
  Pipeline --> Astro[Astro Runtime]
```

## Authoring contract

| Layer | Owned by | Responsibility |
| --- | --- | --- |
| Content | Author | Markdown and adjacent assets |
| Metadata | Human + system | Tags, summary, overrides |
| Runtime | Astro | Rendering only |

```clojure
{:workflow [:edit-markdown :git-push]}
```

![Architecture sketch](./diagram.svg)
