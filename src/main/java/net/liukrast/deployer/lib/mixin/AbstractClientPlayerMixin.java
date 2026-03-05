package net.liukrast.deployer.lib.mixin;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.liukrast.deployer.lib.mixinExtensions.ACPExtension;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin extends Player implements ACPExtension {
    @Unique
    private static final HttpClient deployer$CLIENT = HttpClient.newHttpClient();

    @Unique
    private int deployer$cape = -1;

    public AbstractClientPlayerMixin(Level level, BlockPos pos, float yRot, GameProfile gameProfile) {
        super(level, pos, yRot, gameProfile);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(ClientLevel clientLevel, GameProfile gameProfile, CallbackInfo ci) {
        UUID uuid = gameProfile.getId();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.liukrast.net/cape?uuid=" + uuid))
                .GET()
                .build();

        deployer$CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(JsonParser::parseString)
                .thenAccept(jsonElement -> {
                    var json = jsonElement.getAsJsonObject();

                    if (json.has("cape_id")) {
                        deployer$cape = json.get("cape_id").getAsInt();
                    }
                })
                .exceptionally(ex -> null);


    }

    @Override
    public int deployer$getCape() {
        return deployer$cape;
    }
}
