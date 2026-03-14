package net.liukrast.deployer.lib;

import com.mojang.blaze3d.platform.NativeImage;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import net.createmod.catnip.config.ui.BaseConfigScreen;
import net.liukrast.deployer.lib.helper.ClientRegisterHelpers;
import net.liukrast.deployer.lib.logistics.board.renderer.ScrollPanelRenderer;
import net.liukrast.deployer.lib.logistics.board.renderer.StockPanelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

import java.net.URI;

@Mod(value = DeployerConstants.MOD_ID, dist = Dist.CLIENT)
public class DeployerClient {

    private static final IntObjectMap<ResourceLocation> CAPE_TEXTURES = new IntObjectHashMap<>();

    public DeployerClient(IEventBus eventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, DeployerConfig.Client.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parent) -> new BaseConfigScreen(parent, modContainer.getModId()));
        eventBus.register(this);
        NeoForge.EVENT_BUS.addListener(this::clientPlayerNetworkLoggingOut);
    }

    @SubscribeEvent
    private void fMLClientSetup(FMLClientSetupEvent event) {
        ClientRegisterHelpers.registerPanelTicker(ScrollPanelRenderer::tick);
        ClientRegisterHelpers.registerPanelTicker(StockPanelRenderer::tick);
        ClientRegisterHelpers.registerPanelRenderer(StockPanelRenderer::render);
    }

    private void clientPlayerNetworkLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        CAPE_TEXTURES.clear();
    }

    public static ResourceLocation getCape(int id) {
        if(CAPE_TEXTURES.containsKey(id)) return CAPE_TEXTURES.get(id);

        CAPE_TEXTURES.put(id, null);
        new Thread(() -> {
            try {
                NativeImage nativeImage = NativeImage.read(URI.create("https://liukrast.net/assets/liukrast/textures/capes/" + id + ".png").toURL().openStream());
                CAPE_TEXTURES.put(id, Minecraft.getInstance().getTextureManager().register("deployer_capes/", new DynamicTexture(nativeImage)));
            } catch (Exception e) {
                CAPE_TEXTURES.put(id, null);
            }
        }).start();

        return null;
    }
}
