package com.railmacros;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Config persistence: saves and loads all module settings to a JSON file.
 * File is stored at .minecraft/config/rappture-client.json
 */
public class ModConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("RailMacros");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("rappture-client.json");
    }

    /**
     * Save all module settings to config file.
     */
    public static void save() {
        JsonObject root = new JsonObject();

        // Xbow Macro
        JsonObject xbow = new JsonObject();
        RailMacro rm = RailMacrosMod.RAIL_MACRO;
        xbow.addProperty("enabled", rm.isEnabled());
        xbow.addProperty("railToTntMinDelay", rm.getRailToTntMinDelay());
        xbow.addProperty("railToTntMaxDelay", rm.getRailToTntMaxDelay());
        xbow.addProperty("tntToFlintMinDelay", rm.getTntToFlintMinDelay());
        xbow.addProperty("tntToFlintMaxDelay", rm.getTntToFlintMaxDelay());
        xbow.addProperty("bowSuppressionMs", rm.getBowSuppressionMs());
        root.add("xbowMacro", xbow);

        // InstaCart (BowMacro)
        JsonObject instaCart = new JsonObject();
        BowMacro bm = RailMacrosMod.BOW_MACRO;
        instaCart.addProperty("enabled", bm.isEnabled());
        instaCart.addProperty("bowToRailDelay", bm.getBowToRailDelay());
        instaCart.addProperty("railToTntDelay", bm.getRailToTntDelay());
        instaCart.addProperty("suppressionWindowMs", bm.getSuppressionWindowMs());
        root.add("instaCart", instaCart);

        // TriggerBot
        JsonObject triggerBot = new JsonObject();
        TriggerBot tb = RailMacrosMod.TRIGGER_BOT;
        triggerBot.addProperty("enabled", tb.isEnabled());
        triggerBot.addProperty("missChance", tb.getMissChance());
        triggerBot.addProperty("minReactionDelayMs", tb.getMinReactionDelayMs());
        triggerBot.addProperty("maxReactionDelayMs", tb.getMaxReactionDelayMs());
        triggerBot.addProperty("sprintCooldownMin", tb.getSprintCooldownMin());
        triggerBot.addProperty("sprintCooldownMax", tb.getSprintCooldownMax());
        triggerBot.addProperty("critCooldownMin", tb.getCritCooldownMin());
        triggerBot.addProperty("critCooldownMax", tb.getCritCooldownMax());
        triggerBot.addProperty("sweepCooldownMin", tb.getSweepCooldownMin());
        triggerBot.addProperty("sweepCooldownMax", tb.getSweepCooldownMax());
        root.add("triggerBot", triggerBot);

        // ShieldBreaker
        JsonObject shieldBreaker = new JsonObject();
        ShieldBreaker sb = RailMacrosMod.SHIELD_BREAKER;
        shieldBreaker.addProperty("enabled", sb.isEnabled());
        shieldBreaker.addProperty("missChance", sb.getMissChance());
        shieldBreaker.addProperty("minDelayMs", sb.getMinDelayMs());
        shieldBreaker.addProperty("maxDelayMs", sb.getMaxDelayMs());
        root.add("shieldBreaker", shieldBreaker);

        // AutoSprint
        JsonObject autoSprint = new JsonObject();
        autoSprint.addProperty("enabled", RailMacrosMod.AUTO_SPRINT.isEnabled());
        root.add("autoSprint", autoSprint);

        // CartGuard
        JsonObject cartGuard = new JsonObject();
        cartGuard.addProperty("enabled", RailMacrosMod.CART_GUARD.isEnabled());
        root.add("cartGuard", cartGuard);

        // CrossbowSwap
        JsonObject xbowSwap = new JsonObject();
        xbowSwap.addProperty("enabled", RailMacrosMod.CROSSBOW_SWAP.isEnabled());
        root.add("crossbowSwap", xbowSwap);

        // HoverRefill
        JsonObject hoverRefill = new JsonObject();
        hoverRefill.addProperty("enabled", RailMacrosMod.HOVER_REFILL.isEnabled());
        root.add("hoverRefill", hoverRefill);

        try {
            Files.writeString(getConfigPath(), GSON.toJson(root));
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    /**
     * Load all module settings from config file.
     */
    public static void load() {
        Path path = getConfigPath();
        if (!Files.exists(path)) return;

        try {
            String json = Files.readString(path);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            // Xbow Macro
            if (root.has("xbowMacro")) {
                JsonObject xbow = root.getAsJsonObject("xbowMacro");
                RailMacro rm = RailMacrosMod.RAIL_MACRO;
                if (xbow.has("enabled") && xbow.get("enabled").getAsBoolean() != rm.isEnabled()) rm.toggle();
                if (xbow.has("railToTntMaxDelay")) rm.setRailToTntMaxDelay(xbow.get("railToTntMaxDelay").getAsInt());
                if (xbow.has("railToTntMinDelay")) rm.setRailToTntMinDelay(xbow.get("railToTntMinDelay").getAsInt());
                if (xbow.has("tntToFlintMaxDelay")) rm.setTntToFlintMaxDelay(xbow.get("tntToFlintMaxDelay").getAsInt());
                if (xbow.has("tntToFlintMinDelay")) rm.setTntToFlintMinDelay(xbow.get("tntToFlintMinDelay").getAsInt());
                if (xbow.has("bowSuppressionMs")) rm.setBowSuppressionMs(xbow.get("bowSuppressionMs").getAsInt());
            }

            // InstaCart
            if (root.has("instaCart")) {
                JsonObject ic = root.getAsJsonObject("instaCart");
                BowMacro bm = RailMacrosMod.BOW_MACRO;
                if (ic.has("enabled") && ic.get("enabled").getAsBoolean() != bm.isEnabled()) bm.toggle();
                if (ic.has("bowToRailDelay")) bm.setBowToRailDelay(ic.get("bowToRailDelay").getAsInt());
                if (ic.has("railToTntDelay")) bm.setRailToTntDelay(ic.get("railToTntDelay").getAsInt());
                if (ic.has("suppressionWindowMs")) bm.setSuppressionWindowMs(ic.get("suppressionWindowMs").getAsInt());
            }

            // TriggerBot
            if (root.has("triggerBot")) {
                JsonObject tbo = root.getAsJsonObject("triggerBot");
                TriggerBot tb = RailMacrosMod.TRIGGER_BOT;
                if (tbo.has("enabled") && tbo.get("enabled").getAsBoolean() != tb.isEnabled()) tb.toggle();
                if (tbo.has("missChance")) tb.setMissChance(tbo.get("missChance").getAsDouble());
                if (tbo.has("maxReactionDelayMs")) tb.setMaxReactionDelayMs(tbo.get("maxReactionDelayMs").getAsInt());
                if (tbo.has("minReactionDelayMs")) tb.setMinReactionDelayMs(tbo.get("minReactionDelayMs").getAsInt());
                if (tbo.has("sprintCooldownMax")) tb.setSprintCooldownMax(tbo.get("sprintCooldownMax").getAsFloat());
                if (tbo.has("sprintCooldownMin")) tb.setSprintCooldownMin(tbo.get("sprintCooldownMin").getAsFloat());
                if (tbo.has("critCooldownMax")) tb.setCritCooldownMax(tbo.get("critCooldownMax").getAsFloat());
                if (tbo.has("critCooldownMin")) tb.setCritCooldownMin(tbo.get("critCooldownMin").getAsFloat());
                if (tbo.has("sweepCooldownMax")) tb.setSweepCooldownMax(tbo.get("sweepCooldownMax").getAsFloat());
                if (tbo.has("sweepCooldownMin")) tb.setSweepCooldownMin(tbo.get("sweepCooldownMin").getAsFloat());
            }

            // ShieldBreaker
            if (root.has("shieldBreaker")) {
                JsonObject sbo = root.getAsJsonObject("shieldBreaker");
                ShieldBreaker sb = RailMacrosMod.SHIELD_BREAKER;
                if (sbo.has("enabled") && sbo.get("enabled").getAsBoolean() != sb.isEnabled()) sb.toggle();
                if (sbo.has("missChance")) sb.setMissChance(sbo.get("missChance").getAsDouble());
                if (sbo.has("maxDelayMs")) sb.setMaxDelayMs(sbo.get("maxDelayMs").getAsInt());
                if (sbo.has("minDelayMs")) sb.setMinDelayMs(sbo.get("minDelayMs").getAsInt());
            }

            // AutoSprint
            if (root.has("autoSprint")) {
                JsonObject aso = root.getAsJsonObject("autoSprint");
                if (aso.has("enabled") && aso.get("enabled").getAsBoolean() != RailMacrosMod.AUTO_SPRINT.isEnabled()) {
                    RailMacrosMod.AUTO_SPRINT.toggle();
                }
            }

            // CartGuard
            if (root.has("cartGuard")) {
                JsonObject cg = root.getAsJsonObject("cartGuard");
                if (cg.has("enabled") && cg.get("enabled").getAsBoolean() != RailMacrosMod.CART_GUARD.isEnabled()) {
                    RailMacrosMod.CART_GUARD.toggle();
                }
            }

            // CrossbowSwap
            if (root.has("crossbowSwap")) {
                JsonObject xs = root.getAsJsonObject("crossbowSwap");
                if (xs.has("enabled") && xs.get("enabled").getAsBoolean() != RailMacrosMod.CROSSBOW_SWAP.isEnabled()) {
                    RailMacrosMod.CROSSBOW_SWAP.toggle();
                }
            }

            // HoverRefill
            if (root.has("hoverRefill")) {
                JsonObject hr = root.getAsJsonObject("hoverRefill");
                if (hr.has("enabled") && hr.get("enabled").getAsBoolean() != RailMacrosMod.HOVER_REFILL.isEnabled()) {
                    RailMacrosMod.HOVER_REFILL.toggle();
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed to load config", e);
        }
    }
}
