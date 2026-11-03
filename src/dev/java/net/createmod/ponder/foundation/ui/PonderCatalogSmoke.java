package net.createmod.ponder.foundation.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.ponder.api.editor.SceneDocument;
import net.createmod.ponder.api.editor.ScenePackIO;
import net.createmod.ponder.client.editor.EditorWorkspace;
import net.createmod.ponder.script.PonderJsonLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

/** Installation-jar tests using registered items, JSON themes, real screen input and the framebuffer. */
@Mod.EventBusSubscriber(modid = "ponder_editor_harness", value = Side.CLIENT)
public final class PonderCatalogSmoke {
    @SubscribeEvent public static void registerFixture(RegistryEvent.Register<Item> event) {
        if (Boolean.getBoolean("ponder.dev.smoke")) event.getRegistry().register(new Item() {
            @Override public String getItemStackDisplayName(ItemStack stack) { return "Catalog fixture"; }
        }.setRegistryName("ponder_editor_harness", "catalog_fixture"));
    }

    @SubscribeEvent public static void registerFixtureModel(ModelRegistryEvent event) {
        if (Boolean.getBoolean("ponder.dev.smoke")) ModelLoader.setCustomModelResourceLocation(
            Item.REGISTRY.getObject(new ResourceLocation("ponder_editor_harness:catalog_fixture")), 0,
            new ModelResourceLocation("minecraft:stick", "inventory"));
    }

    private final Minecraft mc;
    private final BiConsumer<Boolean, String> check;
    private final Consumer<String> screenshot;
    private final List<ResourceLocation> fixtureItems = new ArrayList<>();
    private PonderIndexScreen index;
    private PonderCatalogScreen page;
    private ResourceLocation excluded;
    private int phase, wait;

    public PonderCatalogSmoke(Minecraft mc, BiConsumer<Boolean, String> check, Consumer<String> screenshot) {
        this.mc = mc; this.check = check; this.screenshot = screenshot;
    }

