package net.liukrast.deployer.lib.logistics.stockTicker;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.WiFiEffectPacket;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterBlock;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import io.netty.buffer.ByteBuf;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.mixinExtensions.STBEExtension;
import net.liukrast.deployer.lib.registry.DeployerPackets;
import net.liukrast.deployer.lib.registry.DeployerRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenericOrderRequestPacket extends BlockEntityConfigurationPacket<StockTickerBlockEntity> {
    public static final StreamCodec<RegistryFriendlyByteBuf, GenericOrderRequestPacket> STREAM_CODEC = new StreamCodec<>() {

        @Override
        public @NotNull GenericOrderRequestPacket decode(@NotNull RegistryFriendlyByteBuf buf) {
            BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
            PackageOrderWithCrafts defaultOrder = PackageOrderWithCrafts.STREAM_CODEC.decode(buf);
            List<ResourceLocation> otherTypes = ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
            Map<StockInventoryType<?,?,?>, GenericOrderContained<?>> map = new HashMap<>();
            for(ResourceLocation otherType : otherTypes) {
                var type = DeployerRegistries.STOCK_INVENTORY.get(otherType);
                assert type != null;
                GenericOrderContained<?> order = GenericOrderContained.simpleStreamCodec(type.valueHandler().streamCodec()).decode(buf);
                map.put(type, order);
            }
            String address = ByteBufCodecs.STRING_UTF8.decode(buf);
            boolean encodeRequester = ByteBufCodecs.BOOL.decode(buf);
            return new GenericOrderRequestPacket(pos, defaultOrder, map, address, encodeRequester);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void encode(@NotNull RegistryFriendlyByteBuf buf, @NotNull GenericOrderRequestPacket p) {
            BlockPos.STREAM_CODEC.encode(buf, p.pos);
            PackageOrderWithCrafts.STREAM_CODEC.encode(buf, p.defaultOrder);
            List<ResourceLocation> toEncodeKeys = new ArrayList<>();
            List<Runnable> toEncodeValues = new ArrayList<>();
            for(var entry : p.types.entrySet()) {
                var type = entry.getKey();
                toEncodeKeys.add(DeployerRegistries.STOCK_INVENTORY.getKey(entry.getKey()));
                StreamCodec<RegistryFriendlyByteBuf, Object> vC = (StreamCodec<RegistryFriendlyByteBuf, Object>) type.valueHandler().streamCodec();
                StreamCodec<? super ByteBuf, GenericOrderContained<Object>> codec = GenericOrderContained.simpleStreamCodec(vC);
                toEncodeValues.add(() -> codec.encode(buf, (GenericOrderContained<Object>) entry.getValue()));
            }
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, toEncodeKeys);
            for(var elem : toEncodeValues) elem.run();
            ByteBufCodecs.STRING_UTF8.encode(buf, p.address);
            ByteBufCodecs.BOOL.encode(buf, p.encodeRequester);
        }
    };

    private final PackageOrderWithCrafts defaultOrder;
    private final Map<StockInventoryType<?, ?, ?>, GenericOrderContained<?>> types;
    private final String address;
    private final boolean encodeRequester;

    /**
     * Warning: ensure middle ? of stockInventoryType is the same of the generic for orders
     * */
    public GenericOrderRequestPacket(BlockPos pos, PackageOrderWithCrafts defaultOrder, Map<StockInventoryType<?,?,?>, GenericOrderContained<?>> orders, String address, boolean encodeRequester) {
        super(pos);
        this.defaultOrder = defaultOrder;
        this.types = orders;
        this.address = address;
        this.encodeRequester = encodeRequester;
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return DeployerPackets.GENERIC_ORDER_REQUEST;
    }

    @Override
    protected void applySettings(ServerPlayer player, StockTickerBlockEntity be) {
        boolean orderNotEmpty = !defaultOrder.isEmpty() || types.values().stream().anyMatch(r -> !r.isEmpty());
        if(encodeRequester) {
            if(orderNotEmpty)
                AllSoundEvents.CONFIRM.playOnServer(be.getLevel(), pos);
            player.closeContainer();
            RedstoneRequesterBlock.programRequester(player, be, defaultOrder, address); //TODO: Make this work with custom orders too
            return;
        }

        if(orderNotEmpty) {
            AllSoundEvents.STOCK_TICKER_REQUEST.playOnServer(be.getLevel(), pos);
            AllAdvancements.STOCK_TICKER.awardTo(player);
            WiFiEffectPacket.send(player.level(), pos);
        }

        ((STBEExtension)be).deployer$broadcastAllPackageRequest(defaultOrder, LogisticallyLinkedBehaviour.RequestType.PLAYER, types/*, null*/, address);
    }
}
