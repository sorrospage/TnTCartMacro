package com.railmacros;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class ModMenuScreen extends Screen {

    private int winX, winY, winW, winH;
    private boolean dragging = false;
    private int dragOffX, dragOffY;
    private int scrollOffset = 0;
    private int contentHeight = 0;

    private static final int COL_BG         = 0xF0141420;
    private static final int COL_TITLE_BG   = 0xFF1A1430;
    private static final int COL_HEADER     = 0xCC33264D;
    private static final int COL_HEADER_HOV = 0xCC4D3373;
    private static final int COL_SLIDER_BG  = 0xFF1E1830;
    private static final int COL_SLIDER_FG  = 0xFF7358B8;
    private static final int COL_SLIDER_HOV = 0xFF8D6ED6;
    private static final int COL_CHECK_ON   = 0xFF7358B8;
    private static final int COL_CHECK_OFF  = 0xFF2A2240;
    private static final int COL_BORDER     = 0xFF3D3260;
    private static final int COL_TEXT       = 0xFFE0D8F0;
    private static final int COL_TEXT_DIM   = 0xFFA098B0;
    private static final int COL_SEPARATOR  = 0x80403858;

    private static final int TITLE_H = 24;
    private static final int ROW_H   = 18;
    private static final int PAD     = 8;
    private static final int CHECK_SZ = 12;
    private static final int SLIDER_H = 14;

    private final boolean[] collapsed = new boolean[]{true, true, true, true, true, true, true};
    private int activeSlider = -1;
    private int widgetId = 0;
    private final List<SliderState> sliders = new ArrayList<>();
    private final List<CheckboxState> checkboxes = new ArrayList<>();
    private final List<HeaderState> headers = new ArrayList<>();

    private static class SliderState {
        int id, x, y, w;
        float min, max;
        boolean isInt;
        SliderCallback callback;
    }

    private static class CheckboxState {
        int x, y;
        CheckCallback callback;
    }

    private static class HeaderState {
        int x, y, w, h, index;
    }

    @FunctionalInterface
    private interface SliderCallback { void apply(float value); }
    @FunctionalInterface
    private interface CheckCallback { void apply(); }
    @FunctionalInterface
    private interface IntSliderCallback { void apply(int value); }
    @FunctionalInterface
    private interface FloatSliderCallback { void apply(float value); }

    public ModMenuScreen() {
        super(Text.literal("Rappture Client"));
    }

    @Override
    protected void init() {
        super.init();
        winW = 320;
        winH = 420;
        winX = (this.width - winW) / 2;
        winY = (this.height - winH) / 2;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0x66000000);
        sliders.clear();
        checkboxes.clear();
        headers.clear();
        widgetId = 0;

        ctx.fill(winX - 1, winY - 1, winX + winW + 1, winY + winH + 1, COL_BORDER);
        ctx.fill(winX, winY, winX + winW, winY + winH, COL_BG);
        ctx.fill(winX, winY, winX + winW, winY + TITLE_H, COL_TITLE_BG);
        ctx.drawCenteredTextWithShadow(textRenderer, "Rappture Client", winX + winW / 2, winY + 7, 0xFF9D8AE0);

        int contentTop = winY + TITLE_H;
        ctx.enableScissor(winX, contentTop, winX + winW, winY + winH);
        int y = contentTop - scrollOffset + 4;

        RailMacro rm = RailMacrosMod.RAIL_MACRO;
        y = renderHeader(ctx, mouseX, mouseY, y, 0, "Xbow Macro", rm.isEnabled());
        if (!collapsed[0]) {
            y = renderCheckbox(ctx, mouseX, mouseY, y, "Enabled", rm.isEnabled(), () -> { rm.toggle(); ModConfig.save(); });
            y = renderSliderInt(ctx, mouseX, mouseY, y, "Rail>TNT Min", rm.getRailToTntMinDelay(), 0, 10, "f", v -> rm.setRailToTntMinDelay(v));
            y = renderSliderInt(ctx, mouseX, mouseY, y, "Rail>TNT Max", rm.getRailToTntMaxDelay(), 0, 10, "f", v -> rm.setRailToTntMaxDelay(v));
            y = renderSliderInt(ctx, mouseX, mouseY, y, "TNT>Flint Min", rm.getTntToFlintMinDelay(), 0, 10, "f", v -> rm.setTntToFlintMinDelay(v));
            y = renderSliderInt(ctx, mouseX, mouseY, y, "TNT>Flint Max", rm.getTntToFlintMaxDelay(), 0, 10, "f", v -> rm.setTntToFlintMaxDelay(v));
            y = renderSliderInt(ctx, mouseX, mouseY, y, "Bow Suppress", rm.getBowSuppressionMs(), 0, 2000, "ms", v -> rm.setBowSuppressionMs(v));
            y = renderSeparator(ctx, y);
        }

        BowMacro bm = RailMacrosMod.BOW_MACRO;
        y = renderHeader(ctx, mouseX, mouseY, y, 1, "InstaCart", bm.isEnabled());
        if (!collapsed[1]) {
            y = renderCheckbox(ctx, mouseX, mouseY, y, "Enabled", bm.isEnabled(), () -> { bm.toggle(); ModConfig.save(); });
            y = renderSliderInt(ctx, mouseX, mouseY, y, "Bow>Rail Delay", bm.getBowToRailDelay(), 0, 500, "ms", v -> bm.setBowToRailDelay(v));
            y = renderSliderInt(ctx, mouseX, mouseY, y, "Rail>TNT Delay", bm.getRailToTntDelay(), 0, 500, "ms", v -> bm.setRailToTntDelay(v));
            y = renderSliderInt(ctx, mouseX, mouseY, y, "Suppress Window", bm.getSuppressionWindowMs(), 0, 2000, "ms", v -> bm.setSuppressionWindowMs(v));
            y = renderSeparator(ctx, y);
        }

        TriggerBot tb = RailMacrosMod.TRIGGER_BOT;
        y = renderHeader(ctx, mouseX, mouseY, y, 2, "TriggerBot", tb.isEnabled());
        if (!collapsed[2]) {
            y = renderCheckbox(ctx, mouseX, mouseY, y, "Enabled", tb.isEnabled(), () -> { tb.toggle(); ModConfig.save(); });
            y = renderSliderFloat(ctx, mouseX, mouseY, y, "Miss Chance", (float)(tb.getMissChance() * 100.0), 0, 50, "%", v -> tb.setMissChance(v / 100.0));
            y = renderSliderInt(ctx, mouseX, mouseY, y, "Min Delay", tb.getMinReactionDelayMs(), 0, 500, "ms", v -> tb.setMinReactionDelayMs(v));
            y = renderSliderInt(ctx, mouseX, mouseY, y, "Max Delay", tb.getMaxReactionDelayMs(), 0, 500, "ms", v -> tb.setMaxReactionDelayMs(v));
            y = renderSliderFloat(ctx, mouseX, mouseY, y, "Sprint CD Min", (float)(tb.getSprintCooldownMin() * 100.0), 50, 150, "%", v -> tb.setSprintCooldownMin(v / 100.0f));
            y = renderSliderFloat(ctx, mouseX, mouseY, y, "Sprint CD Max", (float)(tb.getSprintCooldownMax() * 100.0), 50, 150, "%", v -> tb.setSprintCooldownMax(v / 100.0f));
            y = renderSliderFloat(ctx, mouseX, mouseY, y, "Crit CD Min", (float)(tb.getCritCooldownMin() * 100.0), 50, 150, "%", v -> tb.setCritCooldownMin(v / 100.0f));
            y = renderSliderFloat(ctx, mouseX, mouseY, y, "Crit CD Max", (float)(tb.getCritCooldownMax() * 100.0), 50, 150, "%", v -> tb.setCritCooldownMax(v / 100.0f));
            y = renderSliderFloat(ctx, mouseX, mouseY, y, "Sweep CD Min", (float)(tb.getSweepCooldownMin() * 100.0), 50, 150, "%", v -> tb.setSweepCooldownMin(v / 100.0f));
            y = renderSliderFloat(ctx, mouseX, mouseY, y, "Sweep CD Max", (float)(tb.getSweepCooldownMax() * 100.0), 50, 150, "%", v -> tb.setSweepCooldownMax(v / 100.0f));
            y = renderSeparator(ctx, y);
        }

        ShieldBreaker sb = RailMacrosMod.SHIELD_BREAKER;
        y = renderHeader(ctx, mouseX, mouseY, y, 3, "ShieldBreaker", sb.isEnabled());
        if (!collapsed[3]) {
            y = renderCheckbox(ctx, mouseX, mouseY, y, "Enabled", sb.isEnabled(), () -> { sb.toggle(); ModConfig.save(); });
            y = renderSliderFloat(ctx, mouseX, mouseY, y, "Miss Chance", (float)(sb.getMissChance() * 100.0), 0, 50, "%", v -> sb.setMissChance(v / 100.0));
            y = renderSliderInt(ctx, mouseX, mouseY, y, "Min Delay", sb.getMinDelayMs(), 0, 500, "ms", v -> sb.setMinDelayMs(v));
            y = renderSliderInt(ctx, mouseX, mouseY, y, "Max Delay", sb.getMaxDelayMs(), 0, 500, "ms", v -> sb.setMaxDelayMs(v));
            y = renderSeparator(ctx, y);
        }

        y = renderHeader(ctx, mouseX, mouseY, y, 4, "AutoSprint", RailMacrosMod.AUTO_SPRINT.isEnabled());
        if (!collapsed[4]) {
            y = renderCheckbox(ctx, mouseX, mouseY, y, "Enabled", RailMacrosMod.AUTO_SPRINT.isEnabled(), () -> { RailMacrosMod.AUTO_SPRINT.toggle(); ModConfig.save(); });
            y = renderSeparator(ctx, y);
        }

        y = renderHeader(ctx, mouseX, mouseY, y, 5, "CartGuard", RailMacrosMod.CART_GUARD.isEnabled());
        if (!collapsed[5]) {
            y = renderCheckbox(ctx, mouseX, mouseY, y, "Enabled", RailMacrosMod.CART_GUARD.isEnabled(), () -> { RailMacrosMod.CART_GUARD.toggle(); ModConfig.save(); });
            y = renderSeparator(ctx, y);
        }

        y = renderHeader(ctx, mouseX, mouseY, y, 6, "CrossbowSwap (MB5)", RailMacrosMod.CROSSBOW_SWAP.isEnabled());
        if (!collapsed[6]) {
            y = renderCheckbox(ctx, mouseX, mouseY, y, "Enabled", RailMacrosMod.CROSSBOW_SWAP.isEnabled(), () -> { RailMacrosMod.CROSSBOW_SWAP.toggle(); ModConfig.save(); });
            y = renderSeparator(ctx, y);
        }

        contentHeight = y + scrollOffset - contentTop;
        ctx.disableScissor();
    }

    private int renderHeader(DrawContext ctx, int mx, int my, int y, int idx, String title, boolean enabled) {
        int x = winX + PAD;
        int w = winW - PAD * 2;
        int h = ROW_H + 2;
        boolean hovered = mx >= x && mx < x + w && my >= y && my < y + h;
        ctx.fill(x, y, x + w, y + h, hovered ? COL_HEADER_HOV : COL_HEADER);
        String arrow = collapsed[idx] ? "> " : "v ";
        String status = enabled ? " [ON]" : " [OFF]";
        int statusColor = enabled ? 0xFF55FF55 : 0xFFFF5555;
        ctx.drawTextWithShadow(textRenderer, arrow + title, x + 6, y + 5, COL_TEXT);
        int titleW = textRenderer.getWidth(arrow + title);
        ctx.drawTextWithShadow(textRenderer, status, x + 6 + titleW, y + 5, statusColor);
        HeaderState hs = new HeaderState();
        hs.x = x; hs.y = y; hs.w = w; hs.h = h; hs.index = idx;
        headers.add(hs);
        return y + h + 2;
    }

    private int renderCheckbox(DrawContext ctx, int mx, int my, int y, String label, boolean value, CheckCallback cb) {
        int x = winX + PAD + 12;
        ctx.fill(x, y + 2, x + CHECK_SZ, y + 2 + CHECK_SZ, value ? COL_CHECK_ON : COL_CHECK_OFF);
        if (value) {
            ctx.drawTextWithShadow(textRenderer, "x", x + 3, y + 2, 0xFFFFFFFF);
        }
        ctx.drawTextWithShadow(textRenderer, label, x + CHECK_SZ + 6, y + 3, COL_TEXT);
        CheckboxState cs = new CheckboxState();
        cs.x = x; cs.y = y; cs.callback = cb;
        checkboxes.add(cs);
        return y + ROW_H;
    }

    private int renderSliderInt(DrawContext ctx, int mx, int my, int y, String label, int value, int min, int max, String suffix, IntSliderCallback cb) {
        int id = widgetId++;
        int x = winX + PAD + 12;
        int totalW = winW - PAD * 2 - 24;
        int labelW = textRenderer.getWidth(label) + 6;
        int sliderX = x + labelW;
        int sliderW = totalW - labelW;
        ctx.drawTextWithShadow(textRenderer, label, x, y + 3, COL_TEXT_DIM);
        ctx.fill(sliderX, y + 2, sliderX + sliderW, y + SLIDER_H, COL_SLIDER_BG);
        float pct = (max > min) ? (float)(value - min) / (max - min) : 0;
        int fillW = (int)(sliderW * pct);
        boolean hovered = mx >= sliderX && mx < sliderX + sliderW && my >= y && my < y + ROW_H;
        ctx.fill(sliderX, y + 2, sliderX + fillW, y + SLIDER_H, hovered ? COL_SLIDER_HOV : COL_SLIDER_FG);
        String valText = value + suffix;
        ctx.drawCenteredTextWithShadow(textRenderer, valText, sliderX + sliderW / 2, y + 3, 0xFFFFFFFF);
        SliderState s = new SliderState();
        s.id = id; s.x = sliderX; s.y = y; s.w = sliderW;
        s.min = min; s.max = max; s.isInt = true;
        s.callback = v -> cb.apply((int) v);
        sliders.add(s);
        return y + ROW_H;
    }

    private int renderSliderFloat(DrawContext ctx, int mx, int my, int y, String label, float value, float min, float max, String suffix, FloatSliderCallback cb) {
        int id = widgetId++;
        int x = winX + PAD + 12;
        int totalW = winW - PAD * 2 - 24;
        int labelW = textRenderer.getWidth(label) + 6;
        int sliderX = x + labelW;
        int sliderW = totalW - labelW;
        ctx.drawTextWithShadow(textRenderer, label, x, y + 3, COL_TEXT_DIM);
        ctx.fill(sliderX, y + 2, sliderX + sliderW, y + SLIDER_H, COL_SLIDER_BG);
        float pct = (max > min) ? (value - min) / (max - min) : 0;
        int fillW = (int)(sliderW * pct);
        boolean hovered = mx >= sliderX && mx < sliderX + sliderW && my >= y && my < y + ROW_H;
        ctx.fill(sliderX, y + 2, sliderX + fillW, y + SLIDER_H, hovered ? COL_SLIDER_HOV : COL_SLIDER_FG);
        String valText = String.format("%.0f%s", value, suffix);
        ctx.drawCenteredTextWithShadow(textRenderer, valText, sliderX + sliderW / 2, y + 3, 0xFFFFFFFF);
        SliderState s = new SliderState();
        s.id = id; s.x = sliderX; s.y = y; s.w = sliderW;
        s.min = min; s.max = max; s.isInt = false;
        s.callback = v -> cb.apply(v);
        sliders.add(s);
        return y + ROW_H;
    }

    private int renderSeparator(DrawContext ctx, int y) {
        ctx.fill(winX + PAD, y + 2, winX + winW - PAD, y + 3, COL_SEPARATOR);
        return y + 6;
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        int button = click.button();
        if (button != 0) return super.mouseClicked(click, bl);
        int mx = (int) click.x();
        int my = (int) click.y();
        if (mx >= winX && mx < winX + winW && my >= winY && my < winY + TITLE_H) {
            dragging = true;
            dragOffX = mx - winX;
            dragOffY = my - winY;
            return true;
        }
        for (HeaderState h : headers) {
            if (mx >= h.x && mx < h.x + h.w && my >= h.y && my < h.y + h.h) {
                collapsed[h.index] = !collapsed[h.index];
                return true;
            }
        }
        for (CheckboxState c : checkboxes) {
            if (mx >= c.x && mx < c.x + CHECK_SZ + 60 && my >= c.y && my < c.y + ROW_H) {
                c.callback.apply();
                return true;
            }
        }
        for (SliderState s : sliders) {
            if (mx >= s.x && mx < s.x + s.w && my >= s.y && my < s.y + ROW_H) {
                activeSlider = s.id;
                updateSlider(s, mx);
                return true;
            }
        }
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        int button = click.button();
        if (button != 0) return super.mouseDragged(click, deltaX, deltaY);
        int mx = (int) click.x();
        int my = (int) click.y();
        if (dragging) {
            winX = mx - dragOffX;
            winY = my - dragOffY;
            return true;
        }
        if (activeSlider >= 0) {
            for (SliderState s : sliders) {
                if (s.id == activeSlider) {
                    updateSlider(s, mx);
                    return true;
                }
            }
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) {
            dragging = false;
            activeSlider = -1;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= winX && mouseX < winX + winW && mouseY >= winY + TITLE_H && mouseY < winY + winH) {
            scrollOffset -= (int)(verticalAmount * 16);
            int maxScroll = Math.max(0, contentHeight - (winH - TITLE_H));
            scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void updateSlider(SliderState s, int mx) {
        float pct = MathHelper.clamp((float)(mx - s.x) / s.w, 0, 1);
        float newVal = s.min + pct * (s.max - s.min);
        if (s.isInt) { newVal = Math.round(newVal); }
        s.callback.apply(newVal);
    }

    @Override
    public void close() {
        ModConfig.save();
        super.close();
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    @Override
    public boolean shouldPause() { return false; }
}
