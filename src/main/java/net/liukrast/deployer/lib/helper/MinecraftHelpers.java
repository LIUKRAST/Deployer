package net.liukrast.deployer.lib.helper;

import net.liukrast.deployer.lib.Deployer;
import net.liukrast.deployer.lib.logistics.GenericPackageOrderData;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.generators.*;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Helper class for minecraft related utility
 * */
public class MinecraftHelpers {
    private MinecraftHelpers() {}

    public static class ModelProvider {
        private ModelProvider() {}

        public static class Blocks {
            private Blocks() {}

            public static BlockModelBuilder createGauge(BlockModelProvider instance, Item item, Function<String, String> texture) {
                var id = Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item));
                return instance.getBuilder(id.toString()).parent(new ModelFile.UncheckedModelFile(Deployer.CONSTANTS.id("block/template_gauge")))
                        .texture("texture", ResourceLocation.fromNamespaceAndPath(id.getNamespace(), texture.apply(id.getPath())))
                        .texture("particle", ResourceLocation.fromNamespaceAndPath(id.getNamespace(), texture.apply(id.getPath())));
            }

            public static BlockModelBuilder createGauge(BlockModelProvider instance, Item item) {
                return createGauge(instance, item, id -> "block/" + id);
            }

            public static BlockModelBuilder createPanel(BlockModelProvider instance, Item item) {
                return createGauge(instance, item, id -> "block/" + id.split("_")[0] + "_panel");
            }
        }

        public static class Items {
            private Items() {}

            public static ItemModelBuilder createGauge(ItemModelProvider instance, Item item, Function<String, String> texture) {
                var id = Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item));
                return instance.getBuilder(id.toString()).parent(new ModelFile.UncheckedModelFile(ResourceLocation.fromNamespaceAndPath("create", "block/factory_gauge/item")))
                        .texture("0", ResourceLocation.fromNamespaceAndPath(id.getNamespace(), texture.apply(id.getPath())))
                        .texture("particle", ResourceLocation.fromNamespaceAndPath(id.getNamespace(), texture.apply(id.getPath())));
            }


            public static ItemModelBuilder createGauge(ItemModelProvider instance, Item item) {
                return createGauge(instance, item, id -> "block/" + id);
            }

            public static ItemModelBuilder createPanel(ItemModelProvider instance, Item item) {
                return createGauge(instance, item, id -> "block/" + id.split("_")[0] + "_panel");
            }
        }
    }

    public static class CreativeTabs {
        private CreativeTabs() {}

        public static CreativeModeTab.Builder createMainTab(String modId, ItemStack icon) {
            return CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + modId))
                    .icon(() -> icon);
        }
    }

    public static class DataComponents {
        private DataComponents() {}

        public static <V> DataComponentType.Builder<GenericPackageOrderData<V>> createOrderData(Supplier<StockInventoryType<?, V, ?>> typeSupplier) {
            return DataComponentType.<GenericPackageOrderData<V>>builder()
                    .persistent(GenericPackageOrderData.createCodec(typeSupplier))
                    .networkSynchronized(GenericPackageOrderData.createStreamCodec(typeSupplier));
        }

        public static <V> DataComponentType.Builder<GenericOrderContained<V>> createContext(Supplier<StockInventoryType<?, V, ?>> typeSupplier) {
            return DataComponentType.<GenericOrderContained<V>>builder()
                    .persistent(CodecHelpers.Normal.createContained(typeSupplier))
                    .networkSynchronized(CodecHelpers.Stream.createContained(typeSupplier));
        }
    }



}
