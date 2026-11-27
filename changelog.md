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

For developers
- `AbstractPanelBehaviour.getValue` returns `Empty` instead of throwing when the connection is not registered on the panel
- `notifyOutputs` keeps notifying the other targets when one is unloaded
- New `OrderingPanelBehaviour#invalidateNetworkSummary` hook, `PanelConnection.hasAnyListener` and `AbstractPanelBehaviour.collectValues`
- `StockTabScreen#clickAmount` now has a default implementation
