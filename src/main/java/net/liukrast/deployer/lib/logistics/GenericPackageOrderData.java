package net.liukrast.deployer.lib.logistics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.Nullable;
import java.rmi.registry.Registry;
import java.util.Optional;

public record GenericPackageOrderData<V>(int orderId, int linkIndex, boolean isFinalLink, int fragmentIndex, boolean isFinal, @Nullable GenericOrderContained<V> orderContext) {
    public GenericPackageOrderData(int orderId, int linkIndex, boolean isFinalLink, int fragmentIndex, boolean isFinal, Optional<GenericOrderContained<V>> orderContext) {
        this(orderId, linkIndex, isFinalLink, fragmentIndex, isFinal, orderContext.orElse(null));
    }

    public static <V> Codec<GenericPackageOrderData<V>> simpleCodec(Codec<V> codec) {
        return RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("order_id").forGetter(GenericPackageOrderData::orderId),
                Codec.INT.fieldOf("link_index").forGetter(GenericPackageOrderData::linkIndex),
                Codec.BOOL.fieldOf("is_final_link").forGetter(GenericPackageOrderData::isFinalLink),
                Codec.INT.fieldOf("fragment_index").forGetter(GenericPackageOrderData::fragmentIndex),
                Codec.BOOL.fieldOf("is_final").forGetter(GenericPackageOrderData::isFinal),
                GenericOrderContained.simpleCodec(codec).optionalFieldOf("order_context").forGetter(i -> Optional.ofNullable(i.orderContext))
        ).apply(instance, GenericPackageOrderData::new));
    }

    public static <B extends ByteBuf, V> StreamCodec<B, GenericPackageOrderData<V>> simpleStreamCodec(StreamCodec<B, V> codec) {
        return StreamCodec.composite(
                ByteBufCodecs.INT, GenericPackageOrderData::orderId,
                ByteBufCodecs.INT, GenericPackageOrderData::linkIndex,
                ByteBufCodecs.BOOL, GenericPackageOrderData::isFinalLink,
                ByteBufCodecs.INT, GenericPackageOrderData::fragmentIndex,
                ByteBufCodecs.BOOL, GenericPackageOrderData::isFinal,
                CatnipStreamCodecBuilders.nullable(GenericOrderContained.simpleStreamCodec(codec)), GenericPackageOrderData::orderContext,
                GenericPackageOrderData::new
        );
    }
}
