package net.liukrast.deployer.lib.logistics.board.cache;

import com.mojang.serialization.DynamicOps;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.foundation.networking.BlockEntityDataPacket;
import net.liukrast.deployer.lib.DeployerConfig;
import net.liukrast.deployer.lib.registry.DeployerPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Client-bound packet used to update the cache of a factory panel's behavior.
 * <p>
 * This packet transports a serialized cache (stored as a {@link CompoundTag})
 * and applies it client-side to the corresponding {@link FactoryPanelBehaviour},
 * if the behavior implements {@link CacheContainer}.
 *
 * <p>The packet identifies the target panel via a {@link FactoryPanelPosition},
 * which specifies both the block entity location and the panel slot index.
 *
 * <p>On receipt, the client decodes the cache values using the registry-aware
 * {@link DynamicOps} context, then loads them into the panel's cache container.
 *
 * @implNote This packet is only processed on the client side. It performs no action if:
 * <ul>
 *     <li>panel caching is disabled in the configuration,</li>
 *     <li>the targeted behavior does not implement {@link CacheContainer},</li>
 *     <li>the client world is not yet available.</li>
 * </ul>
 */
public class PanelCacheUpdatePacket extends BlockEntityDataPacket<FactoryPanelBlockEntity> {
    /**
     * Codec used to serialize and deserialize packet instances for network transport.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, PanelCacheUpdatePacket> STREAM_CODEC = StreamCodec.composite(
            FactoryPanelPosition.STREAM_CODEC, packet -> packet.position,
            ByteBufCodecs.COMPOUND_TAG, packet -> packet.compound,
            PanelCacheUpdatePacket::new
    );
    /**
     * The logical position of the targeted factory panel, including the block entity
     * coordinates and the internal panel slot.
     */
    private final FactoryPanelPosition position;
    /**
     * Serialized form of the panel's cache data.
     */
    private final CompoundTag compound;

    /**
     * Creates a new cache update packet for the given panel position and tag data.
     *
     * @param position the identifying position and slot of the factory panel
     * @param compound the serialized cache data to apply on the client
     */
    public PanelCacheUpdatePacket(FactoryPanelPosition position, CompoundTag compound) {
        super(position.pos());
        this.position = position;
        this.compound = compound;
    }

    /**
     * Applies the received cache to the targeted factory panel behavior.
     * <p>
     * If caching is enabled and the behavior implements {@link CacheContainer},
     * the method deserializes the stored values using the client's registry access
     * and replaces the behavior's local cache with the received one.
     *
     * @param blockEntity the factory panel block entity receiving the update
     *
     * @implNote This method must run strictly on the client side.
     * If the world is not
     * available or the behavior does not support caching, the packet is ignored.
     */
    @Override
    protected void handlePacket(FactoryPanelBlockEntity blockEntity) {
        if(!DeployerConfig.Client.PANEL_CACHING.get()) return;
        FactoryPanelBehaviour behaviour = blockEntity.panels.get(position.slot());
        if(!(behaviour instanceof CacheContainer<?> cacheContainer)) return;
        if(Minecraft.getInstance().level == null) return;
        var access = Minecraft.getInstance().level.registryAccess();
        DynamicOps<Tag> dynamicOps = access.createSerializationContext(NbtOps.INSTANCE);


        cacheContainer.setCache(compound, dynamicOps);
    }

    /**
     * @return the packet type associated with this update packet
     */
    @Override
    public PacketTypeProvider getTypeProvider() {
        return DeployerPackets.PANEL_CACHE_UPDATE;
    }
}
