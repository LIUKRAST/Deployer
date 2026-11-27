Fixes
- Pending fluid and energy promises were lost when a world or chunk was saved and loaded again, so gauges ordered the same resources twice

For developers
- Mixins that Create Factory Logistics makes impossible to apply now live in `mixin.withoutfl` and are skipped when it is installed (new `DeployerMixinPlugin`, extends the compat_manager plugin)
- `PackageItemMixin`, `RequestPromiseQueueMixin` and the packager `unwrapBox` hook use priority 1100, so they apply on top of Factory Logistics overwrites
- Internal `StockKeeperRequestScreenAccessor.CategoryEntryAccessor` methods are prefixed with `deployer$` (they clashed with a Factory Logistics accessor)
