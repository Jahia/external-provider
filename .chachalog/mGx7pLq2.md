---
# Allowed version bumps: patch, minor, major
external-provider: patch
---

**external-provider-ui**: Prevent upgrade deadlock by making the package-migration refresh a live-wiring check — it now runs only while a consumer is still wired to a foreign revision, and defers while a version transition is in flight. Fixes Jahia/jahia-private#5156.
