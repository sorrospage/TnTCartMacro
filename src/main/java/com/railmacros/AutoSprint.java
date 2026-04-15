package com.railmacros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * AutoSprint Module: Automatically sets the player to sprinting whenever they are moving forward.
 * Toggle only from the in-game menu (no keybind).
 */
public class AutoSprint {

    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
    }

    public void tick(MinecraftClient client) {
        if (!enabled) return;

        ClientPlayerEntity player = client.player;
        if (player == null) return;

        // Only sprint if the player is moving forward and not using an item
        boolean movingForward = player.input.playerInput.forward();
        boolean usingItem = player.isUsingItem();

        if (movingForward && !usingItem && !player.isSprinting()) {
            player.setSprinting(true);
        }
    }
}
