package net.liukrast.deployer.lib.mixin.accessors;

import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StockKeeperRequestScreen.class)
public interface StockKeeperRequestScreenAccessor {

    @Mixin(StockKeeperRequestScreen.CategoryEntry.class)
    interface CategoryEntryAccessor {
        @Accessor("y")
        void setY(int value);
        @Accessor("hidden")
        void setHidden(boolean hidden);
        @Accessor("hidden")
        boolean getHidden();
        @Accessor("y")
        int getY();
        @Accessor("name")
        String getName();

        @Accessor("targetBECategory")
        int getTargetBECategory();
    }
}
