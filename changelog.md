Features
- ValueEditBox - Value Settings with text box
- Step size for StockInventoryType scroll values

Performance
- Ordering and stock panels no longer invalidate the whole network inventory cache every tick (~30x lower panel tick cost measured on a 16-packager network) ([CreateExtraGauges#89](https://github.com/LIUKRAST/CreateExtraGauges/issues/89))
- Factory gauges only collect their stock connections when a request is actually due, instead of every tick
- Faster lookup of connected panel values

Fixes
- Gauge connections won't work on sublevels ([#25](https://github.com/LIUKRAST/Deployer/issues/25))
- Custom stock types now query the target inventory from the correct side ([#22](https://github.com/LIUKRAST/Deployer/issues/22))
- Wrong bulb models and glow tinting
- Factory gauges ignored a redstone change when a number or address input changed in the same update
- A panel stopped notifying its remaining targets when one of them was unloaded or was a powered display link
- Stock panels invalidated the item cache instead of the cache of their own stock type
- Custom panels no longer disappear when copied with schematics or blueprints
- Extra panel connections no longer load chunks while checking their source block
- Marked Create: FluidLogistics versions before 1.3.3 as incompatible: they stopped the Re-Packager from merging item and fluid packages ([#26](https://github.com/LIUKRAST/Deployer/issues/26))
- Gauges ordering only custom resources (for example only fluids) no longer produce packages that package shelves never see as complete ([Repackaged#28](https://github.com/LIUKRAST/Repackaged/issues/28))
- Gauge orders mixing several resource types now mark only the last type as final, on each network separately
- Panel inputs are read in the order they were connected, so string concatenation is predictable ([CreateExtraGauges#109](https://github.com/LIUKRAST/CreateExtraGauges/issues/109))
- Custom redstone requester tabs no longer hide the player inventory when another mod wraps its slots ([Repackaged#31](https://github.com/LIUKRAST/Repackaged/issues/31))
- Package Couriers planes no longer void the fluid or energy of generic packages when they unpack on arrival to a player ([Repackaged#44](https://github.com/LIUKRAST/Repackaged/issues/44))
- Pending fluid and energy promises were lost when a world or chunk was saved and loaded again, so gauges ordered the same resources twice

For developers
- No breaking changes: mods built against 0.1.3 keep working
- Built against NeoForge 21.1.265 and Create 6.0.11; the supported version ranges are unchanged
- New `easyWriteSafe` hook: override it to keep data when a panel is saved in a schematic or blueprint
- `AbstractPanelBehaviour.read`, `write` and `writeSafe` should not be overridden anymore (use `easyRead`, `easyWrite`, `easyWriteSafe`); they will become final in a future release
- `AbstractPanelBehaviour.getValue` returns `Empty` instead of throwing when the connection is not registered on the panel
- `notifyOutputs` keeps notifying the other targets when one is unloaded
- New `OrderingPanelBehaviour#invalidateNetworkSummary` hook, `PanelConnection.hasAnyListener` and `AbstractPanelBehaviour.collectValues`
- `StockTabScreen#clickAmount` now has a default implementation
- `getAllValues` and `getAllValuesWithSource` return values in connection order (panels, then links, then extra connections)
- The point where Deployer cancels `FactoryPanelBehaviour#tickRequests` moved after Create's timer and state checks
