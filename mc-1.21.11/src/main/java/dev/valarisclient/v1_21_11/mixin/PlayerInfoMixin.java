package dev.valarisclient.v1_21_11.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.valarisclient.v1_21_11.render.CustomSkinTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.mojang.authlib.GameProfile;

import java.util.UUID;

@Mixin(PlayerInfo.class)
public abstract class PlayerInfoMixin {

    @Shadow
    public abstract GameProfile getProfile();

    @ModifyReturnValue(method = "getSkin", at = @At("RETURN"))
    private PlayerSkin ValarisClient$customSkin(PlayerSkin original) {
        GameProfile profile = getProfile();
        if (profile == null) {
            return original;
        }
        UUID uuid = profile.id();
        Minecraft client = Minecraft.getInstance();
        boolean local = client.player != null && uuid.equals(client.player.getUUID());
        return CustomSkinTextures.maybeOverride(uuid, local, original);
    }
}
