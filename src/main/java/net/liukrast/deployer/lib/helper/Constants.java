package net.liukrast.deployer.lib.helper;

import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.lang.LangNumberFormat;
import net.minecraft.core.Registry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link net.liukrast.constants.Constants} with NeoForge and Create helpers.
 * <p>
 * Ids, logger, registry and plain text helpers come from Constants Manager; this interface adds
 * {@link DeferredRegister} creation, Create item tooltips and Catnip {@link LangBuilder}s.
 */
public interface Constants extends net.liukrast.constants.Constants {

    static Constants of(String modId) {
        return of(modId, "1.0.0");
    }

    static Constants of(String modId, String protocol) {
        Logger logger = LoggerFactory.getLogger(modId);
        return new Constants() {
            @Override
            public String getModId() {
                return modId;
            }

            @Override
            public Logger getLogger() {
                return logger;
            }

            @Override
            public String getProtocol() {
                return protocol;
            }
        };
    }

    default <T> DeferredRegister<T> createDeferred(Registry<T> registry) {
        return DeferredRegister.create(registry, getModId());
    }

    default Item wrapWithShiftSummary(Item item) {
        var modifier = new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                .andThen(TooltipModifier.mapNull(KineticStats.create(item)));
        TooltipModifier.REGISTRY.register(item, modifier);
        return item;
    }

    default LangBuilder langBuilder() {
        return new LangBuilder(getModId());
    }

    default LangBuilder blockName(BlockState state) {
        return langBuilder().add(state.getBlock()
                .getName());
    }

    default LangBuilder itemName(ItemStack stack) {
        return langBuilder().add(stack.getHoverName()
                .copy());
    }

    default LangBuilder fluidName(FluidStack stack) {
        return langBuilder().add(stack.getHoverName()
                .copy());
    }

    default LangBuilder number(double d) {
        return langBuilder().text(LangNumberFormat.format(d));
    }

    default LangBuilder translate(String langKey, Object... args) {
        return langBuilder().translate(langKey, args);
    }

    default LangBuilder text(String text) {
        return langBuilder().text(text);
    }
}
