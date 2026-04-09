package net.liukrast.deployer.lib.logistics.requester;

import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterBlockEntity;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import net.createmod.catnip.platform.CatnipServices;
import net.liukrast.deployer.lib.mixinExtensions.RRBEExtension;
import net.liukrast.deployer.lib.registry.DeployerPackets;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

public class RequesterRequestDataPacket extends BlockEntityConfigurationPacket<RedstoneRequesterBlockEntity> {
    public static final StreamCodec<? super RegistryFriendlyByteBuf, RequesterRequestDataPacket> STREAM_CODEC = BlockPos.STREAM_CODEC.map(
            RequesterRequestDataPacket::new,
            p -> p.pos
    );

    public RequesterRequestDataPacket(BlockPos pos) {
        super(pos);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return DeployerPackets.REQUESTER_REQUEST_DATA;
    }

    @Override
    protected void applySettings(ServerPlayer player, RedstoneRequesterBlockEntity redstoneRequesterBlockEntity) {
        var map = ((RRBEExtension)redstoneRequesterBlockEntity).deployer$getAllEncodedRequests();
        CatnipServices.NETWORK.sendToClient(player, new RequesterExtraDataS2CPacket(pos, map));
    }

}
