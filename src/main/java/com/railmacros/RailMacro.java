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

    // Crossbow swap feature: after flint & steel use, swap to loaded crossbow
    private boolean crossbowSwapEnabled = false;
    private int flintToCrossbowMinDelay = 0;
    private int flintToCrossbowMaxDelay = 2;
    // Tracks whether we're waiting for flint & steel to be used after swapping to it
    private boolean waitingForFlintUse = false;
    // Track flint & steel durability to detect usage
    private int previousFlintDurability = -1;

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
        waitingForFlintUse = false;
        previousFlintDurability = -1;
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
            // Swap now
            int slot = MacroUtils.findHotbarSlot(player, pendingSwapItem);
            if (slot != -1) {
                player.getInventory().setSelectedSlot(slot);
            }
            pendingSwapItem = null;
            pendingSwapFramesRemaining = -1;
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
                        waitingForFlintUse = true;
                        previousFlintDurability = -1; // Will be set on next tick
                    }
            }
        }
        previousCounts.put(Items.TNT_MINECART, tntMinecartCount);

        // Crossbow swap: detect flint & steel usage by durability change
        if (crossbowSwapEnabled && waitingForFlintUse) {
            ItemStack heldStack = player.getMainHandStack();
            if (heldStack.getItem() == Items.FLINT_AND_STEEL) {
                int currentDurability = heldStack.getDamage();
                if (previousFlintDurability == -1) {
                    // First tick after swap — record initial durability
                    previousFlintDurability = currentDurability;
                } else if (currentDurability > previousFlintDurability) {
                    // Durability increased (damage went up) — flint was used
                    waitingForFlintUse = false;
                    previousFlintDurability = -1;
                    queueCrossbowSwap(player);
                }
            } else if (heldStack.getItem() != Items.FLINT_AND_STEEL && pendingSwapItem == null) {
                // Player manually swapped away from flint — cancel waiting
                waitingForFlintUse = false;
                previousFlintDurability = -1;
            }
        }
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

    /**
     * Find and queue a swap to a loaded crossbow in the hotbar.
     */
    private void queueCrossbowSwap(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == Items.CROSSBOW) {
                ChargedProjectilesComponent charged = stack.get(DataComponentTypes.CHARGED_PROJECTILES);
                if (charged != null && !charged.isEmpty()) {
                    // Found a loaded crossbow — queue swap to it
                    queueSwap(Items.CROSSBOW, flintToCrossbowMinDelay, flintToCrossbowMaxDelay);
                    return;
                }
            }
        }
    }
}
