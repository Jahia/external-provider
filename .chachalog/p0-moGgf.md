---
external-provider: minor
---

Fixed lookups by identifier of external content that failed on other cluster nodes after a rename or a move.

On a cluster that runs 4.10.0, such a lookup failed for up to 8 hours. The `org.jahia.modules.external.cfg` file that 4.10.0 added is no longer read.
