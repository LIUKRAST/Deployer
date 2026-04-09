package net.liukrast.deployer.lib.logistics.board.connection;

import com.simibubi.create.content.logistics.factoryBoard.*;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import net.liukrast.deployer.lib.mixinExtensions.FPBExtension;
import net.liukrast.deployer.lib.mixinExtensions.FPCExtension;
import net.liukrast.deployer.lib.registry.DeployerPackets;
import net.liukrast.deployer.lib.registry.DeployerRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class PanelSpecialSetupPacket extends BlockEntityConfigurationPacket<FactoryPanelBlockEntity> {
    public static final StreamCodec<RegistryFriendlyByteBuf, PanelSpecialSetupPacket> STREAM_CODEC = StreamCodec.composite(
            FactoryPanelPosition.STREAM_CODEC, packet -> packet.to,
            FactoryPanelPosition.STREAM_CODEC, packet -> packet.from,
            ByteBufCodecs.BOOL, packet -> packet.disconnect,
            PanelSpecialSetupPacket::new
    );

    private final FactoryPanelPosition to;
    private final FactoryPanelPosition from;
    private final boolean disconnect;

    public PanelSpecialSetupPacket(FactoryPanelPosition to, FactoryPanelPosition from, boolean disconnect) {
        super(to.pos());
        this.to = to;
        this.from = from;
        this.disconnect = disconnect;
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return DeployerPackets.DISCONNECT_PANEL;
    }

    @Override
    protected void applySettings(ServerPlayer player, FactoryPanelBlockEntity be) {
        FactoryPanelBehaviour behaviour = be.panels.get(to.slot());
        if(behaviour == null)
            return;
        assert be.getLevel() != null;

        if(behaviour.targetedBy.containsKey(from)) {
            FactoryPanelBehaviour source = FactoryPanelBehaviour.at(be.getLevel(), from);

            if(!disconnect) {
                FactoryPanelConnection conn = behaviour.targetedBy.get(from);
                var set = ProvidesConnection.getPossibleConnections(source, behaviour);
                var pc = ProvidesConnection.getCurrentConnection(conn, () -> set.stream().findFirst().orElse(null));
                if(pc != null) {
                    List<PanelConnection<?>> li = new ArrayList<>(set);
                    int current = li.indexOf(pc);
                    int nextIndex = (current + 1) % li.size();
                    PanelConnection<?> pc1 = li.get(nextIndex);
                    ((FPCExtension)conn).deployer$setLinkMode(pc1);
                    var id = DeployerRegistries.PANEL_CONNECTION.getKey(pc1);
                    if(id != null)
                        player.displayClientMessage(
                                Component.translatable(
                                        "deployer.factory_panel.transferring_switch",
                                        Component.translatable(
                                                "panel_connection." + id.getNamespace() + "." + id.getPath()))
                                        .withStyle(ChatFormatting.GREEN),
                                true
                        );
                }
            } else {
                behaviour.targetedBy.remove(from);
                if(source != null) {
                    source.targeting.remove(behaviour.getPanelPosition());
                    source.blockEntity.sendData();
                }
            }
        } else if(behaviour.targetedByLinks.containsKey(from.pos())) {
            behaviour.targetedByLinks.remove(from.pos());
            FactoryPanelSupportBehaviour source = FactoryPanelBehaviour.linkAt(be.getLevel(), from);
            if(source != null) {
                source.disconnect(behaviour);
            }
        } else {
            var extra = ((FPBExtension)behaviour).deployer$getExtra();
            extra.remove(from.pos());
        }

        be.notifyUpdate();
    }
}
