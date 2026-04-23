package net.liukrast.deployer.lib.registry;

import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.CatnipPacketRegistry;
import net.liukrast.deployer.lib.Deployer;
import net.liukrast.deployer.lib.logistics.board.connection.PanelSpecialSetupPacket;
import net.liukrast.deployer.lib.logistics.requester.RequesterExtraDataC2SPacket;
import net.liukrast.deployer.lib.logistics.requester.RequesterExtraDataS2CPacket;
import net.liukrast.deployer.lib.logistics.requester.RequesterRequestDataPacket;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderRequestPacket;
import net.liukrast.deployer.lib.logistics.stockTicker.LogisticalStockGenericRequestPacket;
import net.liukrast.deployer.lib.logistics.stockTicker.LogisticalStockGenericResponsePacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Locale;

public enum DeployerPackets implements BasePacketPayload.PacketTypeProvider {
    LOGISTICS_STOCK_RESPONSE(LogisticalStockGenericResponsePacket.class, LogisticalStockGenericResponsePacket.STREAM_CODEC),
    LOGISTICS_STOCK_GENERIC_REQUEST(LogisticalStockGenericRequestPacket.class, LogisticalStockGenericRequestPacket.STREAM_CODEC),
    GENERIC_ORDER_REQUEST(GenericOrderRequestPacket.class, GenericOrderRequestPacket.STREAM_CODEC),
    REQUESTER_EXTRA_DATA_C2S(RequesterExtraDataC2SPacket.class, RequesterExtraDataC2SPacket.STREAM_CODEC),
    REQUESTER_REQUEST_DATA(RequesterRequestDataPacket.class, RequesterRequestDataPacket.STREAM_CODEC),
    REQUESTER_EXTRA_DATA_S2C(RequesterExtraDataS2CPacket.class, RequesterExtraDataS2CPacket.STREAM_CODEC),
    DISCONNECT_PANEL(PanelSpecialSetupPacket.class, PanelSpecialSetupPacket.STREAM_CODEC),
    ;

    private final CatnipPacketRegistry.PacketType<?> type;

    @SuppressWarnings("unchecked")
    <T extends BasePacketPayload> DeployerPackets(Class<T> clazz, StreamCodec<? super RegistryFriendlyByteBuf, ? extends T> codec) {
        String name = this.name().toLowerCase(Locale.ROOT);
        StreamCodec<RegistryFriendlyByteBuf, T> unchecked = (StreamCodec<RegistryFriendlyByteBuf, T>) codec;
        this.type = new CatnipPacketRegistry.PacketType<>(
                new CustomPacketPayload.Type<>(Deployer.CONSTANTS.id(name)),
                clazz, unchecked
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends CustomPacketPayload> CustomPacketPayload.Type<T> getType() {
        return (CustomPacketPayload.Type<T>) this.type.type();
    }

    public static void register() {
        CatnipPacketRegistry packetRegistry = new CatnipPacketRegistry(Deployer.CONSTANTS.getModId(), Deployer.CONSTANTS.getProtocol());
        for (DeployerPackets packet : values()) {
            packetRegistry.registerPacket(packet.type);
        }
        packetRegistry.registerAllPackets();
    }
}
