package net.liukrast.deployer.lib.logistics.requester;

import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterBlockEntity;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterScreen;
import com.simibubi.create.foundation.networking.BlockEntityDataPacket;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.liukrast.deployer.lib.mixinExtensions.RRSExtension;
import net.liukrast.deployer.lib.registry.DeployerPackets;
import net.liukrast.deployer.lib.registry.DeployerRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.system.NonnullDefault;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NonnullDefault
public class RequesterExtraDataS2CPacket extends BlockEntityDataPacket<RedstoneRequesterBlockEntity> {
    public static final StreamCodec<? super RegistryFriendlyByteBuf, RequesterExtraDataS2CPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RequesterExtraDataS2CPacket decode(RegistryFriendlyByteBuf buf) {
            BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
            List<ResourceLocation> otherTypes = ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
            Map<StockInventoryType<?,?,?>, GenericOrderContained<?>> map = new HashMap<>();
            for(ResourceLocation otherType : otherTypes) {
                var type = DeployerRegistries.STOCK_INVENTORY.get(otherType);
                assert type != null;
                GenericOrderContained<?> order = type.valueHandler().orderContainedStreamCodec().decode(buf);
                map.put(type, order);
            }
            return new RequesterExtraDataS2CPacket(pos, map);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void encode(RegistryFriendlyByteBuf buf, RequesterExtraDataS2CPacket p) {
            BlockPos.STREAM_CODEC.encode(buf, p.pos);
            List<ResourceLocation> toEncodeKeys = new ArrayList<>();
            List<Runnable> toEncodeValues = new ArrayList<>();
            for(var entry : p.orders.entrySet()) {
                var type = entry.getKey();
                toEncodeKeys.add(DeployerRegistries.STOCK_INVENTORY.getKey(entry.getKey()));
                StreamCodec<RegistryFriendlyByteBuf, GenericOrderContained<Object>> codec = (StreamCodec<RegistryFriendlyByteBuf, GenericOrderContained<Object>>)(StreamCodec<?,?>)(type.valueHandler().orderContainedStreamCodec());
                toEncodeValues.add(() -> codec.encode(buf, (GenericOrderContained<Object>) entry.getValue()));
            }
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, toEncodeKeys);
            for(var elem : toEncodeValues) elem.run();
        }
    };

    private final Map<StockInventoryType<?,?,?>, GenericOrderContained<?>> orders;

    public RequesterExtraDataS2CPacket(BlockPos pos, Map<StockInventoryType<?,?,?>, GenericOrderContained<?>> orders) {
        super(pos);
        this.orders = orders;
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return DeployerPackets.REQUESTER_EXTRA_DATA_S2C;
    }

    @Override
    protected void handlePacket(RedstoneRequesterBlockEntity blockEntity) {
        var minecraft = Minecraft.getInstance();
        if(!(minecraft.screen instanceof RRSExtension rrs)) return;
        rrs.deployer$receiveData(orders);
    }
}
