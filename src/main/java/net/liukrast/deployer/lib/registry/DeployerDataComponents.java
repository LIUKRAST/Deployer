package net.liukrast.deployer.lib.registry;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.liukrast.deployer.lib.Deployer;
import net.liukrast.deployer.lib.logistics.OrderStockTypeData;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class DeployerDataComponents {
    private DeployerDataComponents() {}

    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, Deployer.CONSTANTS.getModId());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<OrderStockTypeData>> ORDER_STOCK_TYPE_DATA = DATA_COMPONENTS.register("order_stock_type_data", () -> DataComponentType.<OrderStockTypeData>builder()
            .persistent(OrderStockTypeData.CODEC)
            .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Map<StockInventoryType<?,?,?>, GenericOrderContained<?>>>> EXTRA_REQUEST_DATA = DATA_COMPONENTS.register("extra_request_data", () -> DataComponentType.<Map<StockInventoryType<?,?,?>, GenericOrderContained<?>>>builder()
            .persistent(new Codec<>() {
                @Override
                public <T> DataResult<Pair<Map<StockInventoryType<?, ?, ?>, GenericOrderContained<?>>, T>> decode(DynamicOps<T> ops, T input) {
                    Map<StockInventoryType<?,?,?>, GenericOrderContained<?>> map = new HashMap<>();
                    ops.getMap(input).getOrThrow().entries().forEach(pair -> {
                        var id = ResourceLocation.CODEC.decode(ops, pair.getFirst()).getOrThrow().getFirst();
                        var type = DeployerRegistries.STOCK_INVENTORY.get(id);
                        if(type == null) return;
                        var e = type.valueHandler().orderContainedCodec().decode(ops, pair.getSecond()).getOrThrow().getFirst();
                        map.put(type, e);
                    });
                    return DataResult.success(Pair.of(map, ops.empty()));
                }

                @SuppressWarnings("unchecked")
                @Override
                public <T> DataResult<T> encode(Map<StockInventoryType<?, ?, ?>, GenericOrderContained<?>> input, DynamicOps<T> ops, T prefix) {
                    Map<T, T> map = new HashMap<>();
                    input.forEach((key, value) -> {
                        var id = DeployerRegistries.STOCK_INVENTORY.getKey(key);
                        if(id == null) return;
                        T k = ResourceLocation.CODEC.encode(id, ops, prefix).getOrThrow();
                        T v = encodeS((StockInventoryType<?, Object, ?>) key, (GenericOrderContained<Object>)value, ops, prefix);
                        map.put(k, v);
                    });
                    return DataResult.success(ops.createMap(map));
                }

                private <V, T> T encodeS(StockInventoryType<?, V, ?> type, GenericOrderContained<V> order, DynamicOps<T> ops, T prefix) {
                    return type.valueHandler().orderContainedCodec().encode(order, ops, prefix).getOrThrow();
                }
            })
            .networkSynchronized(new StreamCodec<>() {
                @Override
                public @NotNull Map<StockInventoryType<?, ?, ?>, GenericOrderContained<?>> decode(@NotNull RegistryFriendlyByteBuf buf) {
                    int size = buf.readVarInt();
                    Map<StockInventoryType<?, ?, ?>, GenericOrderContained<?>> map = new HashMap<>();
                    for (int i = 0; i < size; i++) {
                        var id = buf.readResourceLocation();
                        var type = DeployerRegistries.STOCK_INVENTORY.get(id);
                        if (type == null) throw new IllegalStateException("Stock inventory not registered");
                        var val = type.valueHandler().orderContainedStreamCodec().decode(buf);
                        map.put(type, val);
                    }
                    return map;
                }

                @SuppressWarnings("unchecked")
                @Override
                public void encode(@NotNull RegistryFriendlyByteBuf buf, @NotNull Map<StockInventoryType<?, ?, ?>, GenericOrderContained<?>> val) {
                    buf.writeVarInt(val.size());
                    val.forEach((key, val1) -> {
                        ResourceLocation id = DeployerRegistries.STOCK_INVENTORY.getKey(key);
                        if (id == null) throw new IllegalStateException("Stock inventory not registered");
                        buf.writeResourceLocation(id);
                        encodeS((StockInventoryType<?, Object, ?>) key, (GenericOrderContained<Object>) val1, buf);
                    });
                }

                private <V> void encodeS(StockInventoryType<?, V, ?> type, GenericOrderContained<V> entry, RegistryFriendlyByteBuf buf) {
                    type.valueHandler().orderContainedStreamCodec().encode(buf, entry);
                }
            })
            .build()
    );

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}
