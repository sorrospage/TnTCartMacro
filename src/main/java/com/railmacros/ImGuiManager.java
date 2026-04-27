package com.railmacros;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the Dear ImGui lifecycle: initialization, frame management, and disposal.
 * Uses Minecraft's GLFW window for input and OpenGL for rendering.
 */
public class ImGuiManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("RailMacros");

    private static final ImGuiImplGlfw IMGUI_GLFW = new ImGuiImplGlfw();
    private static final ImGuiImplGl3 IMGUI_GL3 = new ImGuiImplGl3();

    private static boolean initialized = false;

    /**
     * Initialize ImGui with Minecraft's GLFW window.
     * Must be called on the render thread after the window is created.
     */
    public static void init() {
        if (initialized) return;

        try {
            long windowHandle = MinecraftClient.getInstance().getWindow().getHandle();

            ImGui.createContext();

            ImGuiIO io = ImGui.getIO();
            io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
            io.setIniFilename(null); // Don't save layout to file

            // Set up dark style with custom colors
            setupStyle();

            // Initialize GLFW backend (installCallbacks=true chains with MC's callbacks)
            IMGUI_GLFW.init(windowHandle, true);

            // Initialize OpenGL3 backend (Minecraft uses GL 3.2 / GLSL 150)
            IMGUI_GL3.init("#version 150");

            initialized = true;
            LOGGER.info("[ImGuiManager] Initialized successfully");
        } catch (Exception e) {
            LOGGER.error("[ImGuiManager] Failed to initialize", e);
        }
    }

    /**
     * Configure the ImGui color scheme - dark theme with accent colors.
     */
    private static void setupStyle() {
        ImGui.styleColorsDark();

        // Window styling
        ImGui.getStyle().setWindowRounding(6.0f);
        ImGui.getStyle().setFrameRounding(4.0f);
        ImGui.getStyle().setGrabRounding(3.0f);
        ImGui.getStyle().setScrollbarRounding(4.0f);
        ImGui.getStyle().setWindowPadding(10.0f, 10.0f);
        ImGui.getStyle().setFramePadding(6.0f, 4.0f);
        ImGui.getStyle().setItemSpacing(8.0f, 6.0f);

        // Custom colors - dark purple/blue theme
        ImGui.getStyle().setColor(ImGuiCol.WindowBg, 0.08f, 0.08f, 0.12f, 0.94f);
        ImGui.getStyle().setColor(ImGuiCol.TitleBg, 0.10f, 0.08f, 0.16f, 1.00f);
        ImGui.getStyle().setColor(ImGuiCol.TitleBgActive, 0.16f, 0.12f, 0.24f, 1.00f);
        ImGui.getStyle().setColor(ImGuiCol.Header, 0.20f, 0.15f, 0.30f, 0.80f);
        ImGui.getStyle().setColor(ImGuiCol.HeaderHovered, 0.30f, 0.20f, 0.45f, 0.80f);
        ImGui.getStyle().setColor(ImGuiCol.HeaderActive, 0.35f, 0.25f, 0.50f, 1.00f);
        ImGui.getStyle().setColor(ImGuiCol.Button, 0.20f, 0.15f, 0.30f, 0.80f);
        ImGui.getStyle().setColor(ImGuiCol.ButtonHovered, 0.30f, 0.22f, 0.45f, 1.00f);
        ImGui.getStyle().setColor(ImGuiCol.ButtonActive, 0.35f, 0.28f, 0.55f, 1.00f);
        ImGui.getStyle().setColor(ImGuiCol.FrameBg, 0.12f, 0.10f, 0.18f, 0.90f);
        ImGui.getStyle().setColor(ImGuiCol.FrameBgHovered, 0.18f, 0.14f, 0.26f, 1.00f);
        ImGui.getStyle().setColor(ImGuiCol.FrameBgActive, 0.22f, 0.16f, 0.32f, 1.00f);
        ImGui.getStyle().setColor(ImGuiCol.SliderGrab, 0.45f, 0.35f, 0.65f, 1.00f);
        ImGui.getStyle().setColor(ImGuiCol.SliderGrabActive, 0.55f, 0.42f, 0.78f, 1.00f);
        ImGui.getStyle().setColor(ImGuiCol.CheckMark, 0.55f, 0.42f, 0.78f, 1.00f);
        ImGui.getStyle().setColor(ImGuiCol.Separator, 0.25f, 0.20f, 0.35f, 0.50f);
    }

    /**
     * Begin a new ImGui frame. Call before creating widgets.
     */
    public static void startFrame() {
        if (!initialized) return;
        IMGUI_GLFW.newFrame();
        ImGui.newFrame();
    }

    /**
     * End the ImGui frame and render. Call after all widgets are created.
     */
    public static void endFrame() {
        if (!initialized) return;
        ImGui.render();
        IMGUI_GL3.renderDrawData(ImGui.getDrawData());
    }

    /**
     * Clean up ImGui resources.
     */
    public static void dispose() {
        if (!initialized) return;
        IMGUI_GL3.shutdown();
        IMGUI_GLFW.shutdown();
        ImGui.destroyContext();
        initialized = false;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
