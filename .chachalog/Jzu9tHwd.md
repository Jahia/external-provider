---
external-provider: patch
---

Removed the identifier cache added in 4.10.0, which could break references to external content on other cluster nodes.

Its configuration file `org.jahia.modules.external.cfg` is no longer read.
