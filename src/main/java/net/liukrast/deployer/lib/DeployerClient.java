package net.liukrast.deployer.lib;

import com.mojang.blaze3d.platform.NativeImage;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import net.createmod.catnip.config.ui.BaseConfigScreen;
import net.createmod.catnip.platform.CatnipServices;
import net.liukrast.deployer.lib.helper.ClientRegisterHelpers;
import net.liukrast.deployer.lib.logistics.board.connection.PanelSpecialSetupPacket;
import net.liukrast.deployer.lib.logistics.board.renderer.ScrollPanelRenderer;
import net.liukrast.deployer.lib.logistics.board.renderer.StockPanelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

import java.net.URI;

@Mod(value = DeployerConstants.MOD_ID, dist = Dist.CLIENT)
public class DeployerClient {

    public static FactoryPanelConnection SELECTED_CONNECTION = null;
    public static FactoryPanelPosition SELECTED_SOURCE = null;

    private static final IntObjectMap<ResourceLocation> CAPE_TEXTURES = new IntObjectHashMap<>();

    public DeployerClient(IEventBus eventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, DeployerConfig.Client.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parent) -> new BaseConfigScreen(parent, modContainer.getModId()));
        eventBus.register(this);
        NeoForge.EVENT_BUS.addListener(this::clientPlayerNetworkLoggingOut);
        NeoForge.EVENT_BUS.addListener(this::onTickPost);
    }

    @SubscribeEvent
    private void fMLClientSetup(FMLClientSetupEvent event) {
        ClientRegisterHelpers.registerPanelTicker(ScrollPanelRenderer::tick);
        ClientRegisterHelpers.registerPanelTicker(StockPanelRenderer::tick);
        ClientRegisterHelpers.registerPanelRenderer(StockPanelRenderer::render);

        if(FMLEnvironment.production) return;
        /*var rs = RandomSource.create();
        for(int i = 0; i < 30; i++) {
            var opt =  BuiltInRegistries.ITEM.getRandom(rs);
            Item item = opt.map(Holder.Reference::value).orElse(Items.STONE);
            ClientRegisterHelpers.registerStockKeeperTab((a,b) -> new KeeperTabScreen(a,b, Component.literal("Dev test"), item));
            ClientRegisterHelpers.registerRedstoneRequesterTab(null, (menu,type, data)-> new RequesterTabScreen<>(menu, Component.literal("Dev test"), item, type, data));
        }*/
    }

    private void clientPlayerNetworkLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        CAPE_TEXTURES.clear();
    }

    public static ResourceLocation getCape(int id) {
        if(CAPE_TEXTURES.containsKey(id)) return CAPE_TEXTURES.get(id);

        CAPE_TEXTURES.put(id, null);
        new Thread(() -> {
            try {
                NativeImage nativeImage = NativeImage.read(URI.create("https://liukrast.net/assets/liukrast/textures/capes/" + id + ".png?t=" + System.currentTimeMillis()).toURL().openStream());
                CAPE_TEXTURES.put(id, Minecraft.getInstance().getTextureManager().register("deployer_capes/", new DynamicTexture(nativeImage)));
            } catch (Exception e) {
                CAPE_TEXTURES.put(id, null);
            }
        }).start();

        return null;
    }

    private void onTickPost(InputEvent.InteractionKeyMappingTriggered event) {
        if(SELECTED_CONNECTION == null) return;
        assert Minecraft.getInstance().player != null;
        ItemStack handItem = Minecraft.getInstance().player.getItemInHand(event.getHand());
        if(!handItem.is(AllItems.WRENCH.get())) return;
        if(event.getKeyMapping() == Minecraft.getInstance().options.keyAttack) {
            event.setCanceled(true);
            CatnipServices.NETWORK.sendToServer(new PanelSpecialSetupPacket(SELECTED_SOURCE, SELECTED_CONNECTION.from, true));
        } else if(event.getKeyMapping() == Minecraft.getInstance().options.keyUse) {
            event.setCanceled(true);
            CatnipServices.NETWORK.sendToServer(new PanelSpecialSetupPacket(SELECTED_SOURCE, SELECTED_CONNECTION.from, false));
        }
    }
}
