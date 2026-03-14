package net.liukrast.deployer.lib.logistics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.liukrast.deployer.lib.helper.CodecHelpers;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Supplier;

public record GenericPackageOrderData<V>(int orderId, int linkIndex, boolean isFinalLink, int fragmentIndex, boolean isFinal, @Nullable GenericOrderContained<V> orderContext) {
    public GenericPackageOrderData(int orderId, int linkIndex, boolean isFinalLink, int fragmentIndex, boolean isFinal, Optional<GenericOrderContained<V>> orderContext) {
        this(orderId, linkIndex, isFinalLink, fragmentIndex, isFinal, orderContext.orElse(null));
    }


    public static <V> Codec<GenericPackageOrderData<V>> createCodec(Supplier<StockInventoryType<?, V, ?>> supplierType) {
        return RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("order_id").forGetter(GenericPackageOrderData::orderId),
                Codec.INT.fieldOf("link_index").forGetter(GenericPackageOrderData::linkIndex),
                Codec.BOOL.fieldOf("is_final_link").forGetter(GenericPackageOrderData::isFinalLink),
                Codec.INT.fieldOf("fragment_index").forGetter(GenericPackageOrderData::fragmentIndex),
                Codec.BOOL.fieldOf("is_final").forGetter(GenericPackageOrderData::isFinal),
                CodecHelpers.Normal.deferredCodec(() -> supplierType.get().valueHandler().orderContainedCodec()).optionalFieldOf("order_context").forGetter(i -> Optional.ofNullable(i.orderContext))
        ).apply(instance, GenericPackageOrderData::new));
    }

    public static <V> StreamCodec<RegistryFriendlyByteBuf, GenericPackageOrderData<V>> createStreamCodec(Supplier<StockInventoryType<?, V, ?>> supplierType) {
        return StreamCodec.composite(
                ByteBufCodecs.INT, GenericPackageOrderData::orderId,
                ByteBufCodecs.INT, GenericPackageOrderData::linkIndex,
                ByteBufCodecs.BOOL, GenericPackageOrderData::isFinalLink,
                ByteBufCodecs.INT, GenericPackageOrderData::fragmentIndex,
                ByteBufCodecs.BOOL, GenericPackageOrderData::isFinal,
                CatnipStreamCodecBuilders.nullable(CodecHelpers.Stream.deferredStreamCodec(() -> supplierType.get().valueHandler().orderContainedStreamCodec())), GenericPackageOrderData::orderContext,
                GenericPackageOrderData::new
        );
    }
}
