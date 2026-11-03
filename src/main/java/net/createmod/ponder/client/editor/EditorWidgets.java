package net.createmod.ponder.client.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.renderer.GlStateManager;

final class EditorWidgets {
    static final int BACKGROUND = 0xff111821, PANEL = 0xff1a2431, LINE = 0xff304255;
    static final int TEXT = 0xffe4edf5, MUTED = 0xff9baec2, ACCENT = 0xff72e3bd;
    private EditorWidgets() {}
    static String tr(String key, Object... args) { return I18n.format("ponder.editor." + key, args); }

    static final class Button extends GuiButton {
        private final Runnable callback;
        Button(int id, int x, int y, int width, String text, Runnable callback) {
            super(id, x, y, width, 20, text); this.callback = callback;
        }
        void activate() { if (enabled) callback.run(); }
        @Override public void drawButton(Minecraft mc, int mouseX, int mouseY, float partial) {
            if (!visible) return;
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            GlStateManager.disableLighting(); GlStateManager.disableDepth();
            drawRect(x, y, x + width, y + height, enabled && hovered ? 0xff355648 : PANEL);
            drawRect(x, y + height - 1, x + width, y + height, enabled ? LINE : 0xff27313d);
            drawCenteredString(mc.fontRenderer, mc.fontRenderer.trimStringToWidth(displayString, Math.max(1, width - 6)), x + width / 2, y + 6,
                enabled ? (hovered ? ACCENT : TEXT) : 0xff5c6a79);
            GlStateManager.color(1, 1, 1, 1);
        }
    }
}
