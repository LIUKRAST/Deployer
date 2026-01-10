package net.liukrast.deployer.lib.logistics;

import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface LogisticallyLinked {

    default boolean isFoil(ItemStack stack) {
        return isTuned(stack);
    }

    default boolean isTuned(ItemStack stack) {
        return LogisticallyLinkedBlockItem.isTuned(stack);
    }

    default void assignFrequency(ItemStack stack, Player player, UUID frequency) {
        LogisticallyLinkedBlockItem.assignFrequency(stack, player, frequency);
    }

    @Nullable
    default UUID networkFromStack(ItemStack stack) {
        return LogisticallyLinkedBlockItem.networkFromStack(stack);
    }

    default void appendHoverText(ItemStack stack, Item.TooltipContext tooltipContext,
                                 List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        var uuid = networkFromStack(stack);
        if(uuid == null) return;

        CreateLang.translate("logistically_linked.tooltip")
                .style(ChatFormatting.GOLD)
                .addTo(tooltipComponents);

        CreateLang.translate("logistically_linked.tooltip_clear")
                .style(ChatFormatting.GRAY)
                .addTo(tooltipComponents);
    }

}
