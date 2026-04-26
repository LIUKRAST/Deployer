package net.liukrast.deployer.lib.helper;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.createmod.catnip.render.SuperByteBuffer;
import net.liukrast.deployer.lib.helper.client.PackageVisualExtension;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.liukrast.deployer.lib.logistics.board.PanelType;
import net.liukrast.deployer.lib.logistics.board.connection.PanelConnection;
import net.liukrast.deployer.lib.logistics.board.screen.GaugeSlot;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.packager.screen.KeeperSourceContext;
import net.liukrast.deployer.lib.logistics.packager.screen.KeeperTabScreen;
import net.liukrast.deployer.lib.logistics.packager.screen.RequesterTabScreen;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.ApiStatus;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ClientRegisterHelpers {
    /* HELPERS */
    public static void registerPackageRenderer4ChainConveyor(SuperByteBufferFactory renderer) {
        CHAIN_RENDERERS.add(renderer);
    }
    public static void registerPackageRenderer4Entity(EntityRenderer renderer) {
        ENTITY_RENDERERS.add(renderer);
    }
    public static void registerPackageVisual4ChainConveyor(ChainConveyorFactory factory) {
        CHAIN_VISUALS.add(factory);
    }
    public static void registerPackageVisual4Entity(EntityFactory.Simple factory, Predicate<PackageEntity> predicate) {
        registerPackageVisual4Entity(new EntityFactory() {
            @Override
            public PackageVisualExtension.Entity create(VisualizationContext context, PackageEntity entity, float partialTicks) {
                return factory.create(context, entity, partialTicks);
            }

            @Override
            public boolean validForPackage(PackageEntity box) {
                return predicate.test(box);
            }
        });
    }

    public static void registerPackageVisual4Entity(EntityFactory factory) {
        ENTITY_VISUALS.add(factory);
    }
    public static <A extends AbstractPanelBehaviour> void registerGaugeSlot(PanelType<A> type, PanelFactory<A> factory) {
        GAUGE_MAP.put(type, factory);
    }
    public static void registerStockKeeperTab(BiFunction<KeeperSourceContext, StockKeeperRequestMenu, KeeperTabScreen> screenFactory) {
        KEEPER_TABS.add(screenFactory);
    }
    public static <V> void registerRedstoneRequesterTab(StockInventoryType<?, V, ?> type, RequesterFactory<V> factory) {
        REQUESTER_TABS.add(new RequesterBuilder<>(type, factory));
    }

    public static void registerPanelTicker(Consumer<AbstractPanelBehaviour> ticker) {
        PANEL_TICKERS.add(ticker);
    }

    public static void registerPanelRenderer(PanelRenderer renderer) {
        PANEL_RENDERERS.add(renderer);
    }

    public static void registerSpecialHovering(BooleanSupplier flag, IHaveHoveringInformation info) {
        SPECIAL_HOVERS.add(new Pair<>(flag, info));
    }

    public static void registerSpecialGoggle(BooleanSupplier flag, IHaveGoggleInformation info) {
        registerSpecialHovering(flag, (IHaveHoveringInformation) info);
    }



    /* INTERNAL CONTAINERS */
    private static final Map<PanelType<?>, PanelFactory<?>> GAUGE_MAP = new HashMap<>();
    private static final List<SuperByteBufferFactory> CHAIN_RENDERERS = new ArrayList<>();
    private static final List<EntityRenderer> ENTITY_RENDERERS = new ArrayList<>();
    private static final List<ChainConveyorFactory> CHAIN_VISUALS = new ArrayList<>();
    private static final List<EntityFactory> ENTITY_VISUALS = new ArrayList<>();
    private static final List<BiFunction<KeeperSourceContext, StockKeeperRequestMenu, KeeperTabScreen>> KEEPER_TABS = new ArrayList<>();
    private static final List<Consumer<AbstractPanelBehaviour>> PANEL_TICKERS = new ArrayList<>();
    private static final List<PanelRenderer> PANEL_RENDERERS = new ArrayList<>();
    private static final List<RequesterBuilder<?>> REQUESTER_TABS = new ArrayList<>();
    private static final List<Pair<BooleanSupplier, IHaveHoveringInformation>> SPECIAL_HOVERS = new ArrayList<>();

    private ClientRegisterHelpers() {}


    /* INTERNAL GETTERS */
    @SuppressWarnings("unchecked")
    private static <A extends AbstractPanelBehaviour> GaugeSlot<A> createSlot(
            PanelFactory<?> factory, A panel, PanelConnection<?> connection) {
        return ((PanelFactory<A>) factory).create(panel, connection);
    }

    @ApiStatus.Internal
    public static <A extends AbstractPanelBehaviour> GaugeSlot<A> getSlot(
            A panel, PanelConnection<?> connection) {
        if (panel == null) return null;
        PanelFactory<?> factory = GAUGE_MAP.get(panel.getPanelType());
        if(factory == null) return null;
        return createSlot(factory, panel, connection);
    }

    @ApiStatus.Internal
    public static Iterable<SuperByteBufferFactory> getChainRenderers() {
        return CHAIN_RENDERERS;
    }

    @ApiStatus.Internal
    public static Iterable<EntityRenderer> getEntityRenderers() {
        return ENTITY_RENDERERS;
    }

    @ApiStatus.Internal
    public static Stream<ChainConveyorFactory> getChainVisuals() {
        return CHAIN_VISUALS.stream();
    }

    @ApiStatus.Internal
    public static Stream<EntityFactory> getEntityVisuals() {
        return ENTITY_VISUALS.stream();
    }

    @ApiStatus.Internal
    public static Stream<BiFunction<KeeperSourceContext, StockKeeperRequestMenu, KeeperTabScreen>> getKeeperTabs() {
        return KEEPER_TABS.stream();
    }

    @ApiStatus.Internal
    public static Iterable<Consumer<AbstractPanelBehaviour>> getPanelTickers() {
        return PANEL_TICKERS;
    }

    @ApiStatus.Internal
    public static Iterable<PanelRenderer> getPanelRenderers() {
        return PANEL_RENDERERS;
    }

    @ApiStatus.Internal
    public static Stream<RequesterBuilder<?>> getRequesterTabs() {
        return REQUESTER_TABS.stream();
    }

    @ApiStatus.Internal
    public static Iterable<Pair<BooleanSupplier, IHaveHoveringInformation>> getSpecialHovers() {
        return SPECIAL_HOVERS;
    }

    /* FUNCTIONAL INTERFACES */
    @FunctionalInterface
    public interface SuperByteBufferFactory {
        SuperByteBuffer[] create(ChainConveyorBlockEntity be, ChainConveyorPackage box, float partialTicks);
    }

    @FunctionalInterface
    public interface EntityRenderer {
        void render(PackageEntity entity, float yaw, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light);
    }

    @FunctionalInterface
    public interface ChainConveyorFactory {
        PackageVisualExtension.ChainConveyor create(VisualizationContext context, ChainConveyorBlockEntity be, float partialTicks);
    }

    @FunctionalInterface
    public interface EntityFactory {
        PackageVisualExtension.Entity create(VisualizationContext context, PackageEntity entity, float partialTicks);
        default boolean validForPackage(PackageEntity box) {
            return true;
        }

        interface Simple {
            PackageVisualExtension.Entity create(VisualizationContext context, PackageEntity entity, float partialTicks);
        }
    }

    @FunctionalInterface
    public interface PanelRenderer {
        void render(AbstractPanelBehaviour apb, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay);
    }

    @FunctionalInterface
    public interface PanelFactory<A extends AbstractPanelBehaviour> {
        GaugeSlot<A> create(A panel, PanelConnection<?> connection);
    }

    public record RequesterBuilder<V>(StockInventoryType<?,V,?> type, RequesterFactory<V> factory) {}

    @FunctionalInterface
    public interface RequesterFactory<V> {
        RequesterTabScreen<V> create(RedstoneRequesterMenu menu, StockInventoryType<?, V, ?> type, GenericOrderContained<V> orderData);
    }
}
