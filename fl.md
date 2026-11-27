# Factory Logistics Compatibility

## What works

Starting with Deployer 0.1.5, Deployer and [Create: Extra Gauges](https://modrinth.com/mod/6YJgomwt) can be installed together with [Create: Factory Logistics](https://modrinth.com/mod/QMGO17gm) (FL) 1.6.1 or newer.

- Every Extra Gauges gauge works as usual, next to the Factory Logistics gauges, jars and packagers.
- Factory Logistics keeps its own fluid packages, jars and Stock Keeper integration.

## Repackaged

[Repackaged](https://modrinth.com/mod/D6daOdv9) is **not compatible with Factory Logistics yet**, and the game refuses to start when both are installed.

Both mods add fluid packaging to Create's logistics, and they do it in different ways: Factory Logistics replaces parts of Create's ordering code that Repackaged builds on. Some Repackaged features (fluid and energy orders from Redstone Requesters, orders that mix items with fluids or energy) would silently stop working, so we block the combination instead.

Compatibility between Repackaged and Factory Logistics is **planned** for a future update. Until then, pick one of the two for fluid logistics.

## Reporting problems

If something breaks with Deployer or Extra Gauges and Factory Logistics installed, please open an issue on the [Deployer](https://github.com/LIUKRAST/Deployer/issues) or [Extra Gauges](https://github.com/LIUKRAST/CreateExtraGauges/issues) tracker and mention your Factory Logistics version.
