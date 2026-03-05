package net.liukrast.deployer.lib.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.event.IModBusEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PanelClientEvent extends Event implements IModBusEvent {

    public List<Consumer<AbstractPanelBehaviour>> tickerList = new ArrayList<>();
    public List<Renderer> renderList = new ArrayList<>();

    public static List<Consumer<AbstractPanelBehaviour>> fireTicker() {
        var event = new PanelClientEvent();
        ModLoader.postEvent(event);
        return event.tickerList;
    }

    public static List<Renderer> fireRenderer() {
        var event = new PanelClientEvent();
        ModLoader.postEvent(event);
        return event.renderList;
    }

    private PanelClientEvent() {}

    public void registerTicker(Consumer<AbstractPanelBehaviour> ticker) {
        tickerList.add(ticker);
    }

    public void registerRenderer(Renderer renderer) {
        renderList.add(renderer);
    }

    public interface Renderer {
        void render(AbstractPanelBehaviour apb, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay);
    }
}
