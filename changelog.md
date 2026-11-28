Features
- Deployer and Extra Gauges now work together with Create: Factory Logistics 1.6.1 or newer ([#24](https://github.com/LIUKRAST/Deployer/issues/24)). Repackaged is still incompatible with it, see [fl.md](https://github.com/LIUKRAST/Deployer/blob/master/fl.md)
- New client option `capes` to turn capes (and the requests they need) off

Fixes
- Capes are fetched once per player and per game session, only when their cape is drawn, instead of at every player creation; downloads use a shared thread pool with timeouts and textures are registered on the render thread
- Pending fluid and energy promises were lost when a world or chunk was saved and loaded again, so gauges ordered the same resources twice
- Stock Keeper tabs of custom resources (fluids, energy) showed only the last 100 entries of a network with more than 100 different entries
- Packagers of custom resources no longer take hold of an item package when another mod only simulates inserting it, which could duplicate the package
- Two packagers of the same custom resource on the same inventory (for example two fluid packagers on one tank) counted it twice in the Stock Keeper and split orders between them, so part of the order was never sent. Like Create does for items, the network now counts each inventory once and asks one of its packagers
- Gauges ordering items together with fluids or energy sent the fluid and energy part even when an item packager was too busy, so that part was sent again on the next attempt
- `DeployerLanguageProvider.addShiftSummary` wrote the summary line under a wrong key, so the summary never showed (conditions and behaviours were fine)
- Restocking gauges of custom resources ordered at most 9 units of the selected scale per request (9 mB with a fluid gauge in mB); they now order the whole missing amount

For developers
- First stable release: the public API (everything outside `mixin` packages and `@ApiStatus.Internal` classes) stays compatible until 2.0
- Breaking changes, done before 1.0.0 so the API stays stable until 2.0:
  - `AbstractPanelBehaviour.read`, `write` and `writeSafe` are now `final`: override `easyRead`, `easyWrite` and `easyWriteSafe`
  - Removed the deprecated `PanelConnectionBuilder.put` (use `registerBoth`), `ColoredFactoryPanelSupportBehaviour`, `FPBEExtension`, `ACPExtension` and `DeployerClient.getCape`
  - The `mixin` packages and the `mixinExtensions` interfaces are marked `@ApiStatus.Internal`, except `LLBExtension`, which is the extension point for logistically linked behaviours of other mods
  - `AbstractPanelBehaviour.notifyOutputs` is now `final`; react to inputs in `notifiedFromInput`. The order and timing of target updates within a tick are not part of the contract
  - Stock Keeper tabs no longer depend on the Stock Ticker menu: `KeeperTabScreen`/`StockTabScreen` constructors and `ClientRegisterHelpers.registerStockKeeperTab` take only a `KeeperSourceContext`, which now provides the player (`getPlayer`). Other screens can host the same tabs with their own context (`ClientRegisterHelpers.getKeeperTabs` is public)
  - Removed `VersionedTrackerBehaviour`, which nothing used (packagers use Create's tracker together with `VersionedWrapper`)
  - `StockInventoryType.IStorageHandler.isBulky` now means what its name says (a package with a bulky entry holds nothing else, like Create's items that cannot fit inside containers); it was read inverted before. Types that returned `false` to get one entry per package must return `true`
  - `AbstractPackagerBlockEntity.isValidPackage` no longer has side effects; `unwrapBox` passes invalid boxes through only when not simulating
  - `AbstractInventorySummary.getStacksByCount` lists the largest count first, like Create's `InventorySummary`
  - The panel and stock inventory registries no longer have a default key: `getKey` of an unregistered entry returns `null`
- New `PanelConnection.notifySource(level, pos)`: blocks read through extra connections can call it after a change. It is optional today (panels still re-read extra sources periodically) and will make panels update right away once event-driven propagation lands, without API changes
- New `StockPanelBehaviour.getMaxRestockAmount`, to split restocks into smaller orders
- New public helpers replacing internal accessors: `CreateHelpers.getIconX`, `getIconY`, `iconVertex`, `getGenericRepackageHelper` and `ClientRegisterHelpers.getSelectedRequesterTab`
- `Constants` now extends `net.liukrast.constants.Constants` from Constants Manager (bundled with jarJar, loader independent): ids, logger, registry helpers and `translatable`/`literal`/`translationKey` live there, while `createDeferred`, `wrapWithShiftSummary` and the Catnip `LangBuilder` helpers stay in Deployer. code that uses Deployer's `Constants` compiles and runs unchanged
- Built against NeoForge 21.1.268, Create 6.0.11-346, Ponder 1.0.92, JEI 19.76.1.482 and MixinExtras 0.5.5; the minimum NeoForge (21.1.265) and Create (6.0.10) versions are unchanged
- The published POM no longer lists dev-only mods (Sable, Kotlin for Forge, Architectury, Observable, JEI runtime)
- Built against compat_manager 1.0.9: compatibility mixins (JEI, Create More: Package Couriers) are now skipped when that mod is not installed
- Mixins that Create Factory Logistics makes impossible to apply now live in `mixin.withoutfl` and are skipped when it is installed (new `DeployerMixinPlugin`, extends the compat_manager plugin)
- `PackageItemMixin`, `RequestPromiseQueueMixin` and the packager `unwrapBox` hook use priority 1100, so they apply on top of Factory Logistics overwrites
- Internal `StockKeeperRequestScreenAccessor.CategoryEntryAccessor` methods are prefixed with `deployer$` (they clashed with a Factory Logistics accessor)
