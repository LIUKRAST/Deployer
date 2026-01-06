package net.liukrast.fluid.registry;

import com.google.common.collect.ImmutableList;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.liukrast.deployer.lib.logistics.packager.CustomPackageStyle;
import net.liukrast.fluid.TestConstants;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.stream.Stream;

import static com.simibubi.create.AllPartialModels.*;

public class RegisterPackageStyles {
    private RegisterPackageStyles() {}

    @ApiStatus.Internal
    @Unmodifiable
    public static final List<CustomPackageStyle> BOTTLE_STYLES = ImmutableList.of(
            new CustomPackageStyle(TestConstants.id("bottle"), "copper", 12, 12, 23f, false),
            new CustomPackageStyle(TestConstants.id("bottle"), "copper", 10, 12, 22f, false),
            new CustomPackageStyle(TestConstants.id("bottle"), "copper", 10, 8, 18f, false),
            new CustomPackageStyle(TestConstants.id("bottle"), "copper", 12, 10, 21f, false)
    );

    @ApiStatus.Internal
    @Unmodifiable
    public static final List<CustomPackageStyle> BATTERY_STYLES = ImmutableList.of(
            new CustomPackageStyle(TestConstants.id("battery"), "brass", 12, 12, 23f, false),
            new CustomPackageStyle(TestConstants.id("battery"), "brass", 10, 12, 22f, false),
            new CustomPackageStyle(TestConstants.id("battery"), "brass", 10, 8, 18f, false),
            new CustomPackageStyle(TestConstants.id("battery"), "brass", 12, 10, 21f, false)
    );

    static {
        Stream.concat(
                BOTTLE_STYLES.stream(),
                BATTERY_STYLES.stream()
        ).forEach(style -> {
            ResourceLocation key = style.getItemId();
            PartialModel model = PartialModel.of(TestConstants.id("item/" + key.getPath()));
            PACKAGES.put(key, model);
            if (!style.rare())
                PACKAGES_TO_HIDE_AS.add(model);
            PACKAGE_RIGGING.put(key, PartialModel.of(style.getRiggingModel()));
        });
    }

    @ApiStatus.Internal
    public static void init() {}
}
