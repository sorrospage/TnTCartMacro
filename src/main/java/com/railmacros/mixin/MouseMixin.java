package com.railmacros.mixin;

import com.railmacros.CrossbowSwap;
import com.railmacros.RailMacrosMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_5;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

/**
 * Intercepts mouse button events at the source.
 * When CrossbowSwap is enabled and a loaded crossbow is in the hotbar,
 * Mouse Button 5 is consumed here so no other keybind (vanilla or mod) sees it.
 */
@Mixin(Mouse.class)
public class MouseMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (button == GLFW_MOUSE_BUTTON_5 && action == GLFW_PRESS) {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player != null && client.currentScreen == null) {
                CrossbowSwap swap = RailMacrosMod.CROSSBOW_SWAP;
                if (swap.isEnabled() && swap.hasLoadedCrossbow(player)) {
                    swap.trigger(client);
                    ci.cancel();
                }
            }
        }
    }
}
