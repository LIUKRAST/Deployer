package net.liukrast.deployer.lib.logistics.packager;

import com.simibubi.create.content.logistics.box.PackageStyles;
import net.minecraft.resources.ResourceLocation;

/**
 * @param idAndPackage defines the mod ID you're using and the package name. For instance, a mod that adds {@code bottle} packages should input {@code mod_id:bottle}
 */

/**
 * A class that allows to create package styles
 * */
public record CustomPackageStyle(ResourceLocation idAndPackage, String type, int width, int height, float riggingOffset, boolean rare) {
    public ResourceLocation getItemId() {
        String size = "_" + width + "x" + height;
        String id = type + "_" + idAndPackage.getPath() + (rare ? "" : size);
        return ResourceLocation.fromNamespaceAndPath(idAndPackage.getNamespace(), id);
    }

    public ResourceLocation getRiggingModel() {
        String size = width + "x" + height;
        return ResourceLocation.fromNamespaceAndPath(idAndPackage.getNamespace(), "item/" + idAndPackage.getPath() + "/rigging_" + size);
    }

    public PackageStyles.PackageStyle toOriginal() {
        return new PackageStyles.PackageStyle(type, width, height, riggingOffset, rare);
    }
};
