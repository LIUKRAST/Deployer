package net.liukrast.deployer.lib.mixin.accessors;

import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StockKeeperRequestScreen.class)
public interface StockKeeperRequestScreenAccessor {

    @Mixin(StockKeeperRequestScreen.CategoryEntry.class)
    interface CategoryEntryAccessor {
        @Accessor("y")
        void deployer$setY(int value);
        @Accessor("hidden")
        void deployer$setHidden(boolean hidden);
        @Accessor("hidden")
        boolean deployer$getHidden();
        @Accessor("y")
        int deployer$getY();
        @Accessor("name")
        String deployer$getName();

        @Accessor("targetBECategory")
        int deployer$getTargetBECategory();
    }
}
