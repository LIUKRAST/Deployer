package net.liukrast.deployer.lib.mixin.accessors;

import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import net.liukrast.deployer.lib.mixinExtensions.STBEExtension;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(StockTickerBlockEntity.class)
public interface StockTickerBlockEntityAccessor extends STBEExtension {
    @Accessor("categories")
    List<ItemStack> getCategories();

    @Accessor("hiddenCategoriesByPlayer")
    Map<UUID, List<Integer>> getHiddenCategoriesByPlayer();

    @Accessor("activeLinks")
    int getActiveLinks();
}
