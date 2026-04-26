package net.liukrast.deployer.lib.logistics;

import com.simibubi.create.content.logistics.box.PackageStyles;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public interface IPromiseVisuals {
    default Component getPromisedComponent(int amount) { return null; }
    default ItemStack getPromisedBox() { return PackageStyles.getDefaultBox(); };
}