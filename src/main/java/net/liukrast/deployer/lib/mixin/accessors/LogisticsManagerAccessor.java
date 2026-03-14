package net.liukrast.deployer.lib.mixin.accessors;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Random;

@Mixin(LogisticsManager.class)
public interface LogisticsManagerAccessor {
    @Accessor("r")
    static Random getR() {
        throw new AssertionError("Mixin injection failed");
    }

    @Invoker("getInventoryIdentifierFromLink")
    static InventoryIdentifier invokeGetInventoryIdentifierFromLink(LogisticallyLinkedBehaviour link) {
        throw new AssertionError("Mixin injection failed");
    }
}
