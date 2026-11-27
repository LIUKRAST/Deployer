package net.liukrast.deployer.lib.client;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.liukrast.deployer.lib.Deployer;
import net.liukrast.deployer.lib.DeployerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@ApiStatus.Internal
public final class Capes {
    private static final int NO_CAPE = -1;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final AtomicInteger THREADS = new AtomicInteger();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, task -> {
        Thread thread = new Thread(task, "Deployer capes " + THREADS.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .executor(EXECUTOR)
            .build();

    private static final Map<UUID, Integer> CAPE_IDS = new ConcurrentHashMap<>();
    private static final Map<Integer, Optional<ResourceLocation>> TEXTURES = new ConcurrentHashMap<>();

    private Capes() {}

    @Nullable
    public static ResourceLocation getTexture(AbstractClientPlayer player) {
        if(!DeployerConfig.Client.CAPES.getAsBoolean()) return null;
        int id = CAPE_IDS.computeIfAbsent(player.getUUID(), Capes::requestCapeId);
        if(id < 0) return null;
        Optional<ResourceLocation> texture = TEXTURES.get(id);
        if(texture == null) {
            if(TEXTURES.putIfAbsent(id, Optional.empty()) == null) downloadTexture(id);
            return null;
        }
        return texture.orElse(null);
    }

    public static void clearTextures() {
        TEXTURES.values().forEach(texture -> texture.ifPresent(Minecraft.getInstance().getTextureManager()::release));
        TEXTURES.clear();
    }

    private static int requestCapeId(UUID uuid) {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.liukrast.net/cape?uuid=" + uuid))
                .timeout(TIMEOUT)
                .GET()
                .build();
        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if(response.statusCode() != 200) return;
                    var json = JsonParser.parseString(response.body()).getAsJsonObject();
                    if(json.has("cape_id")) CAPE_IDS.put(uuid, json.get("cape_id").getAsInt());
                })
                .exceptionally(error -> {
                    Deployer.CONSTANTS.getLogger().debug("Could not fetch the cape of {}", uuid, error);
                    return null;
                });
        return NO_CAPE;
    }

    private static void downloadTexture(int id) {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://liukrast.net/assets/liukrast/textures/capes/" + id + ".png"))
                .timeout(TIMEOUT)
                .GET()
                .build();
        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenAccept(response -> {
                    if(response.statusCode() != 200) return;
                    try {
                        NativeImage image = NativeImage.read(response.body());
                        Minecraft.getInstance().execute(() -> TEXTURES.put(id,
                                Optional.of(Minecraft.getInstance().getTextureManager().register("deployer_capes/", new DynamicTexture(image)))));
                    } catch (Exception error) {
                        Deployer.CONSTANTS.getLogger().debug("Invalid cape texture {}", id, error);
                    }
                })
                .exceptionally(error -> {
                    Deployer.CONSTANTS.getLogger().debug("Could not download cape texture {}", id, error);
                    return null;
                });
    }
}
