package net.createmod.ponder.foundation.ui;

import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.createmod.ponder.api.editor.SceneDocument;
import net.createmod.ponder.api.editor.ScenePackIO;
import net.createmod.ponder.client.editor.EditorWorkspace;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.PonderTooltipHandler;
import net.createmod.ponder.script.PonderJsonLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

/** Real client input and framebuffer checks; only included in the development harness. */
public final class PonderViewerSmoke {
    private final Minecraft mc;
    private final BiConsumer<Boolean, String> check;
    private final Consumer<String> screenshot;
    private PonderUI viewer;
    private List<PonderScene> scenes;
    private int phase, wait;
    private RenderTooltipEvent.PostText tooltip;

    public PonderViewerSmoke(Minecraft mc, BiConsumer<Boolean, String> check, Consumer<String> screenshot) {
        this.mc = mc; this.check = check; this.screenshot = screenshot;
        MinecraftForge.EVENT_BUS.register(this);
    }

    public boolean advance() throws Exception {
        if (wait-- > 0) return false;
        switch (phase++) {
            case 0:
                SceneDocument pack = SceneDocument.create("smoke:menu_01", "minecraft:compass", "基础搭建 / Building", "ponder:demo/basics");
                pack.edit(json -> {
                    String[] titles = {"输入与输出 / Input and output", "自动化联动 / Automation", "移动动画 / Moving blocks", "旋转动画 / Rotation"};
                    for (int i = 2; i <= 12; i++) {
                        JsonObject scene = SceneDocument.create(String.format("smoke:menu_%02d", i), "minecraft:compass",
                            titles[(i - 2) % titles.length] + " " + i, "ponder:demo/basics").scene();
                        json.getAsJsonArray("scenes").add(scene);
                    }
                });
                Path path = ScenePackIO.resolveId(EditorWorkspace.packs(), "smoke:menu_pack", ".ponder.json");
                ScenePackIO.save(path, pack);
                check.accept(PonderJsonLoader.reload().errors == 0, "multi-scene JSON pack reloads");
                resize(1280, 720); wait = 10; break;
            case 1:
                viewer = PonderUI.of(new ItemStack(Items.COMPASS));
                scenes = (List<PonderScene>) get(viewer, "scenes");
                check.accept(scenes.size() == 12 && scenes.stream().allMatch(s -> s.getComponent().toString().equals("minecraft:compass")),
                    "one item resolves all twelve registered scenes");
                mc.displayGuiScreen(viewer); wait = 5; break;
            case 2:
                check.accept(!button(2).enabled && button(5).enabled, "first scene disables only previous navigation");
                viewer.keyTyped(' ', Keyboard.KEY_SPACE);
                screenshot.accept("viewer-toolbar.png"); break;
            case 3:
                GuiButton anchor = button(9);
                menu().updateHover(anchor.x + 5, anchor.y + 5, 0);
                check.accept(menu().isOpen(), "hovering scene counter opens the floating menu");
                menu().updateHover(anchor.x + 5, anchor.y + anchor.height + 2, 0);
                check.accept(menu().isOpen(), "pointer can cross from counter into menu");
                viewer.mouseClicked(anchor.x + 5, anchor.y + 5, 0);
                screenshot.accept("scene-menu-zh.png"); break;
            case 4:
                int x = (Integer)get(menu(), "x") + 45;
                int y = (Integer)get(menu(), "y") + 25 + 28 + 10;
                viewer.mouseClicked(x, y, 0);
                check.accept(viewer.getActiveScene() == scenes.get(1), "clicking a menu row opens the second scene");
                check.accept(!menu().isOpen() && !(Boolean)get(viewer, "dragging"), "menu selection closes without rotating the preview");
                check.accept(button(2).enabled && button(5).enabled, "middle scene enables both navigation directions");
                viewer.keyTyped('\t', Keyboard.KEY_TAB);
                viewer.keyTyped('\0', Keyboard.KEY_END);
                check.accept(menu().highlighted() == 11 && (Integer)get(menu(), "firstVisible") > 0,
                    "End scrolls the long list to the final scene");
                screenshot.accept("scene-menu-last.png"); break;
            case 5:
                viewer.keyTyped('\r', Keyboard.KEY_RETURN);
                check.accept(viewer.getActiveScene() == scenes.get(11) && !button(5).enabled && button(2).enabled,
                    "Enter opens final scene and disables next navigation");
                viewer.keyTyped('\t', Keyboard.KEY_TAB);
                viewer.keyTyped('\0', Keyboard.KEY_HOME);
                viewer.keyTyped('\0', Keyboard.KEY_UP);
                check.accept(menu().highlighted() == 0 && (Integer)get(menu(), "firstVisible") == 0,
                    "Home and Up clamp to first scene");
                float zoom = (Float)get(viewer, "zoom");
                Method scroll = PonderUI.class.getDeclaredMethod("scroll", int.class, int.class, int.class);
                scroll.setAccessible(true);
                for (int i = 0; i < 30; i++) scroll.invoke(viewer, -120, (Integer)get(menu(), "x") + 45, (Integer)get(menu(), "y") + 35);
                check.accept((Integer)get(menu(), "firstVisible") == 12 - menu().pageSize(), "mouse wheel clamps at final menu page");
                check.accept((Float)get(viewer, "zoom") == zoom, "scrolling floating menu does not zoom preview");
                viewer.keyTyped('\0', Keyboard.KEY_ESCAPE);
                check.accept(mc.currentScreen == viewer && !menu().isOpen(), "Escape closes menu and keeps viewer open");
                GuiButton a = button(9);
                menu().updateHover(a.x + 4, a.y + 4, 11);
                check.accept(!menu().isOpen(), "closed menu stays closed until pointer leaves counter");
                viewer.keyTyped('\t', Keyboard.KEY_TAB);
                viewer.mouseClicked(12, viewer.height / 2, 0);
                check.accept(!menu().isOpen() && !(Boolean)get(viewer, "dragging"), "outside click dismisses without camera drag");
                resize(854, 480); wait = 10; break;
            case 6:
                check.accept(mc.displayWidth == 854 && mc.displayHeight == 480 && viewer.height == 240, "small-window framebuffer and GUI use requested dimensions");
                viewer.keyTyped('\t', Keyboard.KEY_TAB);
                check.accept((Integer)get(menu(), "x") >= 0 && (Integer)get(menu(), "y") + (Integer)get(menu(), "height") < viewer.height - 38,
                    "small-window menu stays within scene viewport");
                screenshot.accept("scene-menu-small.png"); break;
            case 7:
                mc.gameSettings.language = "en_us";
                mc.getLanguageManager().setCurrentLanguage(mc.getLanguageManager().getLanguages().stream()
                    .filter(l -> "en_us".equals(l.getLanguageCode())).findFirst().orElseThrow());
                mc.refreshResources();
                resize(1280, 720); wait = 10; break;
            case 8:
                viewer.keyTyped('\t', Keyboard.KEY_TAB);
                screenshot.accept("scene-menu-en.png"); break;
            case 9:
                viewer = new PonderUI(Collections.singletonList(scenes.get(0)));
                mc.displayGuiScreen(viewer);
                check.accept(get(viewer, "sceneMenu") == null && !button(2).enabled && !button(5).enabled,
                    "single-scene item has no floating menu and disables both scene arrows");
                mc.displayGuiScreen(new TooltipScreen()); wait = 5; break;
            case 10:
                screenshot.accept("tooltip-progress.png"); break;
            case 11:
                resize(854, 480); wait = 10; break;
            case 12:
                screenshot.accept("tooltip-progress-wrapped.png"); break;
            default: MinecraftForge.EVENT_BUS.unregister(this); return true;
        }
        return false;
    }

