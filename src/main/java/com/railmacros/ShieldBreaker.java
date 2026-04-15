package com.railmacros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ShieldBreaker Module: When you attack a player who is actively blocking with a shield,
 * automatically swaps to an axe, attacks to disable the shield, then swaps back.
 * Only triggers when the target is actively holding up their shield (isBlocking).
 */
public class ShieldBreaker {

    private boolean enabled = false;

    private static final Random RANDOM = new Random();

    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ShieldBreaker-Scheduler");
                t.setDaemon(true);
                return t;
            });

    // Configurable settings
    private int minDelayMs = 0;
    private int maxDelayMs = 100;
    private double missChance = 0.0;

    // Internal state — prevent re-triggering while a break sequence is in progress
    private boolean breakPending = false;

    // Cooldown to prevent rapid re-triggers (ticks)
    private int cooldownTicks = 0;
    private static final int COOLDOWN_DURATION = 10; // 10 ticks = 500ms

    // All axe items (best to worst for shield disabling — all axes disable shields equally)
    private static final Set<Item> AXE_ITEMS = Set.of(
            Items.NETHERITE_AXE, Items.DIAMOND_AXE, Items.IRON_AXE,
            Items.GOLDEN_AXE, Items.STONE_AXE, Items.COPPER_AXE,
            Items.WOODEN_AXE
    );

    // Ordered axe preference (best first)
    private static final Item[] AXE_PRIORITY = {
            Items.NETHERITE_AXE, Items.DIAMOND_AXE, Items.IRON_AXE,
            Items.GOLDEN_AXE, Items.STONE_AXE, Items.COPPER_AXE,
            Items.WOODEN_AXE
    };

    // ---- Getters and setters ----

    public boolean isEnabled() { return enabled; }

    public void toggle() {
        enabled = !enabled;
        breakPending = false;
        cooldownTicks = 0;
    }

    public int getMinDelayMs() { return minDelayMs; }
    public void setMinDelayMs(int v) { minDelayMs = Math.max(0, Math.min(v, maxDelayMs)); }

    public int getMaxDelayMs() { return maxDelayMs; }
    public void setMaxDelayMs(int v) { maxDelayMs = Math.max(minDelayMs, Math.min(500, v)); }

    public double getMissChance() { return missChance; }
    public void setMissChance(double v) { missChance = Math.max(0, Math.min(1, v)); }

    /**
     * Called every tick to manage cooldown and detect shield-blocking targets.
     * The module triggers when the player attacks (left clicks) a player who is blocking.
     */
    public void tick(MinecraftClient client) {
        if (!enabled) return;

        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;

        // Count down cooldown
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        // Don't trigger if a break sequence is already in progress
        if (breakPending) return;

        // Check if left click (attack) is being pressed this tick
        // We detect the attack key being pressed as the trigger
        if (!client.options.attackKey.isPressed()) return;

        // Check if the targeted entity is a player who is actively blocking with a shield
        Entity target = client.targetedEntity;
        if (!(target instanceof PlayerEntity targetPlayer)) return;
        if (!targetPlayer.isAlive()) return;
        if (!targetPlayer.isBlocking()) return;

        // Target is blocking with a shield — check if we already have an axe equipped
        Item heldItem = player.getMainHandStack().getItem();
        if (AXE_ITEMS.contains(heldItem)) {
            // Already holding an axe, no need to swap — the vanilla attack will disable the shield
            return;
        }

        // Check if we have an axe in the hotbar
        int axeSlot = findBestAxeSlot(player);
        if (axeSlot == -1) return; // No axe available

        // Miss chance
        if (RANDOM.nextDouble() < missChance) return;

        // Start the shield break sequence
        int previousSlot = player.getInventory().getSelectedSlot();
        int delayRange = maxDelayMs - minDelayMs;
        int delay = minDelayMs + (delayRange > 0 ? RANDOM.nextInt(delayRange + 1) : 0);

        breakPending = true;
        scheduleShieldBreak(client, axeSlot, previousSlot, delay);
    }

    /**
     * Find the best axe slot in the hotbar (highest tier first).
     */
    private int findBestAxeSlot(ClientPlayerEntity player) {
        for (Item axe : AXE_PRIORITY) {
            int slot = MacroUtils.findHotbarSlot(player, axe);
            if (slot != -1) return slot;
        }
        return -1;
    }

    /**
     * Execute the shield break sequence:
     * 1. Swap to axe
     * 2. Attack (disables shield)
     * 3. Swap back to previous item
     */
    private void scheduleShieldBreak(MinecraftClient client, int axeSlot, int previousSlot, int delayMs) {
        SCHEDULER.schedule(() -> {
            client.execute(() -> {
                if (!enabled) {
                    breakPending = false;
                    return;
                }

                ClientPlayerEntity player = client.player;
                if (player == null || client.interactionManager == null) {
                    breakPending = false;
                    return;
                }

                // Re-check target is still a blocking player
                Entity target = client.targetedEntity;
                if (!(target instanceof PlayerEntity targetPlayer) || !targetPlayer.isAlive()) {
                    breakPending = false;
                    return;
                }

                // Step 1: Swap to axe
                player.getInventory().setSelectedSlot(axeSlot);

                // Step 2: Attack with the axe (this will disable the shield)
                client.interactionManager.attackEntity(player, target);
                player.swingHand(Hand.MAIN_HAND);

                // Step 3: Swap back to previous item after a short delay
                SCHEDULER.schedule(() -> {
                    client.execute(() -> {
                        if (client.player != null) {
                            client.player.getInventory().setSelectedSlot(previousSlot);
                        }
                        breakPending = false;
                        cooldownTicks = COOLDOWN_DURATION;
                    });
                }, 50, TimeUnit.MILLISECONDS);
            });
        }, delayMs, TimeUnit.MILLISECONDS);
    }
}
