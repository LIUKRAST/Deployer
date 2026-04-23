package net.liukrast.deployer.lib.helper;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlockEntity;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderSceneBuilder;
import net.createmod.ponder.foundation.element.ElementLinkImpl;
import net.liukrast.deployer.lib.helper.ponder.CreatePartialInstruction;
import net.liukrast.deployer.lib.helper.ponder.PartialElement;
import net.liukrast.deployer.lib.helper.ponder.PartialElementImpl;
import net.liukrast.deployer.lib.mixinExtensions.FPBExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * Helper class that contains several methods to help developers create ponder scenes
 * */
@SuppressWarnings("unused")
public class PonderSceneHelpers {
    private PonderSceneHelpers() {}

    /**
     * Automatically initializes a gauge scene, shows the baseplate, sets Y and view, and starts showing the init area
     * @param builder The scene builder
     * @param util The scene building util
     * @param id The scene id
     * */
    public static CreateSceneBuilder simpleInit(SceneBuilder builder, SceneBuildingUtil util, String id) {
        return simpleInit(builder, util, id, 7);
    }

    public static CreateSceneBuilder simpleInit(SceneBuilder builder, SceneBuildingUtil util, String id, int width) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title(id, "");
        scene.configureBasePlate(0, 0, width);
        scene.scaleSceneView(0.825f);
        scene.setSceneOffsetY(-2f);
        scene.world().showIndependentSection(util.select().fromTo(width-1, 0, 0, 0, 0, width-1), Direction.UP);
        scene.idle(10);
        return scene;
    }

    /**
     * Automatically shows a text, idles the scene and attach a keyframe if requested
     * @param builder the scene builder
     * @param pos the position to show the text box
     * @param time the time to show the text box
     * @param keyframe whether we want to add a keyframe to the scene or not
     * */
    public static void displayText(SceneBuilder builder, BlockPos pos, int time, boolean keyframe) {
        var overlay = builder.overlay()
                .showText(time)
                .text("")
                .placeNearTarget()
                .pointAt(pos.getCenter().add(-0.25f, 0.25f,0));
        if(keyframe) overlay.attachKeyFrame();
        builder.idle(time+20);
    }

    /**
     * Toggles redstone at a position but also shows particles
     * @param builder The scene builder
     * @param pos The target position
     * */
    public static void activateRedstone(SceneBuilder builder, BlockPos pos) {
        builder.world().toggleRedstonePower(builder.getScene().getSceneBuildingUtil().select().position(pos));
        builder.effects().indicateRedstone(pos);
    }

    /**
     * Sets the text inside a sign
     * @param builder The scene builder
     * @param pos The sign position
     * @param line The sign line we want to modify
     * @param text The component we want to insert inside the sign
     * */
    public static void setSignText(SceneBuilder builder, BlockPos pos, int line, Component text) {
        builder.world().modifyBlockEntity(pos, SignBlockEntity.class, be -> be.setText(be.getText(true).setMessage(line, text), true));
    }

    /**
     * Sets the text inside a nixie tube
     * @param builder The scene builder
     * @param start The position of the first nixie tube on the line
     * @param text The component we want to set
     * @param length The length of the nixie tube line
     * @param direction The direction of the nixie tube line
     * */
    public static void setNixieTubeText(SceneBuilder builder, BlockPos start, Component text, int length, Direction direction) {
        for(int i = 0; i < length; i++) {
            final int index = i;
            builder.world().modifyBlockEntityNBT(builder.getScene().getSceneBuildingUtil().select().position(start.relative(direction, index)), NixieTubeBlockEntity.class, nbt -> {
                String asRaw = Component.Serializer.toJson(text, builder.world().getHolderLookupProvider());
                nbt.putString("RawCustomText", asRaw);
                nbt.putString("CustomText", asRaw);
                nbt.putInt("CustomTextIndex", index);
            });
        }
    }

    /**
     * Helpers for factory and custom gauges
     * */
    public static class Gauge {
        private Gauge() {}
        /**
         * Sets an item inside a factory gauge
         * @param builder The scene builder
         * @param gauge The gauge position
         * @param item The item to set
         * */
        public static void setPanelItem(SceneBuilder builder, FactoryPanelPosition gauge, ItemStack item) {
            withGaugeDo(builder, gauge, pb -> pb.setFilter(item));
        }

        /**
         * Toggles a factory gauge to redstonePowered on or off
         * @param builder The scene builder
         * @param gauge The gauge position
         * @param power The power state to set
         * */
        public static void setPanelPowered(SceneBuilder builder, FactoryPanelPosition gauge, boolean power) {
            withGaugeDo(builder, gauge, pb -> pb.redstonePowered = power);
        }

        /**
         * Activates/deactivates a panel
         * @param builder The scene builder
         * @param gauge The gauge position
         * @param visible The visible state to set
         * */
        public static void setPanelVisible(SceneBuilder builder, FactoryPanelPosition gauge, boolean visible) {
            withGaugeDo(builder, gauge, pb -> pb.active = visible);
        }

        /**
         * Sets a gauge to "Passive" (gray connection line).
         * @param builder The scene builder
         * @param gauge The gauge position
         * */
        public static void setPanelPassive(SceneBuilder builder, FactoryPanelPosition gauge) {
            withGaugeDo(builder, gauge, pb -> pb.count = 0);
        }

        /**
         * Sets a gauge to "Satisfied" (green connection line).
         * Note that this only works by making your scene stock inventory have 1 item of the type requested by the gauge
         * @param builder The scene builder
         * @param gauge The gauge position
         * */
        public static void setPanelSatisfied(SceneBuilder builder, FactoryPanelPosition gauge) {
            withGaugeDo(builder, gauge, pb -> pb.count = 1);
        }

        /**
         * Sets a gauge to "Not satisfied" (before crafting).
         * Note that this only works by making your scene stock inventory have 1 item of the type requested by the gauge
         * @param builder The scene builder
         * @param gauge The gauge position
         * */
        public static void setPanelNotSatisfied(SceneBuilder builder, FactoryPanelPosition gauge) {
            withGaugeDo(builder, gauge, pb -> pb.count = 2);
        }

        /**
         * Sets a gauge to the "Crafting" stage (blue connection line)
         * @param builder The scene builder
         * @param util The scene building util
         * @param gauge The gauge position
         * */
        public static void setPanelCrafting(SceneBuilder builder, SceneBuildingUtil util, FactoryPanelPosition gauge) {
            builder.world().modifyBlockEntityNBT(util.select().position(gauge.pos()), FactoryPanelBlockEntity.class, tag -> {
                CompoundTag panelTag = tag.getCompound(CreateLang.asId(gauge.slot().name()));
                panelTag.putInt("LastPromised", 1);
            });
        }

        /**
         * Flashes a gauge (the green bulb will flash)
         * @param builder The scene builder
         * @param gauge The gauge position
         * */
        public static void flash(SceneBuilder builder, FactoryPanelPosition gauge) {
            withGaugeDo(builder, gauge, pb -> pb.bulb.setValue(1));
        }

        /**
         * Sets a gauge's connection amount
         * @param builder The scene builder
         * @param from The gauge source position of the connection
         * @param to The gauge target position of the connection
         * @param amount The amount to set
         * */
        public static void setConnectionAmount(SceneBuilder builder, FactoryPanelPosition from, FactoryPanelPosition to, int amount) {
            withGaugeDo(builder, from, pb -> pb.targetedBy.get(to).amount = amount);
        }

        /**
         * Removes all connection of a gauge
         * @param builder The scene builder
         * @param gauge The gauge position
         * */
        public static void removePanelConnections(SceneBuilder builder, FactoryPanelPosition gauge) {
            withGaugeDo(builder, gauge, FactoryPanelBehaviour::disconnectAll);
        }

        /**
         * Switches the arrow mode of a gauge (right-click with wrench)
         * @param builder The scene builder
         * @param gauge The gauge position
         * @param from The arrow source
         * @param mode The mode (0 -> 3)
         * */
        public static void setArrowMode(SceneBuilder builder, FactoryPanelPosition gauge, FactoryPanelPosition from, int mode) {
            withGaugeDo(builder, gauge, pb -> {
                FactoryPanelConnection connection = pb.targetedBy.get(new FactoryPanelPosition(from.pos(), from.slot()));
                if (connection == null) {
                    connection = pb.targetedByLinks.get(from.pos());
                    if (connection == null) {
                        connection = ((FPBExtension)pb).deployer$getExtra().get(from.pos());
                        if(connection == null) return;
                    }
                }
                connection.arrowBendMode = mode;
            });
        }

        /**
         * Adds a panel connection
         * @param builder The scene builder
         * @param gauge The target position
         * @param from The connection source
         * */
        public static void addPanelConnection(SceneBuilder builder, FactoryPanelPosition gauge, FactoryPanelPosition from) {
            withGaugeDo(builder, gauge, pb -> pb.addConnection(new FactoryPanelPosition(from.pos(), from.slot())));
        }

        /**
         * Executes something on a gauge
         * @param builder The scene builder
         * @param gauge The gauge position
         * @param consumer The action to perform on the gauge behavior
         * */
        public static void withGaugeDo(SceneBuilder builder, FactoryPanelPosition gauge, Consumer<FactoryPanelBehaviour> consumer) {
            builder.world()
                    .modifyBlockEntity(gauge.pos(), FactoryPanelBlockEntity.class, be -> consumer.accept(be.panels.get(gauge.slot())));
        }

        public static void setGaugeCount(SceneBuilder scene, FactoryPanelPosition gauge, int count) {
            withGaugeDo(scene, gauge, fb -> fb.count = count);
        }
    }

    public static void setAnalogLever(SceneBuilder scene, Selection sel, int power) {
        scene.world().modifyBlockEntityNBT(sel, AnalogLeverBlockEntity.class,
                nbt -> nbt.putInt("State", power));
    }

    public static ScreenElement createComponent(Component text) {
        return (graphics, x, y) -> graphics.drawCenteredString(Minecraft.getInstance().font, text, x+8, y+4, -1);
    }


    public static ElementLink<PartialElement> createPartialModel(PonderSceneBuilder builder, PartialModel model, Vec3 position) {
        return createPartialModel(builder, model, position, 10, Direction.DOWN);
    }

    public static ElementLink<PartialElement> createPartialModel(PonderSceneBuilder builder, PartialModel model, Vec3 position, int fadeInTicks, Direction fadeDirection) {
        ElementLink<PartialElement> link = new ElementLinkImpl<>(PartialElement.class);
        PartialElement element = new PartialElementImpl(model, position);
        builder.addInstruction(new CreatePartialInstruction(10, Direction.DOWN, element));
        builder.addInstruction(scene1 -> scene1.linkElement(element, link));
        return link;
    }
}
