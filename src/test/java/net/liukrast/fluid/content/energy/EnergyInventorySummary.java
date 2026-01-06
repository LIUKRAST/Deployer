package net.liukrast.fluid.content.energy;

import net.liukrast.deployer.lib.logistics.packager.AbstractInventorySummary;
import net.liukrast.fluid.registry.RegisterStockInventoryTypes;

import java.util.function.Supplier;

public class EnergyInventorySummary extends AbstractInventorySummary<Energy, EnergyStack> {

    public static final Supplier<EnergyInventorySummary> EMPTY = EnergyInventorySummary::new;

    public EnergyInventorySummary() {
        super(RegisterStockInventoryTypes.ENERGY.get());
    }
}
