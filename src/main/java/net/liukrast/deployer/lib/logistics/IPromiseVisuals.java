package net.liukrast.deployer.lib.logistics;

import com.simibubi.create.content.logistics.box.PackageStyles;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

/**
 * This can be implemented on any class extending {@link com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour}
 * to replace the promise component and package stack
 */
@ApiStatus.OverrideOnly
public interface IPromiseVisuals {
    /**
     * Replaces the default promise {@link Component}
     *
     * @param amount the promised amount
     * @return the promise component or null to keep the default component
     */
    default Component getPromisedComponent(int amount) {
        return null;
    }

    /**
     * Replaces the default package stack with a custom {@link ItemStack}
     *
     * @return a custom stack
     */
    default ItemStack getPromisedBox() {
        return PackageStyles.getDefaultBox();
    }
}