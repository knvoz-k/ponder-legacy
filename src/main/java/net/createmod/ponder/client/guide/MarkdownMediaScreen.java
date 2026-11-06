package net.createmod.ponder.client.guide;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import net.createmod.catnip.render.GlStateGuard;
import net.createmod.ponder.foundation.ui.AbstractPonderScreen;
import net.createmod.ponder.foundation.ui.PonderButton;
import net.createmod.ponder.guide.GuideMediaFiles;
import net.createmod.ponder.guide.MarkdownMedia;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.*;

/** A scrollable authoring form; opening or cancelling it never edits the Markdown buffer. */
@SideOnly(Side.CLIENT)
public final class MarkdownMediaScreen extends AbstractPonderScreen {
    private final MarkdownEditor parent;
    private final MarkdownMedia.Type type;
    private final Map<String, String> values = new LinkedHashMap<>();
    private final Map<String, GuiTextField> fields = new LinkedHashMap<>();
    private int left, formWidth, scroll, contentHeight;
    private PonderButton insertButton, sourceBrowse, posterBrowse;
    private CompletableFuture<String> importing;
    private String importTarget, error = "";

    MarkdownMediaScreen(MarkdownEditor parent, MarkdownMedia.Type type) {
        this.parent = parent; this.type = type;
        values.put("source", ""); values.put("title", "");
        if (type == MarkdownMedia.Type.VIDEO) values.put("poster", "");
        values.put("width", ""); values.put("height", "");
    }

    private static String tr(String key, Object... args) { return I18n.format("ponder.guide.insert_media." + key, args); }
    private int bodyBottom() { return height - 57; }

    @Override public void initGui() {
        stash(); fields.clear(); buttonList.clear(); Keyboard.enableRepeatEvents(true);
        left = Math.max(12, (width - 620) / 2); formWidth = width - left * 2;
        for (String key : values.keySet()) {
            GuiTextField field = new GuiTextField(fields.size(), fontRenderer, left, 0, formWidth, 20);
            field.setMaxStringLength(key.equals("width") || key.equals("height") ? 4 : key.equals("title") ? 512 : 2048);
            field.setText(values.get(key)); fields.put(key, field);
        }
        fields.get("source").setFocused(true);
        sourceBrowse = button(left + formWidth - 100, 0, 100, "browse", () -> browse("source"));
        if (fields.containsKey("poster")) posterBrowse = button(left + formWidth - 100, 0, 100, "browse", () -> browse("poster"));
        button(left, height - 31, 84, "cancel", () -> mc.displayGuiScreen(parent));
        insertButton = button(left + formWidth - 110, height - 31, 110, "insert", this::apply);
        layout();
    }

    private PonderButton button(int x, int y, int w, String key, Runnable action) {
        return addButton(new PonderButton(100 + buttonList.size(), x, y, w, 22, null).withLabel(tr(key)).withCallback(action));
    }

    private void stash() { fields.forEach((key, field) -> values.put(key, field.getText())); }

