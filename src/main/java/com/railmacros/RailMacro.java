package com.railmacros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Xbow Macro: Toggle with "]"
 * - When a rail (rail, powered_rail, detector_rail, activator_rail) is consumed,
 *   swap to tnt_minecart with a random delay of 4-17ms.
 * - When a tnt_minecart is consumed, swap to flint_and_steel with a random delay of 4-14ms.
 *   (Unless suppressed by the bow macro's 350ms window.)
 */
public class RailMacro {

    private boolean enabled = false;

    // Track previous item counts for consumption detection
    private final Map<Item, Integer> previousCounts = new HashMap<>();

    // Pending swap state (frame-based for precise timing)
    private Item pendingSwapItem = null;
    private int pendingSwapFramesRemaining = -1;

    // Crossbow swap feature: after flint & steel swap completes, swap to loaded crossbow
    private boolean crossbowSwapEnabled = false;
    private int flintToCrossbowMinDelay = 0;
    private int flintToCrossbowMaxDelay = 2;
    // When true, the next flint swap completion will chain into a crossbow swap
    private boolean pendingCrossbowSwap = false;

    private static final Item[] RAIL_ITEMS = {
            Items.RAIL,
            Items.POWERED_RAIL,
            Items.DETECTOR_RAIL,
            Items.ACTIVATOR_RAIL
    };

    // ---- Configurable settings ----

    // Rail -> TNT Minecart swap delay range (frames)
    private int railToTntMinDelay = 0;
    private int railToTntMaxDelay = 1;

    // TNT Minecart -> Flint & Steel swap delay range (frames)
    private int tntToFlintMinDelay = 0;
    private int tntToFlintMaxDelay = 1;

    // Bow suppression: if a bow was shot within this many ms, skip rail->TNT swap
    private int bowSuppressionMs = 350;

    // ---- Crossbow swap getters and setters ----

    public boolean isCrossbowSwapEnabled() { return crossbowSwapEnabled; }
    public void setCrossbowSwapEnabled(boolean v) { crossbowSwapEnabled = v; }

    public int getFlintToCrossbowMinDelay() { return flintToCrossbowMinDelay; }
    public void setFlintToCrossbowMinDelay(int v) { flintToCrossbowMinDelay = Math.max(0, Math.min(v, flintToCrossbowMaxDelay)); }

    public int getFlintToCrossbowMaxDelay() { return flintToCrossbowMaxDelay; }
    public void setFlintToCrossbowMaxDelay(int v) { flintToCrossbowMaxDelay = Math.max(flintToCrossbowMinDelay, Math.min(10, v)); }

    // ---- Getters and setters ----

    public int getRailToTntMinDelay() { return railToTntMinDelay; }
    public void setRailToTntMinDelay(int v) { railToTntMinDelay = Math.max(0, Math.min(v, railToTntMaxDelay)); }

    public int getRailToTntMaxDelay() { return railToTntMaxDelay; }
    public void setRailToTntMaxDelay(int v) { railToTntMaxDelay = Math.max(railToTntMinDelay, Math.min(10, v)); }

    public int getTntToFlintMinDelay() { return tntToFlintMinDelay; }
    public void setTntToFlintMinDelay(int v) { tntToFlintMinDelay = Math.max(0, Math.min(v, tntToFlintMaxDelay)); }

    public int getTntToFlintMaxDelay() { return tntToFlintMaxDelay; }
    public void setTntToFlintMaxDelay(int v) { tntToFlintMaxDelay = Math.max(tntToFlintMinDelay, Math.min(10, v)); }

    public int getBowSuppressionMs() { return bowSuppressionMs; }
    public void setBowSuppressionMs(int v) { bowSuppressionMs = Math.max(0, Math.min(2000, v)); }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
        previousCounts.clear();
        pendingSwapItem = null;
        pendingSwapFramesRemaining = -1;
        pendingCrossbowSwap = false;
    }

    /**
     * Reset tracked item counts to current values without triggering any swaps.
     * Called when inventory screen closes to avoid false consumption detection.
     */
    public void resetCounts(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        for (Item railItem : RAIL_ITEMS) {
            previousCounts.put(railItem, MacroUtils.countInInventory(player, railItem));
        }
        previousCounts.put(Items.TNT_MINECART, MacroUtils.countInInventory(player, Items.TNT_MINECART));
    }

    /**
     * Called every render frame to process pending swaps with frame-level precision.
     * At 60 FPS each frame is ~16ms, at 120 FPS each frame is ~8ms.
     */
    public void onFrame(MinecraftClient client) {
        if (!enabled) return;
        if (pendingSwapItem == null) return;

        ClientPlayerEntity player = client.player;
        if (player == null) return;

        if (pendingSwapFramesRemaining <= 0) {
            // Check if this is a flint swap that should chain to crossbow
            boolean wasFlintSwap = pendingCrossbowSwap && pendingSwapItem == Items.FLINT_AND_STEEL;

            // Swap now — use exact slot for crossbow, findHotbarSlot for everything else
            if (pendingSwapItem == Items.CROSSBOW && pendingCrossbowSlot >= 0) {
                player.getInventory().setSelectedSlot(pendingCrossbowSlot);
                pendingCrossbowSlot = -1;
            } else {
                int slot = MacroUtils.findHotbarSlot(player, pendingSwapItem);
                if (slot != -1) {
                    player.getInventory().setSelectedSlot(slot);
                }
            }
            pendingSwapItem = null;
            pendingSwapFramesRemaining = -1;

            // Chain: after flint swap completes, queue crossbow swap
            if (wasFlintSwap) {
                pendingCrossbowSwap = false;
                queueCrossbowSwap(player);
            }
        } else {
            pendingSwapFramesRemaining--;
        }
    }

    public void tick(MinecraftClient client) {
        if (!enabled) return;

        ClientPlayerEntity player = client.player;
        if (player == null) return;

        // Check for rail consumption
        // Suppress rail->TNT swap if a bow was shot recently (within bowSuppressionMs)
        boolean bowSuppressed = RailMacrosMod.BOW_MACRO.isBowShotWithin(bowSuppressionMs);
        for (Item railItem : RAIL_ITEMS) {
            int currentCount = MacroUtils.countInInventory(player, railItem);
            Integer prevCount = previousCounts.get(railItem);
            if (prevCount != null && currentCount < prevCount && !bowSuppressed) {
                // Rail was consumed -> queue swap to tnt_minecart (instant or tick-delayed)
                queueSwap(Items.TNT_MINECART, railToTntMinDelay, railToTntMaxDelay);
            }
            previousCounts.put(railItem, currentCount);
        }

        // Check for tnt_minecart consumption
        int tntMinecartCount = MacroUtils.countInInventory(player, Items.TNT_MINECART);
        Integer prevTntMinecart = previousCounts.get(Items.TNT_MINECART);
        if (prevTntMinecart != null && tntMinecartCount < prevTntMinecart) {
            // TNT minecart was consumed
            // Check if bow macro suppression is active
            if (!RailMacrosMod.BOW_MACRO.isBowShotRecent()) {
                    // Queue swap to flint_and_steel
                    queueSwap(Items.FLINT_AND_STEEL, tntToFlintMinDelay, tntToFlintMaxDelay);
                    if (crossbowSwapEnabled) {
                        pendingCrossbowSwap = true;
                    }
            }
        }
        previousCounts.put(Items.TNT_MINECART, tntMinecartCount);
    }

    /**
     * Queue a swap to be executed on the next frame(s).
     * Delay is in render frames — at higher FPS the swap happens sooner in real time.
     * 0 frames = next frame, 1 frame = one frame later, etc.
     */
    private void queueSwap(Item item, int minDelayFrames, int maxDelayFrames) {
        int delayFrames = java.util.concurrent.ThreadLocalRandom.current().nextInt(minDelayFrames, maxDelayFrames + 1);
        pendingSwapItem = item;
        pendingSwapFramesRemaining = delayFrames;
    }

    // Pending crossbow slot swap (by exact slot, not item type)
    private int pendingCrossbowSlot = -1;

    /**
     * Find a loaded crossbow in the hotbar and queue a swap to its exact slot.
     */
    private void queueCrossbowSwap(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == Items.CROSSBOW) {
                ChargedProjectilesComponent charged = stack.get(DataComponentTypes.CHARGED_PROJECTILES);
                if (charged != null && !charged.isEmpty()) {
                    // Found a loaded crossbow — queue swap to its exact slot
                    int delayFrames = java.util.concurrent.ThreadLocalRandom.current()
                            .nextInt(flintToCrossbowMinDelay, flintToCrossbowMaxDelay + 1);
                    pendingCrossbowSlot = i;
                    pendingSwapItem = Items.CROSSBOW; // marker for frame processing
                    pendingSwapFramesRemaining = delayFrames;
                    return;
                }
            }
        }
    }
}
