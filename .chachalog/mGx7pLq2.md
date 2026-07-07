---
# Allowed version bumps: patch, minor, major
external-provider: patch
---

**external-provider-ui**: Prevent upgrade deadlock by turning the package-migration refresh into a proper migration step: it now defers while another revision of the module is still resolved/active (the module manager owns the re-wire during a version transition), and otherwise refreshes only the consumers that depend on the moved packages without being wired to this bundle — leaving already-wired consumers untouched so stop/start and redeploy are no-ops. Fixes Jahia/jahia-private#5156.
