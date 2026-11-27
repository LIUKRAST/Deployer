Features
- Deployer and Extra Gauges now work together with Create: Factory Logistics 1.6.1 or newer ([#24](https://github.com/LIUKRAST/Deployer/issues/24)). Repackaged is still incompatible with it, see [fl.md](https://github.com/LIUKRAST/Deployer/blob/master/fl.md)

Fixes
- Pending fluid and energy promises were lost when a world or chunk was saved and loaded again, so gauges ordered the same resources twice

For developers
- Built against compat_manager 1.0.8: compatibility mixins (JEI, Create More: Package Couriers) are now skipped when that mod is not installed
- Mixins that Create Factory Logistics makes impossible to apply now live in `mixin.withoutfl` and are skipped when it is installed (new `DeployerMixinPlugin`, extends the compat_manager plugin)
- `PackageItemMixin`, `RequestPromiseQueueMixin` and the packager `unwrapBox` hook use priority 1100, so they apply on top of Factory Logistics overwrites
- Internal `StockKeeperRequestScreenAccessor.CategoryEntryAccessor` methods are prefixed with `deployer$` (they clashed with a Factory Logistics accessor)
