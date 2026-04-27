package com.railmacros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CrossbowSwap: On pressing Mouse Button 5, swap to a loaded crossbow in hotbar.
 * Toggleable from the HUD menu only.
 */
public class CrossbowSwap {

    private static final Logger LOGGER = LoggerFactory.getLogger("RailMacros");

    private boolean enabled = false;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { enabled = v; }

    public void toggle() {
        enabled = !enabled;
    }

    /**
     * Called when Mouse Button 5 is pressed. Swaps to a loaded crossbow if one exists in hotbar.
     */
    public void trigger(MinecraftClient client) {
        if (!enabled) return;

        ClientPlayerEntity player = client.player;
        if (player == null) return;

        int loadedSlot = findLoadedCrossbowSlot(player);
        if (loadedSlot != -1) {
            player.getInventory().setSelectedSlot(loadedSlot);
            LOGGER.info("[CrossbowSwap] Swapped to loaded crossbow in slot {}", loadedSlot);
        } else {
            LOGGER.info("[CrossbowSwap] No loaded crossbow found in hotbar");
        }
    }

    /**
     * Check if there is a loaded crossbow anywhere in the hotbar.
     */
    public boolean hasLoadedCrossbow(ClientPlayerEntity player) {
        return findLoadedCrossbowSlot(player) != -1;
    }

    /**
     * Find a loaded crossbow in the hotbar (one with charged projectiles).
     * Returns the slot index, or -1 if not found.
     */
    private int findLoadedCrossbowSlot(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == Items.CROSSBOW) {
                ChargedProjectilesComponent charged = stack.get(DataComponentTypes.CHARGED_PROJECTILES);
                if (charged != null && !charged.isEmpty()) {
                    return i;
                }
            }
        }
        return -1;
    }
}
