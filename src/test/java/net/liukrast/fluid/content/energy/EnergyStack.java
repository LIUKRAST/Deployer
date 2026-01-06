package net.liukrast.fluid.content.energy;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class EnergyStack {
    public static final EnergyStack EMPTY = new EnergyStack(0);

    public static final Codec<EnergyStack> CODEC = Codec.INT.xmap(
            EnergyStack::new,
            EnergyStack::getAmount
    );

    public static final StreamCodec<ByteBuf, EnergyStack> STREAM_CODEC = StreamCodec.of(
            (buf, val) -> buf.writeInt(val.getAmount()),
            buf -> new EnergyStack(buf.readInt())
    );

    private int amount;

    public EnergyStack(int amount) {
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public boolean isEmpty() {
        return amount == 0;
    }
}