    @SubscribeEvent public void tooltip(RenderTooltipEvent.PostText event) { tooltip = event; }

    public void verifyFrame(String name) throws Exception {
        if (!name.equals("viewer-toolbar.png") && !name.startsWith("tooltip-progress")) return;
        BufferedImage image = ScreenShotHelper.createScreenshot(mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        int scale = new net.minecraft.client.gui.ScaledResolution(mc).getScaleFactor();
        if (name.equals("viewer-toolbar.png")) {
            for (int id : new int[]{4, 5}) {
                GuiButton button = button(id);
                int bright = 0;
                for (int x = (button.x + 3) * scale; x < (button.x + button.width - 3) * scale; x++)
                    for (int y = (button.y + 3) * scale; y < (button.y + button.height - 3) * scale; y++)
                        if ((image.getRGB(x, y) & 0xffffff) == 0xe3e8ed) bright++;
                check.accept(bright >= 40, "framebuffer contains visible " + (id == 4 ? "play" : "next scene") + " icon");
            }
        } else {
            check.accept(tooltip != null, "native tooltip post-text event fires");
            int row = -1;
            for (int i = 0; i < tooltip.getLines().size(); i++)
                if (tooltip.getLines().get(i).endsWith("\u00a7r\u00a7r ")) { row = i; break; }
            check.accept(row >= 0, "tooltip keeps reserved progress row after wrapping");
            if (name.equals("tooltip-progress-wrapped.png"))
                check.accept(row >= 3 && tooltip.getWidth() < 259, "small-window native tooltip actually wraps the long title");
            int x = tooltip.getX() * scale;
            int y = (tooltip.getY() + 2 + row * 10 + 5) * scale;
            int width = tooltip.getWidth() * scale;
            int track = 0, filled = 0;
            for (int i = x; i < x + width; i++) {
                int rgb = image.getRGB(i, y) & 0xffffff;
                if (rgb == 0x526373 || rgb == 0x1c2630 || rgb == 0x9fd6d2) track++;
                if (rgb == 0x9fd6d2) filled++;
            }
            check.accept(track >= width * .98, "progress track spans native tooltip width (" + tooltip.getWidth() + " GUI pixels; track="
                + track + ", filled=" + filled + ", x=" + x + ", y=" + y + ", scale=" + scale + ")");
            check.accept(Math.abs(filled / (double)width - .6) < .03, "progress fill occupies sixty percent of track");
        }
    }

    private PonderSceneMenu menu() throws Exception { return (PonderSceneMenu)get(viewer, "sceneMenu"); }
    private void resize(int width, int height) throws Exception {
        Display.setDisplayMode(new DisplayMode(width, height));
        mc.resize(width, height);
    }
    private GuiButton button(int id) throws Exception {
        List<GuiButton> buttons = (List<GuiButton>)ReflectionHelper.findField(GuiScreen.class, "buttonList", "field_146292_n").get(viewer);
        return buttons.stream().filter(b -> b.id == id).findFirst().orElseThrow();
    }
    private static Object get(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }

    private final class TooltipScreen extends GuiScreen {
        private final ItemStack subject = new ItemStack(Items.COMPASS).setStackDisplayName("Ponder multi-scene example with a longer item name");
        @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();
            try {
                Field progress = PonderTooltipHandler.class.getDeclaredField("progress"); progress.setAccessible(true);
                // Native tooltip gathering updates tracking; fix progress only for the reference screenshot.
                subject.getTooltip(mc.player, net.minecraft.client.util.ITooltipFlag.TooltipFlags.NORMAL);
                progress.setFloat(null, .6f);
                renderToolTip(subject, width / 2, height / 2);
            } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
        }
    }
}
