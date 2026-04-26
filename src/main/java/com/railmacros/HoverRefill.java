package com.railmacros;

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
     * Get the last swapped slot id (for dedup in the mixin).
     */
    public int getLastSwappedSlotId() { return lastSwappedSlotId; }

    /**
     * Set the last swapped slot id after a swap is performed.
     */
    public void setLastSwappedSlotId(int id) { lastSwappedSlotId = id; }

    /**
     * Reset tracking when the screen closes or cursor moves off a tnt_minecart.
     */
    public void resetTracking() { lastSwappedSlotId = -1; }
}