    public boolean advance() throws Exception {
        if (wait-- > 0) return false;
        switch (phase++) {
            case 0:
                fixture();
                language("zh_cn"); resize(1280, 720);
                index = new PonderIndexScreen(); page = index;
                mc.displayGuiScreen(index); wait = 8; break;
            case 1:
                check.accept(index.entries.size() > 200 && index.grid.canScroll(), "large item catalog exposes a scrollable viewport");
                check.accept(index.entries.stream().filter(e -> e.id.toString().equals("minecraft:compass")).count() == 1,
                    "twelve scenes produce one catalog entry");
                check.accept(index.entries.stream().noneMatch(e -> e.id.equals(excluded)), "catalog respects JSON index exclusions");
                bounded(page);
                capture("catalog-mods-zh.png"); break;
            case 2:
                index.scrollAt(-120, index.grid.left + 8, index.grid.top + 8);
                check.accept(index.grid.firstRow() == 1, "wheel advances item viewport by one row");
                index.scrollAt(-120, 40, index.height - 20);
                check.accept(index.grid.firstRow() == 1, "wheel over footer does not scroll catalog");
                index.keyTyped('\0', Keyboard.KEY_END);
                lastVisible(page); bounded(page);
                capture("catalog-last.png"); break;
            case 3:
                dragGrid(index, false);
                check.accept(index.grid.firstRow() == 0, "dragging item scrollbar reaches the first row");
                dragGrid(index, true);
                check.accept(index.grid.firstRow() == index.grid.maxFirstRow(), "dragging item scrollbar reaches the final row");
                chooseCategory("ponder_editor_harness");
                check.accept(index.entries.size() == 1 && index.entries.get(0).id.getNamespace().equals("ponder_editor_harness"),
                    "mod category filters by the component owner namespace");
                clickButton(index, 4);
                chooseCategory("smoke:category_00");
                check.accept(index.entries.size() == fixtureItems.size() - 1, "theme category resolves all assigned items except exclusions");
                check.accept(index.grid.firstRow() == 0, "category change resets item scroll");
                capture("catalog-themes-zh.png"); break;
            case 4:
                PonderScrollPane side = (PonderScrollPane)get(index, "categoriesPane");
                for (int i = 0; i < 100; i++) index.scrollAt(-120, side.left + 8, side.top + 8);
                check.accept(side.firstRow() == side.maxFirstRow() && side.canScroll(), "many theme categories scroll to the last category independently");
                check.accept(index.grid.firstRow() == 0, "category wheel leaves item viewport unchanged");
                chooseCategory("#uncategorized");
                check.accept(index.entries.stream().anyMatch(e -> e.id.toString().equals("minecraft:compass")),
                    "unassigned item appears in Uncategorized");
                resize(854, 480); wait = 8; break;
            case 5:
                PonderScrollPane resizedSide = (PonderScrollPane)get(index, "categoriesPane");
                check.accept(resizedSide.firstRow() > 0 && resizedSide.firstRow() <= resizedSide.maxFirstRow(),
                    "resize preserves a valid category scroll position");
                index = new PonderIndexScreen(); page = index; mc.displayGuiScreen(index);
                index.keyTyped('\0', Keyboard.KEY_END);
                lastVisible(page); bounded(page);
                capture("catalog-small.png"); break;
            case 6:
                typeQuery("compass");
                check.accept(index.entries.size() == 1 && index.entries.get(0).id.toString().equals("minecraft:compass"),
                    "ID search filters the real catalog and resets its scroll");
                check.accept(index.grid.firstRow() == 0 && index.entries.get(0).tooltip.get(2).contains("12"),
                    "multi-scene catalog tooltip reports twelve scenes");
                capture("catalog-search.png"); break;
            case 7:
                PonderButton entry = index.visibleEntries.get(0);
                index.mouseClicked(entry.x + 5, entry.y + 5, 0); wait = 5; break;
            case 8:
                check.accept(mc.currentScreen instanceof PonderUI && ((PonderUI)mc.currentScreen).getActiveScene().getComponent()
                    .toString().equals("minecraft:compass"), "clicking filtered catalog entry opens the correct item viewer");
                check.accept(ScreenOpener.openPreviousScreen(), "viewer can navigate back to catalog"); wait = 5; break;
            case 9:
                check.accept(mc.currentScreen == index && search().getText().equals("compass"), "returning to catalog preserves the query");
                typeQuery("missing_catalog_item");
                check.accept(index.entries.isEmpty() && index.visibleEntries.isEmpty(), "unmatched search leaves no hidden clickable entries");
                typeQuery("");
                check.accept(mc.currentScreen == index && index.entries.size() > 200, "Backspace edits search without navigating away");
                resize(1280, 720);
                page = new PonderTagIndexScreen(); mc.displayGuiScreen(page); wait = 8; break;
            case 10:
                check.accept(page.entries.size() >= 40 && page.grid.canScroll(), "theme index scrolls large registered theme collections");
                page.keyTyped('\0', Keyboard.KEY_END); lastVisible(page); bounded(page);
                capture("catalog-theme-index.png"); break;
            case 11:
                page.keyTyped('\0', Keyboard.KEY_HOME);
                PonderButton tag = page.visibleEntries.stream().filter(b -> page.entries.get(b.id - 100).id.toString().equals("smoke:category_00"))
                    .findFirst().orElseThrow();
                page.mouseClicked(tag.x + 6, tag.y + 6, 0); wait = 5; break;
            case 12:
                check.accept(mc.currentScreen instanceof PonderTagScreen, "theme icon opens the theme contents");
                page = (PonderCatalogScreen)mc.currentScreen;
                check.accept(page.entries.size() == fixtureItems.size() - 1 && page.grid.top >= 90,
                    "long theme description reserves space above a large item list");
                page.keyTyped('\0', Keyboard.KEY_END); lastVisible(page); bounded(page);
                capture("catalog-theme-items.png"); break;
            case 13:
                language("en_us"); resize(854, 480);
                index = new PonderIndexScreen(); page = index; mc.displayGuiScreen(index); wait = 8; break;
            case 14:
                clickButton(index, 4); chooseCategory("smoke:category_00");
                bounded(page); capture("catalog-en-small.png"); break;
            default: return true;
        }
        return false;
    }

