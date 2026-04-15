package com.railmacros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * AutoMace Module: If the player attacks someone while falling with 2+ blocks of fall height,
 * automatically swaps to mace, attacks, then swaps back.
 *
 * Configurable delay in ms before the swap.
 * Toggled from menu only.
 */
public class AutoMace {

    private boolean enabled = false;

    private static final Random RANDOM = new Random();

    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "AutoMace-Scheduler");
                t.setDaemon(true);
                return t;
            });

    // Configurable delays (ms)
    private int minDelayMs = 0;
    private int maxDelayMs = 50;

    // Internal state
    private boolean macePending = false;
    private int cooldownTicks = 0;
    private static final int COOLDOWN_DURATION = 10;

    // ---- Getters and setters ----

    public boolean isEnabled() { return enabled; }

    public void toggle() {
        enabled = !enabled;
        macePending = false;
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

        if (macePending) return;

        // Only trigger on attack key press
        if (!client.options.attackKey.isPressed()) return;

        // Must be targeting a player
        Entity target = client.targetedEntity;
        if (!(target instanceof PlayerEntity)) return;
        if (!target.isAlive()) return;

        // Must be falling with 2+ blocks of fall height
        if (player.isOnGround()) return;
        if (player.getVelocity().y >= 0) return; // Must be falling (negative Y velocity)
        if (player.fallDistance < 2.0f) return; // Need at least 2 blocks of fall distance

        // Already holding mace — no swap needed
        if (player.getMainHandStack().getItem() == Items.MACE) return;

        // Need mace in hotbar
        int maceSlot = MacroUtils.findHotbarSlot(player, Items.MACE);
        if (maceSlot == -1) return;

        int previousSlot = player.getInventory().getSelectedSlot();
        int delay = randomDelay();

        macePending = true;
        scheduleAutoMace(client, maceSlot, previousSlot, delay);
    }

    private void scheduleAutoMace(MinecraftClient client, int maceSlot, int previousSlot, int delayMs) {
        SCHEDULER.schedule(() -> {
            client.execute(() -> {
                if (!enabled) { macePending = false; return; }
                ClientPlayerEntity player = client.player;
                if (player == null || client.interactionManager == null) { macePending = false; return; }

                Entity target = client.targetedEntity;
                if (!(target instanceof PlayerEntity) || !target.isAlive()) { macePending = false; return; }

                // Swap to mace and attack
                player.getInventory().setSelectedSlot(maceSlot);
                client.interactionManager.attackEntity(player, target);
                player.swingHand(Hand.MAIN_HAND);

                // Swap back after short delay
                SCHEDULER.schedule(() -> {
                    client.execute(() -> {
                        if (client.player != null) {
                            client.player.getInventory().setSelectedSlot(previousSlot);
                        }
                        macePending = false;
                        cooldownTicks = COOLDOWN_DURATION;
                    });
                }, 50, TimeUnit.MILLISECONDS);
            });
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    private int randomDelay() {
        if (minDelayMs >= maxDelayMs) return minDelayMs;
        return RANDOM.nextInt(minDelayMs, maxDelayMs + 1);
    }
}
