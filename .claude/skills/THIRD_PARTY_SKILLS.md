# Third-party skills (vendored)

Copied verbatim from upstream on 2026-11-08 after inspection. They contain no hooks, no MCP servers and no executable scripts.
To update a skill, re-copy its folder from the same path upstream and bump the commit here.

| Skill | Source repo @ commit | Upstream path | Licence |
|---|---|---|---|
| glance-appwidget-updates-and-memory | android/skills @ bf68e2d | system/ | Apache-2.0 |
| glance-canonical-layouts | android/skills @ bf68e2d | system/ | Apache-2.0 |
| edge-to-edge | android/skills @ bf68e2d | system/ | Apache-2.0 |
| jetpack-compose-performance | android/skills @ bf68e2d | jetpack-compose/ | Apache-2.0 |
| navigation-3 | android/skills @ bf68e2d | navigation/ | Apache-2.0 |
| compose-animations | chrisbanes/skills @ 72628ae | skills/ | Apache-2.0 |
| compose-component-design | chrisbanes/skills @ 72628ae | skills/ | Apache-2.0 |
| compose-state-and-effects | chrisbanes/skills @ 72628ae | skills/ | Apache-2.0 |
| compose-ui-testing-patterns | chrisbanes/skills @ 72628ae | skills/ | Apache-2.0 |
| kotlin-concurrency-and-flow | chrisbanes/skills @ 72628ae | skills/ | Apache-2.0 |
| kotlin-api-design | chrisbanes/skills @ 72628ae | skills/ | Apache-2.0 |
| android-data-layer | rcosteira79/android-skills @ 0f9af5a | plugins/android-skills/skills/ | MIT |
| datastore | rcosteira79/android-skills @ 0f9af5a | plugins/android-skills/skills/ | MIT |
| android-testing | rcosteira79/android-skills @ 0f9af5a | plugins/android-skills/skills/ | MIT |
| android-debugging | rcosteira79/android-skills @ 0f9af5a | plugins/android-skills/skills/ | MIT |
| android-ux | rcosteira79/android-skills @ 0f9af5a | plugins/android-skills/skills/ | MIT |

Licence texts are in `_third-party-licenses/`.

Known quirks:
- `android-data-layer`, `datastore` and `android-debugging` mention sibling skills by `android-skills:<name>`. The ones not installed here (android-dev, android-retrofit, kmp-boundaries, kotlin-flows, compose) are intentionally omitted. Ignore those references.
- `android-ux` is Material 3-centric. Apply its touch-target, spacing and accessibility rules. For visual style, `nothing-design-system` and `docs/DESIGN_SPEC.md` take precedence.
