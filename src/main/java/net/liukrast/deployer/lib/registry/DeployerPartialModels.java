package net.liukrast.deployer.lib.registry;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.box.PackageStyles;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.lang.Lang;
import net.liukrast.deployer.lib.Deployer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

public class DeployerPartialModels {
    private DeployerPartialModels() {}

    public static final Map<Direction, PartialModel> ARROW_HIGHLIGHTS = new EnumMap<>(Direction.class);
    public static final Map<Direction, PartialModel> LINES_HIGHLIGHTS = new EnumMap<>(Direction.class);

    static {
        for(PackageStyles.PackageStyle style : DeployerPackages.STYLES) {
            ResourceLocation key = Deployer.CONSTANTS.id(style.getItemId().getPath());
            PartialModel model = PartialModel.of(Deployer.CONSTANTS.id("item/" + key.getPath()));
            AllPartialModels.PACKAGES.put(key, model);
            if (!style.rare())
                AllPartialModels.PACKAGES_TO_HIDE_AS.add(model);
            AllPartialModels.PACKAGE_RIGGING.put(key, PartialModel.of(style.getRiggingModel()));
        }

        for (Direction d : Iterate.horizontalDirections) {
            ARROW_HIGHLIGHTS.put(d, block("panel_connection_highlights/arrow_" + Lang.asId(d.name())));
            LINES_HIGHLIGHTS.put(d, block("panel_connection_highlights/line_" + Lang.asId(d.name())));
        }

    }

    private static PartialModel block(String path) {
        return PartialModel.of(Deployer.CONSTANTS.id("block/" + path));
    }

    public static void init() {}
}
