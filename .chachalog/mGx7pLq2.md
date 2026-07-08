---
# Allowed version bumps: patch, minor, major
external-provider: patch
---

**external-provider-ui**: Fixed an issue where upgrading or restarting the module could freeze the instance by triggering unnecessary module refreshes. Refreshes now happen only when actually needed. Fixes Jahia/jahia-private#5156.
