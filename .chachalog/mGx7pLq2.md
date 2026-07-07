---
# Allowed version bumps: patch, minor, major
external-provider: patch
---

**external-provider-ui**: Prevent upgrade deadlock by turning the package-migration refresh into a per-consumer, self-correcting migration step driven by the live wiring graph. A consumer wired to any revision of this module (this one or an older sibling still present during an upgrade) is left untouched — so the module manager owns the re-wire during a version transition and there is no competing refresh to deadlock on (#5156), and stop/start and redeploy are no-ops. Only consumers wired to a foreign/stale revision, or declaring an unwired import of the moved packages (e.g. an optional import unresolved because this module was installed later), are refreshed. Fixes Jahia/jahia-private#5156.
