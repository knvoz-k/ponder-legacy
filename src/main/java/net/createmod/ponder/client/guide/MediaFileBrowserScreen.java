package net.createmod.ponder.client.guide;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import net.createmod.ponder.foundation.ui.AbstractPonderScreen;
import net.createmod.ponder.foundation.ui.PonderButton;
import net.createmod.ponder.guide.GuideMediaFiles;
import net.createmod.ponder.guide.MarkdownMedia;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.*;

/** Game-native picker, including external files which the media form imports into the pack. */
@SideOnly(Side.CLIENT)
final class MediaFileBrowserScreen extends AbstractPonderScreen {
    private final GuiScreen parent;
    private final MarkdownMedia.Type type;
    private final Consumer<Path> accept;
    private final List<Path> entries = new ArrayList<>();
    private Path directory = GuideMediaSources.root(), selected;
    private GuiTextField path;
    private PonderButton choose;
    private int scroll;
    private String error = "", pathText;

    MediaFileBrowserScreen(GuiScreen parent, MarkdownMedia.Type type, Consumer<Path> accept) {
        this.parent = parent; this.type = type; this.accept = accept;
    }

    private static String tr(String key) { return I18n.format("ponder.guide.insert_media." + key); }
    private int rows() { return Math.max(1, (height - 145) / 24); }

    @Override public void initGui() {
        buttonList.clear(); Keyboard.enableRepeatEvents(true);
        path = new GuiTextField(0, fontRenderer, 12, 42, width - 94, 20);
        path.setMaxStringLength(4096); path.setText(pathText == null ? directory.toString() : pathText);
        button(width - 76, 41, 64, "open", this::openPath);
        button(12, 70, 62, "up", () -> { if (directory.getParent() != null) navigate(directory.getParent()); });
        button(80, 70, 90, "media_folder", () -> navigate(GuideMediaSources.root()));
        button(176, 70, 112, "game_folder", () -> navigate(mc.gameDir.toPath()));
        button(12, height - 31, 84, "cancel", () -> mc.displayGuiScreen(parent));
        choose = button(width - 122, height - 31, 110, "select_file", this::select);
        refresh();
    }

    private PonderButton button(int x, int y, int w, String key, Runnable action) {
        return addButton(new PonderButton(100 + buttonList.size(), x, y, w, 22, null).withLabel(tr(key)).withCallback(action));
    }

    private void refresh() {
        entries.clear(); error = "";
        try {
            if (directory.equals(GuideMediaSources.root())) Files.createDirectories(directory);
            try (var files = Files.list(directory)) {
                files.filter(file -> Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS)
                    || Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) && GuideMediaFiles.accepts(file, type))
                    .sorted(Comparator.<Path, Boolean>comparing(file -> !Files.isDirectory(file))
                        .thenComparing(file -> file.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .limit(1000).forEach(entries::add);
            }
        } catch (IOException | SecurityException failure) { error = failure.getMessage(); }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, entries.size() - rows())));
        choose.enabled = selected != null && Files.isRegularFile(selected, LinkOption.NOFOLLOW_LINKS);
    }

    private void navigate(Path folder) {
        directory = folder.toAbsolutePath().normalize(); selected = null; scroll = 0;
        path.setText(directory.toString()); pathText = path.getText(); refresh();
    }

    private void openPath() {
        try {
            Path entered = Paths.get(path.getText());
            if (!entered.isAbsolute()) entered = directory.resolve(entered);
            entered = entered.normalize();
            if (Files.isDirectory(entered, LinkOption.NOFOLLOW_LINKS)) navigate(entered);
            else if (Files.isRegularFile(entered, LinkOption.NOFOLLOW_LINKS) && GuideMediaFiles.accepts(entered, type)) {
                navigate(entered.getParent()); selected = entered; choose.enabled = true;
                scroll = Math.max(0, Math.min(entries.indexOf(entered), Math.max(0, entries.size() - rows())));
            } else error = tr("file_missing");
        } catch (InvalidPathException failure) { error = tr("file_missing"); }
    }

    private void select() {
        if (selected != null && Files.isRegularFile(selected, LinkOption.NOFOLLOW_LINKS)) accept.accept(selected);
    }

    @Override protected void renderWindow(int mx, int my, float partial) {
        fontRenderer.drawString(tr(type == MarkdownMedia.Type.IMAGE ? "choose_image" : "choose_video"), 12, 13, 0xffd6e0e8);
        path.drawTextBox();
        for (int i = scroll; i < Math.min(entries.size(), scroll + rows()); i++) {
            Path file = entries.get(i); int y = 100 + (i - scroll) * 24;
            boolean folder = Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS);
            Gui.drawRect(12, y, width - 12, y + 22, file.equals(selected) ? 0xff31525a : my >= y && my < y + 22 ? 0xff30414f : 0xff202b34);
            String name = (folder ? "+ " : "  ") + file.getFileName();
            fontRenderer.drawString(fontRenderer.trimStringToWidth(name, width - 42), 18, y + 7, folder ? 0xffa9ddd4 : 0xffd6e0e8);
        }
        if (entries.isEmpty()) fontRenderer.drawSplitString(tr("empty_folder"), 18, 105, width - 36, 0xff819aaa);
        if (entries.size() > rows()) {
            int track = rows() * 24, thumb = Math.max(12, track * rows() / entries.size());
            int y = 100 + scroll * (track - thumb) / (entries.size() - rows());
            Gui.drawRect(width - 8, 100, width - 6, 100 + track, 0xff293c49);
            Gui.drawRect(width - 8, y, width - 6, y + thumb, 0xff8bbbc2);
        }
        String note = !error.isEmpty() ? error : tr("file_hint");
        fontRenderer.drawString(fontRenderer.trimStringToWidth(note, width - 24), 12, height - 47, error.isEmpty() ? 0xffa4bdc8 : 0xffff9c9c);
    }

    @Override protected void mouseClicked(int mx, int my, int button) throws IOException {
        path.mouseClicked(mx, my, button);
        if (button == 0 && mx >= 12 && mx < width - 12 && my >= 100 && my < 100 + rows() * 24) {
            int index = scroll + (my - 100) / 24;
            if (index < entries.size()) {
                Path file = entries.get(index);
                if (Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS)) navigate(file);
                else { selected = file; choose.enabled = true; }
            }
            return;
        }
        super.mouseClicked(mx, my, button);
    }

    @Override protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(parent); return; }
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) { if (path.isFocused()) openPath(); else select(); return; }
        path.textboxKeyTyped(c, key);
    }
    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput(); int wheel = Mouse.getEventDWheel();
        if (wheel != 0) scroll = Math.max(0, Math.min(scroll - Integer.signum(wheel) * 3, Math.max(0, entries.size() - rows())));
    }
    @Override public void updateScreen() { path.updateCursorCounter(); }
    @Override public void onGuiClosed() { pathText = path.getText(); Keyboard.enableRepeatEvents(false); }
}
