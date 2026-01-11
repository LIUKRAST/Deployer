package net.liukrast.deployer.lib.mixin;

import com.simibubi.create.content.logistics.box.PackageEntity;
import net.createmod.catnip.lang.LangBuilder;
import net.liukrast.deployer.lib.DeployerConfig;
import net.liukrast.deployer.lib.DeployerConstants;
import net.liukrast.deployer.lib.helper.DeployerGoggleInformation;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(PackageEntity.class)
public class PackageEntityMixin implements DeployerGoggleInformation {

    @Shadow
    public ItemStack box;

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if(!DeployerConfig.Client.PACKAGE_GOGGLE_INFO.getAsBoolean()) return DeployerGoggleInformation.super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        var li = box.getTooltipLines(Item.TooltipContext.of(Minecraft.getInstance().level), Minecraft.getInstance().player, TooltipFlag.NORMAL);
        for(var c : li) {
            new LangBuilder(DeployerConstants.MOD_ID).add(c).forGoggles(tooltip, 0);
        }
        return true;
    }
}
