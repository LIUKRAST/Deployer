package net.liukrast.deployer.lib.logistics.stockTicker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

public record GenericOrder<V>(List<V> stacks) {

    public boolean isEmpty() {
        return stacks.isEmpty();
    }

    public static <V> Codec<GenericOrder<V>> simpleCodec(Codec<V> codec) {
        return RecordCodecBuilder.create(instance -> instance.group(
                Codec.list(codec).fieldOf("entries").forGetter(GenericOrder::stacks)
        ).apply(instance, GenericOrder::new));
    }

    public static <B extends ByteBuf, V> StreamCodec<B, GenericOrder<V>> simpleStreamCodec(StreamCodec<B, V> codec) {
        return CatnipStreamCodecBuilders.list(codec)
                .map(GenericOrder::new, GenericOrder::stacks);
    }
}
