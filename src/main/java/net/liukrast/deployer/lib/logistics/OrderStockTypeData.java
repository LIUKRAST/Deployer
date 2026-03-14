package net.liukrast.deployer.lib.logistics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record OrderStockTypeData(int index, boolean isFinal) {
    public static final Codec<OrderStockTypeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("index").forGetter(OrderStockTypeData::index),
            Codec.BOOL.fieldOf("is_final").forGetter(OrderStockTypeData::isFinal)
    ).apply(instance, OrderStockTypeData::new));

    public static final OrderStockTypeData EMPTY = new OrderStockTypeData(0, true);

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof OrderStockTypeData(int index1, boolean aFinal)) return index1 == index() && aFinal == isFinal();
        return false;
    }
}
