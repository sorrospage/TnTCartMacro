package com.railmacros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * SafeAnchor Module: Upon placing a respawn anchor, swaps to glowstone,
 * places 2 glowstone to charge it, swaps to totem of undying,
 * then right-clicks the anchor to explode it.
 *
 * State machine: IDLE → ANCHOR_PLACED → SWAP_TO_GLOWSTONE → GLOWSTONE_1 → GLOWSTONE_2
 *                → SWAP_TO_TOTEM → INTERACT_ANCHOR
 *
 * Uses frame-based delays for precise timing (like RailMacro).
 */
public class SafeAnchor {

    private boolean enabled = false;

    // Configurable delays (frame-based)
    private int minDelay = 0;
    private int maxDelay = 2;

    // State machine
    private enum State {
        IDLE,
        ANCHOR_PLACED,        // Anchor was just placed, waiting to swap to glowstone
        GLOWSTONE_SWAP_PENDING, // Waiting for glowstone swap delay
        GLOWSTONE_1_PENDING,  // Waiting for first glowstone to be placed (right-click)
        GLOWSTONE_1_PLACED,   // First glowstone placed, waiting for second
        GLOWSTONE_2_PENDING,  // Waiting for second glowstone to be placed (right-click)
        GLOWSTONE_2_PLACED,   // Second glowstone placed, swap to totem
        TOTEM_SWAP_PENDING,   // Waiting for totem swap delay
        TOTEM_READY,          // Totem in hand, interact with anchor
        INTERACT_PENDING      // Waiting for interact delay
    }

    private State state = State.IDLE;
    private int framesRemaining = -1;

    // Track anchor item count to detect placement
    private int previousAnchorCount = -1;
    // Track glowstone count to detect placement
    private int previousGlowstoneCount = -1;

    // ---- Getters and setters ----

    public boolean isEnabled() { return enabled; }

    public void toggle() {
        enabled = !enabled;
        reset();
    }

    public int getMinDelay() { return minDelay; }
    public void setMinDelay(int v) { minDelay = Math.max(0, Math.min(v, maxDelay)); }

    public int getMaxDelay() { return maxDelay; }
    public void setMaxDelay(int v) { maxDelay = Math.max(minDelay, Math.min(10, v)); }

    private void reset() {
        state = State.IDLE;
        framesRemaining = -1;
        previousAnchorCount = -1;
        previousGlowstoneCount = -1;
    }

    /**
     * Reset tracked counts without triggering swaps.
     * Called when inventory screen closes.
     */
    public void resetCounts(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        previousAnchorCount = MacroUtils.countInInventory(player, Items.RESPAWN_ANCHOR);
        previousGlowstoneCount = MacroUtils.countInInventory(player, Items.GLOWSTONE);
        state = State.IDLE;
        framesRemaining = -1;
    }

    /**
     * Called every render frame for frame-based delay processing.
     */
    public void onFrame(MinecraftClient client) {
        if (!enabled) return;
        if (framesRemaining < 0) return;

        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;

        if (framesRemaining > 0) {
            framesRemaining--;
            return;
        }

        // framesRemaining == 0: execute the pending action
        switch (state) {
            case GLOWSTONE_SWAP_PENDING -> {
                // Swap to glowstone
                int slot = MacroUtils.findHotbarSlot(player, Items.GLOWSTONE);
                if (slot != -1) {
                    player.getInventory().setSelectedSlot(slot);
                    previousGlowstoneCount = MacroUtils.countInInventory(player, Items.GLOWSTONE);
                    state = State.GLOWSTONE_1_PENDING;
                    framesRemaining = randomDelay();
                } else {
                    reset(); // No glowstone available
                }
            }
            case GLOWSTONE_1_PENDING -> {
                // Right-click to place first glowstone
                if (client.crosshairTarget instanceof BlockHitResult blockHit
                        && blockHit.getType() == HitResult.Type.BLOCK) {
                    client.interactionManager.interactBlock(player, Hand.MAIN_HAND, blockHit);
                    player.swingHand(Hand.MAIN_HAND);
                    state = State.GLOWSTONE_1_PLACED;
                    framesRemaining = randomDelay();
                } else {
                    framesRemaining = 1; // Retry next frame
                }
            }
            case GLOWSTONE_1_PLACED -> {
                // Right-click to place second glowstone
                if (client.crosshairTarget instanceof BlockHitResult blockHit
                        && blockHit.getType() == HitResult.Type.BLOCK) {
                    client.interactionManager.interactBlock(player, Hand.MAIN_HAND, blockHit);
                    player.swingHand(Hand.MAIN_HAND);
                    state = State.TOTEM_SWAP_PENDING;
                    framesRemaining = randomDelay();
                } else {
                    framesRemaining = 1; // Retry next frame
                }
            }
            case TOTEM_SWAP_PENDING -> {
                // Swap to totem of undying
                int slot = MacroUtils.findHotbarSlot(player, Items.TOTEM_OF_UNDYING);
                if (slot != -1) {
                    player.getInventory().setSelectedSlot(slot);
                    state = State.INTERACT_PENDING;
                    framesRemaining = randomDelay();
                } else {
                    reset(); // No totem available
                }
            }
            case INTERACT_PENDING -> {
                // Right-click the anchor to explode it
                if (client.crosshairTarget instanceof BlockHitResult blockHit
                        && blockHit.getType() == HitResult.Type.BLOCK) {
                    client.interactionManager.interactBlock(player, Hand.MAIN_HAND, blockHit);
                    player.swingHand(Hand.MAIN_HAND);
                }
                reset(); // Done — back to idle
            }
            default -> {
                framesRemaining = -1;
            }
        }
    }

    /**
     * Called every tick for item consumption detection.
     */
    public void tick(MinecraftClient client) {
        if (!enabled) return;

        ClientPlayerEntity player = client.player;
        if (player == null) return;

        // Only detect anchor placement when idle
        if (state == State.IDLE) {
            int anchorCount = MacroUtils.countInInventory(player, Items.RESPAWN_ANCHOR);

            if (previousAnchorCount == -1) {
                previousAnchorCount = anchorCount;
                return;
            }

            if (anchorCount < previousAnchorCount) {
                // Anchor was placed — start the chain
                state = State.GLOWSTONE_SWAP_PENDING;
                framesRemaining = randomDelay();
            }

            previousAnchorCount = anchorCount;
        }
    }

    private int randomDelay() {
        if (minDelay >= maxDelay) return minDelay;
        return java.util.concurrent.ThreadLocalRandom.current().nextInt(minDelay, maxDelay + 1);
    }
}