    public void verifyFrame(String name) {
        if (!name.startsWith("catalog-")) return;
        BufferedImage image = ScreenShotHelper.createScreenshot(mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        int scale = new net.minecraft.client.gui.ScaledResolution(mc).getScaleFactor();
        int y = (page.height - 35) * scale;
        int background = 0;
        for (int x = 0; x < image.getWidth(); x++) if ((image.getRGB(x, y) & 0xffffff) == 0x171b20) background++;
        check.accept(background == image.getWidth(), name + " framebuffer keeps entries clear of the fixed footer");
    }

    private void bounded(PonderCatalogScreen screen) {
        check.accept(screen.visibleEntries.stream().allMatch(b -> b.y >= screen.grid.top && b.y + b.height <= screen.grid.bottom
            && b.x >= screen.grid.left && b.x + b.width < screen.grid.right - 10),
            screen.getClass().getSimpleName() + " renders and hit-tests only entries inside its viewport");
    }

    private void lastVisible(PonderCatalogScreen screen) {
        check.accept(screen.grid.firstRow() == screen.grid.maxFirstRow() && screen.visibleEntries.stream()
            .anyMatch(b -> b.id == 100 + screen.entries.size() - 1), screen.getClass().getSimpleName() + " can reach the final entry");
    }

    private void dragGrid(PonderCatalogScreen screen, boolean end) throws Exception {
        int x = screen.grid.right - 5;
        screen.mouseClicked(x, screen.grid.thumbTop() + 2, 0);
        screen.mouseClickMove(x, end ? screen.grid.bottom + 50 : screen.grid.top - 50, 0, 50);
        screen.mouseReleased(x, 0, 0);
    }

    private void chooseCategory(String key) throws Exception {
        List<?> categories = (List<?>)get(index, "categories");
        int selected = -1;
        for (int i = 0; i < categories.size(); i++) if (key.equals(get(categories.get(i), "key"))) { selected = i; break; }
        if (selected < 0) throw new IllegalStateException("Missing category " + key);
        PonderScrollPane side = (PonderScrollPane)get(index, "categoriesPane");
        side.setFirstRow(selected);
        index.mouseClicked(side.left + 8, side.top + (selected - side.firstRow()) * 22 + 8, 0);
    }

    private GuiTextField search() throws Exception { return (GuiTextField)get(index, "search"); }
    private void typeQuery(String query) throws Exception {
        GuiTextField field = search();
        index.mouseClicked(field.x + 5, field.y + 5, 0);
        field.setCursorPositionEnd();
        while (!field.getText().isEmpty()) index.keyTyped('\b', Keyboard.KEY_BACK);
        for (char c : query.toCharArray()) index.keyTyped(c, Keyboard.KEY_NONE);
    }

    private void clickButton(GuiScreen screen, int id) throws Exception {
        List<GuiButton> buttons = (List<GuiButton>)ReflectionHelper.findField(GuiScreen.class, "buttonList", "field_146292_n").get(screen);
        GuiButton button = buttons.stream().filter(b -> b.id == id).findFirst().orElseThrow();
        index.mouseClicked(button.x + 4, button.y + 4, 0);
    }

    private void fixture() throws Exception {
        for (Item item : Item.REGISTRY) {
            if (item == Items.AIR || item == Items.COMPASS || fixtureItems.size() >= 240) continue;
            fixtureItems.add(item.getRegistryName());
        }
        fixtureItems.add(new ResourceLocation("ponder_editor_harness:catalog_fixture"));
        excluded = fixtureItems.get(239);
        SceneDocument pack = SceneDocument.create("smoke:catalog_0", fixtureItems.get(0).toString(), "Catalog entry", "ponder:demo/basics");
        pack.edit(json -> {
            for (int i = 1; i < fixtureItems.size(); i++) json.getAsJsonArray("scenes").add(SceneDocument.create("smoke:catalog_" + i,
                fixtureItems.get(i).toString(), "Catalog entry " + i, "ponder:demo/basics").scene());
            JsonArray tags = new JsonArray();
            for (int i = 0; i < 42; i++) {
                JsonObject tag = new JsonObject(); tag.addProperty("id", String.format("smoke:category_%02d", i));
                tag.addProperty("title", String.format("%02d 分类 / Theme", i));
                tag.addProperty("icon", fixtureItems.get(i).toString());
                tag.addProperty("description", "按步骤查看场景中的输入、输出和搭建过程。 Follow the steps to build and automate this structure. ".repeat(5));
                JsonArray components = new JsonArray();
                if (i == 0) for (ResourceLocation component : fixtureItems) components.add(component.toString());
                else components.add(fixtureItems.get(i).toString());
                tag.add("components", components); tags.add(tag);
            }
            json.add("tags", tags);
            JsonArray exclusions = new JsonArray(); exclusions.add(excluded.toString()); json.add("indexExclusions", exclusions);
        });
        Path path = ScenePackIO.resolveId(EditorWorkspace.packs(), "smoke:catalog_pack", ".ponder.json");
        ScenePackIO.save(path, pack);
        check.accept(PonderJsonLoader.reload().errors == 0, "large catalog fixture reloads through the public JSON pipeline");
    }

    private void language(String code) {
        mc.gameSettings.language = code;
        mc.getLanguageManager().setCurrentLanguage(mc.getLanguageManager().getLanguages().stream()
            .filter(l -> code.equals(l.getLanguageCode())).findFirst().orElseThrow());
        mc.refreshResources();
    }
    private void capture(String name) { Mouse.setCursorPosition(2, 2); screenshot.accept(name); }
    private void resize(int width, int height) throws Exception { Display.setDisplayMode(new DisplayMode(width, height)); mc.resize(width, height); }
    private static Object get(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
}
