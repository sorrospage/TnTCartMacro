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
 * StunSlam Module: On hitting a player holding a shield, if there is an axe and mace in hotbar:
 * 1. Swap to axe
 * 2. Break shield (attack)
 * 3. Swap to mace (after configurable delay)
 * 4. Mace the player (attack)
 * 5. Swap back to previous slot
 *
 * Configurable delay between axe break and mace swap in ms.
 * Toggled from menu only.
 */
public class StunSlam {

    private boolean enabled = false;

    private static final Random RANDOM = new Random();

    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "StunSlam-Scheduler");
                t.setDaemon(true);
                return t;
            });

    // Configurable delays (ms)
    private int minDelayMs = 0;
    private int maxDelayMs = 100;

    // Internal state
    private boolean slamPending = false;
    private int cooldownTicks = 0;
    private static final int COOLDOWN_DURATION = 10;

    // Axe items ordered by priority
    private static final Item[] AXE_PRIORITY = {
            Items.NETHERITE_AXE, Items.DIAMOND_AXE, Items.IRON_AXE,
            Items.GOLDEN_AXE, Items.STONE_AXE, Items.COPPER_AXE,
            Items.WOODEN_AXE
    };

    private static final Set<Item> AXE_ITEMS = Set.of(
            Items.NETHERITE_AXE, Items.DIAMOND_AXE, Items.IRON_AXE,
            Items.GOLDEN_AXE, Items.STONE_AXE, Items.COPPER_AXE,
            Items.WOODEN_AXE
    );

    // ---- Getters and setters ----

    public boolean isEnabled() { return enabled; }

    public void toggle() {
        enabled = !enabled;
        slamPending = false;
        cooldownTicks = 0;
    }

    public int getMinDelayMs() { return minDelayMs; }
    public void setMinDelayMs(int v) { minDelayMs = Math.max(0, Math.min(v, maxDelayMs)); }

    public int getMaxDelayMs() { return maxDelayMs; }
    public void setMaxDelayMs(int v) { maxDelayMs = Math.max(minDelayMs, Math.min(500, v)); }

    public void tick(MinecraftClient client) {
        if (!enabled) return;

        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        if (slamPending) return;

        // Only trigger on attack key press
        if (!client.options.attackKey.isPressed()) return;

        // Target must be a player who is blocking
        Entity target = client.targetedEntity;
        if (!(target instanceof PlayerEntity targetPlayer)) return;
        if (!targetPlayer.isAlive()) return;
        if (!targetPlayer.isBlocking()) return;

        // Already holding an axe — don't interfere
        Item heldItem = player.getMainHandStack().getItem();
        if (AXE_ITEMS.contains(heldItem)) return;

        // Need both axe and mace in hotbar
        int axeSlot = findBestAxeSlot(player);
        if (axeSlot == -1) return;

        int maceSlot = MacroUtils.findHotbarSlot(player, Items.MACE);
        if (maceSlot == -1) return;

        int previousSlot = player.getInventory().getSelectedSlot();
        int delay = randomDelay();

        slamPending = true;
        scheduleStunSlam(client, axeSlot, maceSlot, previousSlot, delay);
    }

    private int findBestAxeSlot(ClientPlayerEntity player) {
        for (Item axe : AXE_PRIORITY) {
            int slot = MacroUtils.findHotbarSlot(player, axe);
            if (slot != -1) return slot;
        }
        return -1;
    }

    private void scheduleStunSlam(MinecraftClient client, int axeSlot, int maceSlot, int previousSlot, int delayMs) {
        // Step 1: Swap to axe and attack (break shield)
        SCHEDULER.schedule(() -> {
            client.execute(() -> {
                if (!enabled) { slamPending = false; return; }
                ClientPlayerEntity player = client.player;
                if (player == null || client.interactionManager == null) { slamPending = false; return; }

                Entity target = client.targetedEntity;
                if (!(target instanceof PlayerEntity) || !target.isAlive()) { slamPending = false; return; }

                // Swap to axe and attack
                player.getInventory().setSelectedSlot(axeSlot);
                client.interactionManager.attackEntity(player, target);
                player.swingHand(Hand.MAIN_HAND);

                // Step 2: After delay, swap to mace and attack
                int maceDelay = randomDelay();
                SCHEDULER.schedule(() -> {
                    client.execute(() -> {
                        if (!enabled) { slamPending = false; return; }
                        ClientPlayerEntity p = client.player;
                        if (p == null || client.interactionManager == null) { slamPending = false; return; }

                        Entity t = client.targetedEntity;
                        if (!(t instanceof PlayerEntity) || !t.isAlive()) {
                            // Target gone — just swap back
                            p.getInventory().setSelectedSlot(previousSlot);
                            slamPending = false;
                            cooldownTicks = COOLDOWN_DURATION;
                            return;
                        }

                        // Swap to mace and attack
                        p.getInventory().setSelectedSlot(maceSlot);
                        client.interactionManager.attackEntity(p, t);
                        p.swingHand(Hand.MAIN_HAND);

                        // Step 3: Swap back after short delay
                        SCHEDULER.schedule(() -> {
                            client.execute(() -> {
                                if (client.player != null) {
                                    client.player.getInventory().setSelectedSlot(previousSlot);
                                }
                                slamPending = false;
                                cooldownTicks = COOLDOWN_DURATION;
                            });
                        }, 50, TimeUnit.MILLISECONDS);
                    });
                }, maceDelay, TimeUnit.MILLISECONDS);
            });
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    private int randomDelay() {
        if (minDelayMs >= maxDelayMs) return minDelayMs;
        return RANDOM.nextInt(minDelayMs, maxDelayMs + 1);
    }
}
