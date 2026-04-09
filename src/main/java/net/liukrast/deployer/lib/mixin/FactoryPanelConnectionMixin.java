package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import net.liukrast.deployer.lib.logistics.board.connection.PanelConnection;
import net.liukrast.deployer.lib.mixinExtensions.FPCExtension;
import net.liukrast.deployer.lib.registry.DeployerRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FactoryPanelConnection.class)
public class FactoryPanelConnectionMixin implements FPCExtension {
    @Unique private PanelConnection<?> deployer$id;

    @ModifyExpressionValue(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/serialization/codecs/RecordCodecBuilder;create(Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;"
            )
    )
    private static Codec<FactoryPanelConnection> clinit(Codec<FactoryPanelConnection> original) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<FactoryPanelConnection, T>> decode(DynamicOps<T> ops, T input) {
                return original.decode(ops, input).flatMap(pair -> {
                    FactoryPanelConnection connection = pair.getFirst();
                    ops.get(input, "deployer$id").result().flatMap(nodeID -> ResourceLocation.CODEC.parse(ops, nodeID).result())
                            .ifPresent(rl -> ((FPCExtension)connection).deployer$setLinkMode(DeployerRegistries.PANEL_CONNECTION.get(rl)));
                    return DataResult.success(pair);
                });
            }

            @Override
            public <T> DataResult<T> encode(FactoryPanelConnection input, DynamicOps<T> ops, T prefix) {
                return original.encode(input, ops, prefix).flatMap(encodeBase -> {
                    var conn = ((FPCExtension)input).deployer$getLinkMode();
                    ResourceLocation id = conn == null ? null : DeployerRegistries.PANEL_CONNECTION.getKey(conn);
                    if(id != null) return ResourceLocation.CODEC.encode(id, ops, ops.empty())
                            .flatMap(idElement -> ops.mergeToMap(encodeBase, ops.createString("deployer$id"), idElement));
                    return DataResult.success(encodeBase);
                });
            }
        };
    }

    @Override
    public @Nullable PanelConnection<?> deployer$getLinkMode() {
        return deployer$id;
    }

    @Override
    public void deployer$setLinkMode(@Nullable PanelConnection<?> rl) {
        deployer$id = rl;
    }
}
