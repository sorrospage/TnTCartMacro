package com.railmacros;

import com.railmacros.mixin.HandledScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HoverRefill: When the mouse hovers over a tnt_minecart in the inventory,
 * swap it to hotbar slot 3. Toggleable from the HUD menu only.
 */
public class HoverRefill {

    private static final Logger LOGGER = LoggerFactory.getLogger("RailMacros");

    private boolean enabled = false;

    /** The slot id we last triggered a swap on, to avoid repeated swaps on the same slot. */
    private int lastSwappedSlotId = -1;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { enabled = v; }

    public void toggle() {
        enabled = !enabled;
    }

    /**
     * Called every client tick. Checks if the current screen is a HandledScreen
     * and the focused slot has a tnt_minecart, then swaps it to hotbar slot 3.
     */
    public void tick(MinecraftClient client) {
        if (!enabled) return;
        if (client.player == null || client.interactionManager == null) return;

        // Only works when a handled screen (inventory, chest, etc.) is open
        if (!(client.currentScreen instanceof HandledScreen<?> handledScreen)) {
            lastSwappedSlotId = -1;
            return;
        }

        // Get the focused slot via accessor mixin
        Slot focusedSlot = ((HandledScreenAccessor) handledScreen).getFocusedSlot();

        // If not hovering over a slot or the slot is empty, reset tracking
        if (focusedSlot == null || !focusedSlot.hasStack()) {
            lastSwappedSlotId = -1;
            return;
        }

        // Only trigger for tnt_minecart items
        if (focusedSlot.getStack().getItem() != Items.TNT_MINECART) {
            lastSwappedSlotId = -1;
            return;
        }

        // Don't re-trigger on the same slot
        if (focusedSlot.id == lastSwappedSlotId) {
            return;
        }

        // Perform swap: SlotActionType.SWAP with button=2 means swap with hotbar slot 3 (0-indexed)
        int syncId = handledScreen.getScreenHandler().syncId;
        client.interactionManager.clickSlot(syncId, focusedSlot.id, 2, SlotActionType.SWAP, client.player);
        lastSwappedSlotId = focusedSlot.id;

        LOGGER.info("[HoverRefill] Swapped tnt_minecart from slot {} to hotbar slot 3", focusedSlot.id);
    }
}
