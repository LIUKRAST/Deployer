package net.liukrast.deployer.lib.mixin.accessors;

import com.simibubi.create.content.fluids.FlowSource;
import com.simibubi.create.foundation.ICapabilityProvider;
import net.createmod.catnip.math.BlockFace;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FlowSource.class)
public interface FlowSourceAccessor {

    @Accessor("location")
    BlockFace getLocation();

    @Accessor("EMPTY")
    static ICapabilityProvider<IFluidHandler> getEMPTY() {
        throw new AssertionError("Mixin injection failed");
    }
}