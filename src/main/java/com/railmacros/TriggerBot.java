package com.railmacros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TriggerBot {

    private boolean enabled = false;

    private static final Random RANDOM = new Random();

    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "TriggerBot-Scheduler");
                t.setDaemon(true);
                return t;
            });

    // ---- Configurable settings (exposed to menu sliders) ----

    // Miss chance: 0.0 = never miss, 1.0 = always miss
    private double missChance = 0.09;

    // Reaction delay range in ms
    private int minReactionDelayMs = 0;
    private int maxReactionDelayMs = 165;

    // Sprint cooldown threshold range (e.g. 90-104% -> min=0.90, max=1.04)
    private float sprintCooldownMin = 0.90f;
    private float sprintCooldownMax = 1.04f;

    // Crit cooldown threshold range (falling, not sprinting) (e.g. 99-111%)
    private float critCooldownMin = 0.99f;
    private float critCooldownMax = 1.11f;

    // Sweep cooldown threshold range (on ground, not sprinting) (e.g. 99-111%)
    private float sweepCooldownMin = 0.99f;
    private float sweepCooldownMax = 1.11f;

    // ---- Getters and setters for menu ----

    public double getMissChance() { return missChance; }
    public void setMissChance(double v) { missChance = Math.max(0, Math.min(1, v)); }

    public int getMinReactionDelayMs() { return minReactionDelayMs; }
    public void setMinReactionDelayMs(int v) { minReactionDelayMs = Math.max(0, Math.min(v, maxReactionDelayMs)); }

    public int getMaxReactionDelayMs() { return maxReactionDelayMs; }
    public void setMaxReactionDelayMs(int v) { maxReactionDelayMs = Math.max(minReactionDelayMs, Math.min(500, v)); }

    public float getSprintCooldownMin() { return sprintCooldownMin; }
    public void setSprintCooldownMin(float v) { sprintCooldownMin = Math.max(0.5f, Math.min(v, sprintCooldownMax)); }

    public float getSprintCooldownMax() { return sprintCooldownMax; }
    public void setSprintCooldownMax(float v) { sprintCooldownMax = Math.max(sprintCooldownMin, Math.min(1.5f, v)); }

    public float getCritCooldownMin() { return critCooldownMin; }
    public void setCritCooldownMin(float v) { critCooldownMin = Math.max(0.5f, Math.min(v, critCooldownMax)); }

    public float getCritCooldownMax() { return critCooldownMax; }
    public void setCritCooldownMax(float v) { critCooldownMax = Math.max(critCooldownMin, Math.min(1.5f, v)); }

    public float getSweepCooldownMin() { return sweepCooldownMin; }
    public void setSweepCooldownMin(float v) { sweepCooldownMin = Math.max(0.5f, Math.min(v, sweepCooldownMax)); }

    public float getSweepCooldownMax() { return sweepCooldownMax; }
    public void setSweepCooldownMax(float v) { sweepCooldownMax = Math.max(sweepCooldownMin, Math.min(1.5f, v)); }

    // ---- Runtime state ----

    // Per-attack randomized cooldown threshold when sprinting
    private float sprintCooldownThreshold = 1.0f;

    // Per-attack randomized cooldown threshold for crit when not sprinting (falling)
    private float critCooldownThreshold = 1.0f;

    // Per-attack randomized cooldown threshold for sweep when not sprinting (on ground)
    private float sweepCooldownThreshold = 1.0f;

    // Prevents queueing multiple attacks during the reaction delay
    private boolean attackPending = false;

    // Track consecutive ticks at full charge for overcooldown simulation
    private int ticksAtFullCharge = 0;

    // All sword, axe, and mace items
    private static final Set<Item> WEAPON_ITEMS = Set.of(
            Items.WOODEN_SWORD, Items.COPPER_SWORD, Items.STONE_SWORD,
            Items.GOLDEN_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD,
            Items.NETHERITE_SWORD,
            Items.WOODEN_AXE, Items.COPPER_AXE, Items.STONE_AXE,
            Items.GOLDEN_AXE, Items.IRON_AXE, Items.DIAMOND_AXE,
            Items.NETHERITE_AXE,
            Items.MACE
    );

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
    }

    public void tick(MinecraftClient client) {
        if (!enabled) return;

        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;

        // Only attack when holding a sword, axe, or mace
        Item heldItem = player.getMainHandStack().getItem();
        if (!WEAPON_ITEMS.contains(heldItem)) return;

        // Only target players that the crosshair is directly over
        Entity target = client.targetedEntity;
        if (!(target instanceof PlayerEntity)) return;
        if (!target.isAlive()) return;

        float cooldown = player.getAttackCooldownProgress(0.5f);

        // Track time at full charge for overcooldown simulation
        // MC caps getAttackCooldownProgress at 1.0, so we approximate >100%
        // by counting extra ticks. ~12 ticks = full recharge for a sword,
        // so each extra tick ~ 8% overcooldown.
        if (cooldown >= 1.0f) {
            ticksAtFullCharge++;
        } else {
            ticksAtFullCharge = 0;
        }
        float effectiveCooldown = cooldown + (ticksAtFullCharge * 0.08f);

        Vec3d velocity = player.getVelocity();
        boolean onGround = player.isOnGround();
        boolean falling = !onGround && velocity.y < 0;
        boolean jumpingUp = !onGround && velocity.y > 0;
        boolean sprinting = player.isSprinting();

        // Determine if this is a sprint hit scenario (sprinting on ground or jumping)
        boolean canSprintHit = sprinting && (onGround || jumpingUp);

        // Determine if this is a crit scenario (not sprinting, falling)
        boolean canCrit = !sprinting && falling;

        // Determine if this is a sweep scenario (not sprinting, on ground)
        boolean canSweep = !sprinting && onGround;

        // Cooldown thresholds vary by attack type
        if (canSprintHit) {
            if (effectiveCooldown < sprintCooldownThreshold) return;
        } else if (canCrit) {
            if (effectiveCooldown < critCooldownThreshold) return;
        } else if (canSweep) {
            if (effectiveCooldown < sweepCooldownThreshold) return;
        } else {
            if (cooldown < 1.0f) return;
        }

        // Don't queue another attack if one is already pending
        if (attackPending) return;

        // Miss chance -- skip this attack window
        if (RANDOM.nextDouble() < missChance) return;

        boolean shouldAttack = false;

        // Priority 1: Critical hit -- player is falling, attack now for crit damage
        if (falling) {
            shouldAttack = true;
        }
        // Priority 2: Sprint hit -- player is sprinting with upward momentum
        else if (jumpingUp && sprinting) {
            shouldAttack = true;
        }
        // If jumping up without sprint, wait to fall for a crit instead
        else if (jumpingUp && !sprinting) {
            return;
        }
        // On ground or other state: attack at full charge
        else {
            shouldAttack = true;
        }

        if (shouldAttack) {
            int delayRange = maxReactionDelayMs - minReactionDelayMs;
            int delay = minReactionDelayMs + (delayRange > 0 ? RANDOM.nextInt(delayRange + 1) : 0);
            scheduleAttack(client, delay);
        }
    }

    private void scheduleAttack(MinecraftClient client, int delayMs) {
        attackPending = true;
        SCHEDULER.schedule(() -> {
            client.execute(() -> {
                attackPending = false;
                if (!enabled) return;
                ClientPlayerEntity player = client.player;
                if (player == null || client.interactionManager == null) return;

                // Re-check target is still valid at time of attack
                Entity target = client.targetedEntity;
                if (!(target instanceof PlayerEntity)) return;
                if (!target.isAlive()) return;

                // Simulate a left click
                client.interactionManager.attackEntity(player, target);
                player.swingHand(Hand.MAIN_HAND);

                // Reset overcooldown counter after attacking
                ticksAtFullCharge = 0;

                // Randomize thresholds for the next attack using configurable ranges
                float sprintRange = sprintCooldownMax - sprintCooldownMin;
                sprintCooldownThreshold = sprintCooldownMin + RANDOM.nextFloat() * sprintRange;

                float critRange = critCooldownMax - critCooldownMin;
                critCooldownThreshold = critCooldownMin + RANDOM.nextFloat() * critRange;

                float sweepRange = sweepCooldownMax - sweepCooldownMin;
                sweepCooldownThreshold = sweepCooldownMin + RANDOM.nextFloat() * sweepRange;
            });
        }, delayMs, TimeUnit.MILLISECONDS);
    }
}
