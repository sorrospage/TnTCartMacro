package com.railmacros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/**
 * ElytraSwap Module: When the player presses "4", toggles elytra equip/unequip.
 *
 * If elytra is in the hotbar: swap to it, equip it (swap with chestplate slot), swap back.
 * If elytra is already worn in chest armor slot: unequip it back to the hotbar, swap back.
 *
 * Uses frame-based delays for timing between steps.
 * Toggled from menu only (keybind "4" is the trigger, not the toggle).
 */
public class ElytraSwap {

    private boolean enabled = false;

    // Configurable delays (frame-based)
    private int minDelay = 0;
    private int maxDelay = 2;

    // State machine
    private enum State {
        IDLE,
        SWAP_TO_ELYTRA,    // Waiting to swap hotbar selection to elytra (equip path)
        EQUIP_ELYTRA,      // Waiting to equip elytra into chest slot
        SWAP_BACK,          // Waiting to swap back to previous slot
        UNEQUIP_ELYTRA      // Waiting to unequip elytra from chest slot to hotbar
    }

    private State state = State.IDLE;
    private int framesRemaining = -1;
    private int previousSlot = -1;
    private int elytraSlot = -1;

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
        elytraSlot = -1;
    }

    /**
     * Called when the trigger key ("4") is pressed.
     * If elytra is in hotbar, equip it. If elytra is worn, unequip it.
     */
    public void trigger(MinecraftClient client) {
        if (!enabled) return;
        if (state != State.IDLE) return;

        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;

        previousSlot = player.getInventory().getSelectedSlot();

        // Check if elytra is already worn in chest armor slot
        if (player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
            // Elytra is worn — unequip it
            // Find an empty hotbar slot or use current slot
            elytraSlot = findEmptyHotbarSlot(player);
            if (elytraSlot == -1) {
                elytraSlot = previousSlot; // Use current slot if no empty slot
            }
            state = State.UNEQUIP_ELYTRA;
            framesRemaining = randomDelay();
            return;
        }

        // Check if elytra is in hotbar — equip it
        int slot = MacroUtils.findHotbarSlot(player, Items.ELYTRA);
        if (slot == -1) return; // No elytra anywhere relevant

        elytraSlot = slot;
        state = State.SWAP_TO_ELYTRA;
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
            case SWAP_TO_ELYTRA -> {
                // Step 1: Select the elytra slot in hotbar
                player.getInventory().setSelectedSlot(elytraSlot);
                state = State.EQUIP_ELYTRA;
                framesRemaining = randomDelay();
            }
            case EQUIP_ELYTRA -> {
                // Step 2: Swap elytra from hotbar into chestplate slot (slot 6 in player inventory)
                // In the player's inventory screen:
                //   Slot 6 = chest armor slot
                //   Slots 36-44 = hotbar slots 0-8
                int hotbarScreenSlot = 36 + elytraSlot;
                int syncId = player.currentScreenHandler.syncId;

                // Pick up the elytra from hotbar
                client.interactionManager.clickSlot(syncId, hotbarScreenSlot, 0, SlotActionType.PICKUP, player);
                // Place it in chest armor slot (slot 6)
                client.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, player);
                // If there was a chestplate, it's now on the cursor — put it back in the hotbar
                client.interactionManager.clickSlot(syncId, hotbarScreenSlot, 0, SlotActionType.PICKUP, player);

                state = State.SWAP_BACK;
                framesRemaining = randomDelay();
            }
            case UNEQUIP_ELYTRA -> {
                // Unequip: swap elytra from chest armor slot to hotbar
                int hotbarScreenSlot = 36 + elytraSlot;
                int syncId = player.currentScreenHandler.syncId;

                // Pick up from chest armor slot (slot 6)
                client.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, player);
                // Place in hotbar slot
                client.interactionManager.clickSlot(syncId, hotbarScreenSlot, 0, SlotActionType.PICKUP, player);
                // If there was an item in that hotbar slot, put it in chest slot
                client.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, player);

                state = State.SWAP_BACK;
                framesRemaining = randomDelay();
            }
            case SWAP_BACK -> {
                // Swap back to previous hotbar slot
                player.getInventory().setSelectedSlot(previousSlot);
                reset();
            }
            default -> {
                framesRemaining = -1;
            }
        }
    }

    private int findEmptyHotbarSlot(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private int randomDelay() {
        if (minDelay >= maxDelay) return minDelay;
        return java.util.concurrent.ThreadLocalRandom.current().nextInt(minDelay, maxDelay + 1);
    }
}
