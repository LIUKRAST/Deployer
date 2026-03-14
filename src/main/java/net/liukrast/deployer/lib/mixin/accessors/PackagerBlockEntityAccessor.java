package net.liukrast.deployer.lib.mixin.accessors;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryTrackerBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PackagerBlockEntity.class)
public interface PackagerBlockEntityAccessor {
    @Accessor("invVersionTracker")
    VersionedInventoryTrackerBehaviour getInvVersionTracker();

    @Invoker("getLinkPos")
    BlockPos invokeGetLinkPos();

    @Accessor("advancements")
    AdvancementBehaviour getAdvancement();

    @Invoker("supportsBlockEntity")
    boolean invokeSupportsBlockEntity(BlockEntity blockEntity);
}
