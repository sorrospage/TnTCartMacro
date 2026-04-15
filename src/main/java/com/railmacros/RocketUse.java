package com.railmacros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/**
 * RocketUse Module: When the player presses Mouse Button 5 (forward side button),
 * if firework rockets are in the hotbar, swap to them, use once, then swap back.
 *
 * Uses frame-based delays for timing between steps.
 * Toggled from menu only.
 */
public class RocketUse {

    private boolean enabled = false;

    // Configurable delays (frame-based)
    private int minDelay = 0;
    private int maxDelay = 2;

    // State machine
    private enum State {
        IDLE,
        SWAP_TO_ROCKET,    // Waiting to swap hotbar selection to rockets
        USE_ROCKET,        // Waiting to use the rocket
        SWAP_BACK          // Waiting to swap back to previous slot
    }

    private State state = State.IDLE;
    private int framesRemaining = -1;
    private int previousSlot = -1;

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
        previousSlot = -1;
    }

    /**
     * Called when the trigger key (Mouse Button 5) is pressed.
     * Initiates the rocket use sequence if conditions are met.
     */
    public void trigger(MinecraftClient client) {
        if (!enabled) return;
        if (state != State.IDLE) return;

        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;

        int slot = MacroUtils.findHotbarSlot(player, Items.FIREWORK_ROCKET);
        if (slot == -1) return; // No rockets in hotbar

        previousSlot = player.getInventory().getSelectedSlot();
        state = State.SWAP_TO_ROCKET;
        framesRemaining = randomDelay();
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
            case SWAP_TO_ROCKET -> {
                // Step 1: Select the rocket slot in hotbar
                int slot = MacroUtils.findHotbarSlot(player, Items.FIREWORK_ROCKET);
                if (slot != -1) {
                    player.getInventory().setSelectedSlot(slot);
                    state = State.USE_ROCKET;
                    framesRemaining = randomDelay();
                } else {
                    reset(); // No rockets available
                }
            }
            case USE_ROCKET -> {
                // Step 2: Use the rocket (right-click)
                client.interactionManager.interactItem(player, Hand.MAIN_HAND);
                player.swingHand(Hand.MAIN_HAND);
                state = State.SWAP_BACK;
                framesRemaining = randomDelay();
            }
            case SWAP_BACK -> {
                // Step 3: Swap back to previous hotbar slot
                player.getInventory().setSelectedSlot(previousSlot);
                reset();
            }
            default -> {
                framesRemaining = -1;
            }
        }
    }

    private int randomDelay() {
        if (minDelay >= maxDelay) return minDelay;
        return java.util.concurrent.ThreadLocalRandom.current().nextInt(minDelay, maxDelay + 1);
    }
}