    private void layout() {
        contentHeight = type == MarkdownMedia.Type.VIDEO ? 170 : 128;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight - (bodyBottom() - 40))));
        int y = 52 - scroll;
        place("source", left, y, formWidth - 106); sourceBrowse.y = y - 1;
        place("title", left, y + 54, formWidth);
        if (fields.containsKey("poster")) { place("poster", left, y + 96, formWidth - 106); posterBrowse.y = y + 95; }
        int dims = y + (type == MarkdownMedia.Type.VIDEO ? 138 : 96), half = (formWidth - 12) / 2;
        place("width", left, dims, half); place("height", left + half + 12, dims, half);
        for (PonderButton browse : new PonderButton[] {sourceBrowse, posterBrowse}) if (browse != null) {
            browse.visible = browse.y >= 40 && browse.y + browse.height <= bodyBottom();
            browse.enabled = importing == null;
        }
        insertButton.enabled = importing == null;
    }

    private void place(String key, int x, int y, int w) {
        GuiTextField field = fields.get(key); field.x = x; field.y = y; field.width = w;
    }

    private void browse(String key) {
        stash();
        mc.displayGuiScreen(new MediaFileBrowserScreen(this, key.equals("poster") ? MarkdownMedia.Type.IMAGE : type, path -> importFile(path, key)));
    }

    private void importFile(Path path, String target) {
        importTarget = target; error = "";
        MarkdownMedia.Type mediaType = target.equals("poster") ? MarkdownMedia.Type.IMAGE : type;
        Path root = GuideMediaSources.root();
        importing = CompletableFuture.supplyAsync(() -> {
            try { return GuideMediaFiles.importFile(root, path, mediaType); }
            catch (IOException failure) { throw new CompletionException(failure); }
        });
        mc.displayGuiScreen(this);
    }

    private void apply() {
        if (importing != null) return;
        stash(); error = "";
        try {
            String source = values.get("source").trim(), poster = values.getOrDefault("poster", "").trim();
            if (source.isEmpty()) { error = tr("source_required"); return; }
            int w = dimension(values.get("width")), h = dimension(values.get("height"));
            if (source.startsWith("local:")) GuideMediaSources.local(source.substring(6));
            if (poster.startsWith("local:")) GuideMediaSources.local(poster.substring(6));
            parent.insertMedia(type, source, values.get("title"), poster, w, h);
            mc.displayGuiScreen(parent);
        } catch (IOException | RuntimeException failure) {
            error = tr("invalid", failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage());
        }
    }

    private int dimension(String text) {
        if (text.isBlank()) return 0;
        try { int value = Integer.parseInt(text.trim()); if (value >= 0 && value <= 2048) return value; }
        catch (NumberFormatException ignored) {}
        throw new IllegalArgumentException(tr("dimension_error"));
    }

    @Override public void updateScreen() {
        fields.values().forEach(GuiTextField::updateCursorCounter);
        if (importing != null && importing.isDone()) {
            try { String source = importing.join(); values.put(importTarget, source); fields.get(importTarget).setText(source); }
            catch (CompletionException failure) { error = tr("invalid", failure.getCause().getMessage()); }
            importing = null; layout();
        }
    }

    @Override protected void renderWindow(int mx, int my, float partial) {
        fontRenderer.drawString(tr(type == MarkdownMedia.Type.IMAGE ? "image" : "video"), left, 13, 0xffd6e0e8);
        layout();
        try (GlStateGuard ignored = GlStateGuard.capture()) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(0, mc.displayHeight - bodyBottom() * mc.displayHeight / height,
                mc.displayWidth, Math.max(0, (bodyBottom() - 40) * mc.displayHeight / height));
            for (var entry : fields.entrySet()) {
                GuiTextField field = entry.getValue();
                fontRenderer.drawString(tr(entry.getKey()), field.x, field.y - 12, 0xffa9bdc9);
                field.drawTextBox();
            }
            fontRenderer.drawString(fontRenderer.trimStringToWidth(tr("source_hint"), formWidth), left, 78 - scroll, 0xff819aaa);
        }
        if (contentHeight > bodyBottom() - 40) {
            int track = bodyBottom() - 40, thumb = Math.max(16, track * track / contentHeight);
            int y = 40 + scroll * (track - thumb) / Math.max(1, contentHeight - track);
            Gui.drawRect(width - left + 5, 40, width - left + 7, bodyBottom(), 0xff293c49);
            Gui.drawRect(width - left + 5, y, width - left + 7, y + thumb, 0xff8bbbc2);
        }
        String note = importing != null ? tr("importing") : !error.isEmpty() ? error
            : type == MarkdownMedia.Type.VIDEO && !Loader.isModLoaded("media_api") ? I18n.format("ponder.guide.media_missing") : tr("hint");
        fontRenderer.drawString(fontRenderer.trimStringToWidth(note, formWidth), left, height - 47, error.isEmpty() ? 0xffa4bdc8 : 0xffff9c9c);
    }

    @Override protected void renderWindowForeground(int mx, int my, float partial) {
        if (!error.isEmpty() && my >= height - 50 && my < height - 34)
            drawHoveringText(fontRenderer.listFormattedStringToWidth(error, Math.max(80, Math.min(360, width - 32))), mx, my);
    }

    @Override protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (my >= 40 && my < bodyBottom()) for (GuiTextField field : fields.values()) field.mouseClicked(mx, my, button);
        super.mouseClicked(mx, my, button);
    }

    @Override protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(parent); return; }
        if ((key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) && isCtrlKeyDown()) { apply(); return; }
        if (key == Keyboard.KEY_TAB) {
            List<GuiTextField> inputs = new ArrayList<>(fields.values()); int focused = -1;
            for (int i = 0; i < inputs.size(); i++) if (inputs.get(i).isFocused()) focused = i;
            inputs.forEach(field -> field.setFocused(false));
            GuiTextField next = inputs.get(Math.floorMod(focused + (isShiftKeyDown() ? -1 : 1), inputs.size())); next.setFocused(true);
            if (next.y < 52) scroll -= 52 - next.y;
            if (next.y + 22 > bodyBottom()) scroll += next.y + 22 - bodyBottom();
            layout(); return;
        }
        for (GuiTextField field : fields.values()) if (field.isFocused()) field.textboxKeyTyped(c, key);
        error = "";
    }

    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput(); int wheel = Mouse.getEventDWheel();
        if (wheel != 0) { scroll -= Integer.signum(wheel) * 24; layout(); }
    }
    @Override public void onGuiClosed() { stash(); Keyboard.enableRepeatEvents(false); }
}
