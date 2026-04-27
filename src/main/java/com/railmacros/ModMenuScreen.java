package com.railmacros;

import imgui.type.ImBoolean;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Module menu opened with the Pause key.
 * Uses Dear ImGui for a floating window with collapsible sections per module.
 */
public class ModMenuScreen extends Screen {

    // Temporary arrays for ImGui widget state (reused each frame)
    private final int[] tmpInt = new int[1];
    private final float[] tmpFloat = new float[1];
    private final ImBoolean tmpBool = new ImBoolean();

    public ModMenuScreen() {
        super(Text.literal("Rappture Client"));
    }

    @Override
    protected void init() {
        super.init();
        // Initialize ImGui on first menu open
        ImGuiManager.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw semi-transparent background
        context.fill(0, 0, this.width, this.height, 0x66000000);

        // Render ImGui overlay
        if (ImGuiManager.isInitialized()) {
            ImGuiManager.startFrame();
            renderImGuiMenu();
            ImGuiManager.endFrame();
        }
    }

    private void renderImGuiMenu() {
        // Center the window on first appearance
        ImGui.setNextWindowPos(
                ImGui.getMainViewport().getSizeX() / 2.0f - 175,
                ImGui.getMainViewport().getSizeY() / 2.0f - 250,
                ImGuiCond.FirstUseEver
        );
        ImGui.setNextWindowSize(350, 500, ImGuiCond.FirstUseEver);

        int windowFlags = ImGuiWindowFlags.NoCollapse;
        if (ImGui.begin("Rappture Client", windowFlags)) {

            // ===== Xbow Macro =====
            if (ImGui.collapsingHeader("Xbow Macro")) {
                RailMacro rm = RailMacrosMod.RAIL_MACRO;

                tmpBool.set(rm.isEnabled());
                if (ImGui.checkbox("Enabled##xbow", tmpBool)) {
                    rm.toggle();
                    ModConfig.save();
                }

                tmpInt[0] = rm.getRailToTntMinDelay();
                if (ImGui.sliderInt("Rail\u2192TNT Min##xbow", tmpInt, 0, 10, "%df")) {
                    rm.setRailToTntMinDelay(tmpInt[0]);
                }

                tmpInt[0] = rm.getRailToTntMaxDelay();
                if (ImGui.sliderInt("Rail\u2192TNT Max##xbow", tmpInt, 0, 10, "%df")) {
                    rm.setRailToTntMaxDelay(tmpInt[0]);
                }

                tmpInt[0] = rm.getTntToFlintMinDelay();
                if (ImGui.sliderInt("TNT\u2192Flint Min##xbow", tmpInt, 0, 10, "%df")) {
                    rm.setTntToFlintMinDelay(tmpInt[0]);
                }

                tmpInt[0] = rm.getTntToFlintMaxDelay();
                if (ImGui.sliderInt("TNT\u2192Flint Max##xbow", tmpInt, 0, 10, "%df")) {
                    rm.setTntToFlintMaxDelay(tmpInt[0]);
                }

                tmpInt[0] = rm.getBowSuppressionMs();
                if (ImGui.sliderInt("Bow Suppress##xbow", tmpInt, 0, 2000, "%dms")) {
                    rm.setBowSuppressionMs(tmpInt[0]);
                }

                ImGui.separator();
            }

            // ===== InstaCart Macro =====
            if (ImGui.collapsingHeader("InstaCart")) {
                BowMacro bm = RailMacrosMod.BOW_MACRO;

                tmpBool.set(bm.isEnabled());
                if (ImGui.checkbox("Enabled##instacart", tmpBool)) {
                    bm.toggle();
                    ModConfig.save();
                }

                tmpInt[0] = bm.getBowToRailDelay();
                if (ImGui.sliderInt("Bow\u2192Rail Delay##ic", tmpInt, 0, 500, "%dms")) {
                    bm.setBowToRailDelay(tmpInt[0]);
                }

                tmpInt[0] = bm.getRailToTntDelay();
                if (ImGui.sliderInt("Rail\u2192TNT Delay##ic", tmpInt, 0, 500, "%dms")) {
                    bm.setRailToTntDelay(tmpInt[0]);
                }

                tmpInt[0] = bm.getSuppressionWindowMs();
                if (ImGui.sliderInt("Suppress Window##ic", tmpInt, 0, 2000, "%dms")) {
                    bm.setSuppressionWindowMs(tmpInt[0]);
                }

                ImGui.separator();
            }

            // ===== TriggerBot =====
            if (ImGui.collapsingHeader("TriggerBot")) {
                TriggerBot tb = RailMacrosMod.TRIGGER_BOT;

                tmpBool.set(tb.isEnabled());
                if (ImGui.checkbox("Enabled##triggerbot", tmpBool)) {
                    tb.toggle();
                    ModConfig.save();
                }

                tmpFloat[0] = (float)(tb.getMissChance() * 100.0);
                if (ImGui.sliderFloat("Miss Chance##tb", tmpFloat, 0, 50, "%.0f%%")) {
                    tb.setMissChance(tmpFloat[0] / 100.0);
                }

                tmpInt[0] = tb.getMinReactionDelayMs();
                if (ImGui.sliderInt("Min Delay##tb", tmpInt, 0, 500, "%dms")) {
                    tb.setMinReactionDelayMs(tmpInt[0]);
                }

                tmpInt[0] = tb.getMaxReactionDelayMs();
                if (ImGui.sliderInt("Max Delay##tb", tmpInt, 0, 500, "%dms")) {
                    tb.setMaxReactionDelayMs(tmpInt[0]);
                }

                tmpFloat[0] = (float)(tb.getSprintCooldownMin() * 100.0);
                if (ImGui.sliderFloat("Sprint CD Min##tb", tmpFloat, 50, 150, "%.0f%%")) {
                    tb.setSprintCooldownMin(tmpFloat[0] / 100.0f);
                }

                tmpFloat[0] = (float)(tb.getSprintCooldownMax() * 100.0);
                if (ImGui.sliderFloat("Sprint CD Max##tb", tmpFloat, 50, 150, "%.0f%%")) {
                    tb.setSprintCooldownMax(tmpFloat[0] / 100.0f);
                }

                tmpFloat[0] = (float)(tb.getCritCooldownMin() * 100.0);
                if (ImGui.sliderFloat("Crit CD Min##tb", tmpFloat, 50, 150, "%.0f%%")) {
                    tb.setCritCooldownMin(tmpFloat[0] / 100.0f);
                }

                tmpFloat[0] = (float)(tb.getCritCooldownMax() * 100.0);
                if (ImGui.sliderFloat("Crit CD Max##tb", tmpFloat, 50, 150, "%.0f%%")) {
                    tb.setCritCooldownMax(tmpFloat[0] / 100.0f);
                }

                tmpFloat[0] = (float)(tb.getSweepCooldownMin() * 100.0);
                if (ImGui.sliderFloat("Sweep CD Min##tb", tmpFloat, 50, 150, "%.0f%%")) {
                    tb.setSweepCooldownMin(tmpFloat[0] / 100.0f);
                }

                tmpFloat[0] = (float)(tb.getSweepCooldownMax() * 100.0);
                if (ImGui.sliderFloat("Sweep CD Max##tb", tmpFloat, 50, 150, "%.0f%%")) {
                    tb.setSweepCooldownMax(tmpFloat[0] / 100.0f);
                }

                ImGui.separator();
            }

            // ===== ShieldBreaker =====
            if (ImGui.collapsingHeader("ShieldBreaker")) {
                ShieldBreaker sb = RailMacrosMod.SHIELD_BREAKER;

                tmpBool.set(sb.isEnabled());
                if (ImGui.checkbox("Enabled##shieldbreaker", tmpBool)) {
                    sb.toggle();
                    ModConfig.save();
                }

                tmpFloat[0] = (float)(sb.getMissChance() * 100.0);
                if (ImGui.sliderFloat("Miss Chance##sb", tmpFloat, 0, 50, "%.0f%%")) {
                    sb.setMissChance(tmpFloat[0] / 100.0);
                }

                tmpInt[0] = sb.getMinDelayMs();
                if (ImGui.sliderInt("Min Delay##sb", tmpInt, 0, 500, "%dms")) {
                    sb.setMinDelayMs(tmpInt[0]);
                }

                tmpInt[0] = sb.getMaxDelayMs();
                if (ImGui.sliderInt("Max Delay##sb", tmpInt, 0, 500, "%dms")) {
                    sb.setMaxDelayMs(tmpInt[0]);
                }

                ImGui.separator();
            }

            // ===== AutoSprint =====
            if (ImGui.collapsingHeader("AutoSprint")) {
                tmpBool.set(RailMacrosMod.AUTO_SPRINT.isEnabled());
                if (ImGui.checkbox("Enabled##autosprint", tmpBool)) {
                    RailMacrosMod.AUTO_SPRINT.toggle();
                    ModConfig.save();
                }

                ImGui.separator();
            }

            // ===== CartGuard =====
            if (ImGui.collapsingHeader("CartGuard")) {
                tmpBool.set(RailMacrosMod.CART_GUARD.isEnabled());
                if (ImGui.checkbox("Enabled##cartguard", tmpBool)) {
                    RailMacrosMod.CART_GUARD.toggle();
                    ModConfig.save();
                }

                ImGui.separator();
            }

            // ===== CrossbowSwap =====
            if (ImGui.collapsingHeader("CrossbowSwap (MB5)")) {
                tmpBool.set(RailMacrosMod.CROSSBOW_SWAP.isEnabled());
                if (ImGui.checkbox("Enabled##xbowswap", tmpBool)) {
                    RailMacrosMod.CROSSBOW_SWAP.toggle();
                    ModConfig.save();
                }

                ImGui.separator();
            }
        }
        ImGui.end();
    }

    @Override
    public void close() {
        // Save config whenever the menu is closed
        ModConfig.save();
        super.close();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
