package com.railmacros;

import com.railmacros.mixin.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spear Macro: Toggle from the in-game menu only (no keybind).
 * When Middle Mouse Button is pressed:
 *   1. Remember the current hotbar slot.
 *   2. After a configurable pre-swap delay, swap to a spear in the hotbar.
 *   3. After a configurable attack delay, left-click (attack).
 *   4. After a configurable swap-back delay, swap back to the original slot.
 */
public class SpearMacro {

    private static final Logger LOGGER = LoggerFactory.getLogger("RailMacros");

    private boolean enabled = false;

    // Prevents queueing multiple spear attacks
    private boolean actionPending = false;

    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "SpearMacro-Scheduler");
                t.setDaemon(true);
                return t;
            });

    // All spear item IDs (custom/modded items looked up by registry)
    private static final String[] SPEAR_IDS = {
            "minecraft:wooden_spear",
            "minecraft:stone_spear",
            "minecraft:iron_spear",
            "minecraft:gold_spear",
            "minecraft:diamond_spear",
            "minecraft:netherite_spear"
    };

    // ---- Configurable settings ----

    // Delay before swapping to spear (ms)
    private int preSwapDelayMs = 0;

    // Delay between swapping to spear and attacking (ms)
    private int attackDelayMs = 50;

    // Delay between attacking and swapping back (ms)
    private int swapBackDelayMs = 50;

    // ---- Getters and setters ----

    public int getPreSwapDelayMs() { return preSwapDelayMs; }
    public void setPreSwapDelayMs(int v) { preSwapDelayMs = Math.max(0, Math.min(500, v)); }

    public int getAttackDelayMs() { return attackDelayMs; }
    public void setAttackDelayMs(int v) { attackDelayMs = Math.max(0, Math.min(500, v)); }

    public int getSwapBackDelayMs() { return swapBackDelayMs; }
    public void setSwapBackDelayMs(int v) { swapBackDelayMs = Math.max(0, Math.min(500, v)); }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
        actionPending = false;
    }

    /**
     * Find a spear in the hotbar (slots 0-8).
     * Returns the slot index, or -1 if no spear found.
     */
    private int findSpearSlot(ClientPlayerEntity player) {
        List<Item> spearItems = resolveSpearItems();
        for (Item spearItem : spearItems) {
            int slot = MacroUtils.findHotbarSlot(player, spearItem);
            if (slot != -1) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * Resolve spear Item instances from the registry by their identifiers.
     */
    private List<Item> resolveSpearItems() {
        List<Item> items = new ArrayList<>();
        for (String id : SPEAR_IDS) {
            Identifier identifier = Identifier.of(id);
            if (Registries.ITEM.containsId(identifier)) {
                items.add(Registries.ITEM.get(identifier));
            }
        }
        return items;
    }

    /**
     * Called when Middle Mouse Button is pressed.
     * Initiates the spear swap -> attack -> swap back sequence.
     */
    public void onMiddleClick(MinecraftClient client) {
        if (!enabled) return;
        if (actionPending) return;

        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;

        int spearSlot = findSpearSlot(player);
        if (spearSlot == -1) return; // No spear in hotbar

        int originalSlot = player.getInventory().getSelectedSlot();
        if (spearSlot == originalSlot) return; // Already holding a spear

        actionPending = true;
        LOGGER.info("[SpearMacro] Middle click detected, starting spear sequence");

        // Step 1: After pre-swap delay, swap to spear
        SCHEDULER.schedule(() -> {
            client.execute(() -> {
                if (!enabled) { actionPending = false; return; }
                ClientPlayerEntity p = client.player;
                if (p == null) { actionPending = false; return; }

                p.getInventory().setSelectedSlot(spearSlot);
                LOGGER.info("[SpearMacro] Swapped to spear (slot {})", spearSlot);

                // Step 2: After attack delay, simulate a full left click to trigger spear lunge
                SCHEDULER.schedule(() -> {
                    client.execute(() -> {
                        if (!enabled) { actionPending = false; return; }
                        ClientPlayerEntity p2 = client.player;
                        if (p2 == null) {
                            actionPending = false;
                            return;
                        }

                        // Use doAttack() to simulate a real left click - this triggers
                        // the spear's lunge mechanic properly
                        ((MinecraftClientAccessor) client).invokeDoAttack();
                        LOGGER.info("[SpearMacro] Left click (lunge) triggered");

                        // Step 3: After swap-back delay, swap back to original slot
                        SCHEDULER.schedule(() -> {
                            client.execute(() -> {
                                if (!enabled) { actionPending = false; return; }
                                ClientPlayerEntity p3 = client.player;
                                if (p3 == null) { actionPending = false; return; }

                                p3.getInventory().setSelectedSlot(originalSlot);
                                actionPending = false;
                                LOGGER.info("[SpearMacro] Swapped back to slot {}", originalSlot);
                            });
                        }, swapBackDelayMs, TimeUnit.MILLISECONDS);
                    });
                }, attackDelayMs, TimeUnit.MILLISECONDS);
            });
        }, preSwapDelayMs, TimeUnit.MILLISECONDS);
    }
}
