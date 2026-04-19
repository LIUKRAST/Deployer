package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.logistics.box.PackageEntity;
import net.createmod.catnip.lang.LangBuilder;
import net.liukrast.deployer.lib.DeployerConfig;
import net.liukrast.deployer.lib.DeployerConstants;
import net.liukrast.deployer.lib.helper.client.DeployerGoggleInformation;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(PackageEntity.class)
public class PackageEntityMixin implements DeployerGoggleInformation {

    @ModifyExpressionValue(method = {"writeSpawnData", "readSpawnData"}, at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/ItemStack;STREAM_CODEC:Lnet/minecraft/network/codec/StreamCodec;", opcode = Opcodes.GETSTATIC))
    private StreamCodec<RegistryFriendlyByteBuf, ItemStack> writeSpawnData(StreamCodec<RegistryFriendlyByteBuf, ItemStack> original) {
        return ItemStack.OPTIONAL_STREAM_CODEC;
    }
}
