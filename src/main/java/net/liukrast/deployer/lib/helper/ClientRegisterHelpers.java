package net.liukrast.deployer.lib.helper;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.createmod.catnip.render.SuperByteBuffer;
import net.liukrast.deployer.lib.helper.client.PackageVisualExtension;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.liukrast.deployer.lib.logistics.board.GaugeSlot;
import net.liukrast.deployer.lib.logistics.board.PanelType;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.packager.screen.KeeperTabScreen;
import net.liukrast.deployer.lib.logistics.packager.screen.StockRequesterPage;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.function.BiFunction;
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
    public static <A extends AbstractPanelBehaviour> void registerGaugeSlot(PanelType<A> type, GaugeSlot<?, A> slot) {
        GAUGE_MAP.put(type, slot);
    }
    public static void registerStockKeeperTab(BiFunction<StockTickerBlockEntity, StockKeeperRequestMenu, KeeperTabScreen> screenFactory) {
        KEEPER_TABS.add(screenFactory);
    }
    public static <V> void registerRedstoneRequesterTab(RequesterFactory<V> factory) {
        REQUESTER_TABS.add(factory);
    }

    public static void registerPanelTicker(Consumer<AbstractPanelBehaviour> ticker) {
        PANEL_TICKERS.add(ticker);
    }

    public static void registerPanelRenderer(PanelRenderer renderer) {
        PANEL_RENDERERS.add(renderer);
    }

    /* INTERNAL CONTAINERS */
    private static final Map<PanelType<?>, GaugeSlot<?,?>> GAUGE_MAP = new HashMap<>();
    private static final List<SuperByteBufferFactory> CHAIN_RENDERERS = new ArrayList<>();
    private static final List<EntityRenderer> ENTITY_RENDERERS = new ArrayList<>();
    private static final List<ChainConveyorFactory> CHAIN_VISUALS = new ArrayList<>();
    private static final List<EntityFactory> ENTITY_VISUALS = new ArrayList<>();
    private static final List<BiFunction<StockTickerBlockEntity, StockKeeperRequestMenu, KeeperTabScreen>> KEEPER_TABS = new ArrayList<>();
    private static final List<Consumer<AbstractPanelBehaviour>> PANEL_TICKERS = new ArrayList<>();
    private static final List<PanelRenderer> PANEL_RENDERERS = new ArrayList<>();
    private static final List<RequesterFactory<?>> REQUESTER_TABS = new ArrayList<>();

    private ClientRegisterHelpers() {}


    /* INTERNAL GETTERS */
    @ApiStatus.Internal
    @SuppressWarnings("unchecked")
    public static <A extends AbstractPanelBehaviour> GaugeSlot<?, A> getSlot(PanelType<A> type) {
        return (GaugeSlot<?, A>) GAUGE_MAP.get(type);
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
    public static Stream<BiFunction<StockTickerBlockEntity, StockKeeperRequestMenu, KeeperTabScreen>> getKeeperTabs() {
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
    public static Stream<RequesterFactory<?>> getRequesterTabs() {
        return REQUESTER_TABS.stream();
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
    public interface RequesterFactory<V> {
        StockRequesterPage<V> create(RedstoneRequesterMenu menu);
    }
}
