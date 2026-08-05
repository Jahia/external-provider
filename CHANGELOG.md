# external-provider Changelog

## 0.0.1

* **external-provider-ui**: Prevented startup deadlock by skipping manual bundle refreshes during the initial OSGi start-level transition. (#226)

* Render the mount-points and provider administration screens only for server administrators

* Escape the mount point name before using it in the JCR-SQL2 lookup query

* **external-provider-ui**: Fixed an issue where upgrading or restarting the module could freeze the instance by triggering unnecessary module refreshes. Refreshes now happen only when actually needed. Fixes Jahia/jahia-private#5156.
