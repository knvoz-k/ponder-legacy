package net.createmod.ponder.verification.revoui;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.compat.revoui.RevoTooltipLayout;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderTooltipHandler;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

/** Installation-jar renderer checks; never included in Ponder's release jar. */
@Mod(modid = "ponder_revo_harness", name = "Ponder Revo UI verification", version = "1",
    dependencies = "required-after:ponder-legacy;after:neofontrender_ui_enhancements;after:ae2",
    clientSideOnly = true, acceptableRemoteVersions = "*")
public final class RevoTooltipSmoke {
    private static final ResourceLocation CABLE = new ResourceLocation("ae2:gray_smart_cable");
    private final Minecraft mc = Minecraft.getMinecraft();
    private final JsonArray checks = new JsonArray(), frames = new JsonArray();
    private final boolean modern = "present".equals(System.getProperty("ponder.revo.smoke"));
    private Path output;
    private int phase, ticks, wait, exitAt = Integer.MAX_VALUE;
    private TooltipScreen screen;
    private RenderTooltipEvent.PostText tooltip;
    private RevoTooltipLayout.Bounds bounds;
    private String shot;

    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event) {
        PonderIndex.addPlugin(new PonderPlugin() {
            @Override public String getModId() { return "ponder_revo_harness"; }
            @Override public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
                for (ResourceLocation component : new ResourceLocation[]{new ResourceLocation("minecraft:compass"), CABLE}) {
                    for (int i = 0; i < 2; i++) {
                        final int number = i;
                        helper.addStoryBoard(component, new ResourceLocation("ponder:demo/basics"), (scene, util) -> {
                            scene.title(component.getPath() + "_" + number, "Revo UI tooltip verification " + number);
                            scene.configureBasePlate(0, 0, 5); scene.showBasePlate(); scene.idle(40); scene.markAsFinished();
                        }).identifiedBy(new ResourceLocation("ponder_revo_harness", component.getPath() + "_" + i));
                    }
                }
            }
        });
    }

    @Mod.EventHandler public void init(FMLInitializationEvent event) throws Exception {
        output = mc.gameDir.toPath().resolve("ponder-revo-smoke");
        Files.createDirectories(output);
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        mc.gameSettings.renderDistanceChunks = 2;
        mc.gameSettings.hideGUI = true;
        mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        mc.getToastGui().clear();
        MinecraftForge.EVENT_BUS.register(this);
        write("STARTING", null);
    }

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ticks++;
        if (ticks >= exitAt) { mc.shutdown(); return; }
        if (exitAt != Integer.MAX_VALUE || wait-- > 0 || shot != null) return;
        try {
            if (ticks > 2400) throw new IllegalStateException("Timeout at phase " + phase);
            switch (phase) {
                case 0:
                    if (!(mc.currentScreen instanceof GuiMainMenu)) return;
                    check(Loader.isModLoaded("neofontrender_ui_enhancements") == modern, "expected optional Revo UI installation");
                    WorldSettings settings = new WorldSettings(1729, GameType.CREATIVE, false, false, WorldType.FLAT);
                    settings.setGeneratorOptions("3;minecraft:air;1;"); settings.enableCommands();
                    mc.launchIntegratedServer("ponder-revo-smoke", "Ponder Revo UI smoke", settings);
                    break;
                case 1:
                    if (mc.world == null || mc.player == null) return;
                    language("zh_cn");
                    configure(true, true, 10, 1);
                    Item cable = Item.REGISTRY.getObject(CABLE);
                    boolean hasCable = cable != null && cable != Items.AIR;
                    check(!Boolean.getBoolean("ponder.revo.requireAe2s") || hasCable, "requested AE2S gray smart cable available");
                    show("tooltip-zh", hasCable ? new ItemStack(cable) : new ItemStack(Items.COMPASS), true);
                    break;
                case 2:
                    language("en_us");
                    show("tooltip-en", screen.subject, true);
                    break;
                case 3:
                    resize(854, 480);
                    show("tooltip-wrapped", new ItemStack(Items.COMPASS).setStackDisplayName(
                        "Ponder multi-scene example with a much longer item name that needs to wrap across several title lines "
                            + "in this small window with the model preview and rarity label enabled"), true);
                    break;
                case 4:
                    resize(1280, 720);
                    configure(true, true, 16, 1.5f);
                    show("tooltip-large-text", new ItemStack(Items.COMPASS), true);
                    break;
                case 5:
                    configure(true, true, 8, .75f);
                    show("tooltip-compact", new ItemStack(Items.COMPASS), true);
                    break;
                case 6:
                    configure(true, false, 12, 1);
                    show("tooltip-no-preview", new ItemStack(Items.COMPASS), true);
                    break;
                case 7:
                    configure(false, false, 10, 1);
                    show("tooltip-vanilla", new ItemStack(Items.COMPASS), false);
                    break;
                case 8:
                    PonderUI viewer = PonderUI.of(new ItemStack(Items.COMPASS));
                    mc.displayGuiScreen(viewer);
                    check(viewer.getActiveScene() != null, "tutorial viewer opens with Revo installed or absent");
                    shot = "viewer.png"; wait = 15;
                    break;
                default:
                    check(PonderIndex.getSceneAccess().getRegisteredEntries().size() == 4,
                        "optional tooltip integration does not inject tutorials");
                    check(PonderIndex.getDiagnosticSnapshot(net.createmod.ponder.api.diagnostic.PonderDiagnosticView.LOCAL)
                        .getIssues().isEmpty(), "fixture scenes have no diagnostic issues");
                    write("PASS", null); exitAt = ticks + 5; return;
            }
            phase++; wait = Math.max(wait, 10);
        } catch (Throwable failure) { fail(failure); }
    }

    private void show(String name, ItemStack subject, boolean revo) {
        screen = new TooltipScreen(subject, modern && revo);
        mc.displayGuiScreen(screen);
        shot = name + ".png";
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void tooltip(RenderTooltipEvent.PostText event) throws Exception {
        tooltip = event;
        // Observe the real captured geometry before Ponder's NORMAL listener consumes it.
        ThreadLocal<?> pending = (ThreadLocal<?>) field(RevoTooltipLayout.class, "CURRENT").get(null);
        Object snapshot = pending.get();
        bounds = snapshot == null ? null : (RevoTooltipLayout.Bounds) field(snapshot.getClass(), "bounds").get(snapshot);
    }

    @SubscribeEvent public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || shot == null || wait > 0) return;
        if (!shot.equals("viewer.png") && mc.currentScreen != screen) {
            mc.displayGuiScreen(screen); screen.draws = 0; wait = 5; return;
        }
        // Let the new screen finish its first paint before checking the framebuffer.
        if (mc.currentScreen == screen && screen.draws < 3) return;
        String name = shot;
        try {
            ScreenShotHelper.saveScreenshot(output.toFile(), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            if (!name.equals("viewer.png")) verifyFrame(name);
            check(GL11.glGetError() == GL11.GL_NO_ERROR, name + ": no OpenGL errors");
        } catch (Throwable failure) { fail(failure); }
        finally { shot = null; }
    }

    private void verifyFrame(String name) throws Exception {
        check(tooltip != null && PonderTooltipHandler.progressRow(tooltip.getLines()) > 0, name + ": progress row survives layout");
        check((bounds != null) == screen.revo, name + ": actual Revo row hook or vanilla fallback");
        int row = PonderTooltipHandler.progressRow(tooltip.getLines());
        int x = bounds == null ? tooltip.getX() : bounds.x;
        int y = bounds == null ? tooltip.getY() + 4 + row * 10 : bounds.y;
        int width = bounds == null ? tooltip.getWidth() : bounds.width;
        int height = bounds == null ? 6 : bounds.height;
        check(width >= 3 && height >= 3, name + ": usable track bounds");
        if (screen.revo && !name.contains("no-preview"))
            check(x > tooltip.getX() && width < tooltip.getWidth(), name + ": model preview excluded from track");
        if (name.contains("wrapped")) check(row > 2, name + ": long title actually wraps");
        BufferedImage image = ScreenShotHelper.createScreenshot(mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        int scale = new ScaledResolution(mc).getScaleFactor(), track = 0, filled = 0;
        check(x >= 0 && y >= 0 && (x + width) * scale <= image.getWidth() && (y + height) * scale <= image.getHeight(),
            name + ": track stays within screen");
        for (int px = x * scale; px < (x + width) * scale; px++) {
            int rgb = image.getRGB(px, (y + height / 2) * scale) & 0xffffff;
            if (rgb == 0x526373 || rgb == 0x1c2630 || rgb == 0x9fd6d2) track++;
            if (rgb == 0x9fd6d2) filled++;
        }
        check(track >= width * scale * .98, name + ": rendered track spans full text width (" + track + "/" + width * scale + ")");
        check(Math.abs(filled / (double)(width * scale) - .6) < .035, name + ": fill is sixty percent");
        // A full-width border outside this row would reproduce the reported overlap.
        int stray = 0;
        for (int py = Math.max(0, tooltip.getY() * scale); py < Math.min(image.getHeight(), (tooltip.getY() + tooltip.getHeight()) * scale); py++) {
            if (py >= y * scale && py < (y + height) * scale) continue;
            if ((image.getRGB(x * scale, py) & 0xffffff) == 0x526373
                && (image.getRGB((x + width - 1) * scale, py) & 0xffffff) == 0x526373) stray++;
        }
        check(stray == 0, name + ": no extra progress border across title or instructions");
        JsonObject frame = new JsonObject();
        frame.addProperty("image", "screenshots/" + name); frame.addProperty("revo", screen.revo);
        frame.addProperty("x", x); frame.addProperty("y", y); frame.addProperty("width", width); frame.addProperty("height", height);
        frame.addProperty("tooltipX", tooltip.getX()); frame.addProperty("tooltipWidth", tooltip.getWidth());
        frame.addProperty("scale", scale); frame.addProperty("progressRow", row); frames.add(frame);
    }

    private void configure(boolean enabled, boolean preview, int lineHeight, float scale) throws Exception {
        if (!modern) return;
        Class<?> config = Class.forName("neofontrender.addons.tooltips.TooltipConfig");
        field(config, "enabled").setBoolean(null, enabled);
        field(config, "headerIconEnabled").setBoolean(null, preview);
        field(config, "rarityEnabled").setBoolean(null, preview);
        field(config, "itemPreviewEnabled").setBoolean(null, preview);
        field(config, "itemPreviewScope").set(null, "all");
        field(config, "lineHeight").setInt(null, lineHeight);
        Method profile = config.getDeclaredMethod("profile", String.class); profile.setAccessible(true);
        Object vanilla = profile.invoke(null, "vanilla");
        field(vanilla.getClass(), "textScale").setFloat(vanilla, scale);
    }

    private void language(String code) {
        mc.gameSettings.language = code;
        mc.getLanguageManager().setCurrentLanguage(mc.getLanguageManager().getLanguages().stream()
            .filter(language -> code.equals(language.getLanguageCode())).findFirst().orElseThrow());
        mc.refreshResources();
    }

    private void resize(int width, int height) throws Exception {
        Display.setDisplayMode(new DisplayMode(width, height)); mc.resize(width, height);
    }

    private void check(boolean condition, String description) {
        if (!condition) throw new IllegalStateException(description);
        checks.add(description);
    }

    private void fail(Throwable failure) {
        Ponder.LOGGER.error("Revo UI smoke failed at phase " + phase, failure);
        write("FAIL", failure); exitAt = ticks + 5;
    }

    private void write(String result, Throwable failure) {
        try {
            JsonObject report = new JsonObject();
            report.addProperty("result", result); report.addProperty("phase", phase);
            report.addProperty("cleanroom", Loader.instance().getIndexedModList().get("cleanroom").getVersion());
            report.addProperty("java", System.getProperty("java.version"));
            report.addProperty("mode", modern ? "present" : "absent");
            report.addProperty("renderer", GL11.glGetString(GL11.GL_RENDERER));
            for (String id : new String[]{"neofontrender_ui_enhancements", "neofontrender", "ae2"})
                if (Loader.isModLoaded(id)) report.addProperty(id, Loader.instance().getIndexedModList().get(id).getVersion());
            java.net.URL source = net.createmod.ponder.PonderMod.class.getProtectionDomain().getCodeSource().getLocation();
            if (source.getProtocol().equals("jar")) source = ((java.net.JarURLConnection)source.openConnection()).getJarFileURL();
            Path jar = Path.of(source.toURI());
            report.addProperty("ponderSource", jar.toString());
            report.addProperty("ponderSha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))));
            report.add("checks", checks); report.add("frames", frames);
            if (failure != null) {
                StringWriter trace = new StringWriter(); failure.printStackTrace(new PrintWriter(trace));
                report.addProperty("failure", trace.toString());
            }
            Files.writeString(output.resolve("report.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (Exception error) { Ponder.LOGGER.error("Cannot write Revo report", error); }
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
    }

    private final class TooltipScreen extends GuiScreen {
        final ItemStack subject;
        final boolean revo;
        int draws;
        TooltipScreen(ItemStack subject, boolean revo) { this.subject = subject; this.revo = revo; }
        @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground(); tooltip = null; bounds = null;
            try {
                subject.getTooltip(mc.player, ITooltipFlag.TooltipFlags.NORMAL);
                field(PonderTooltipHandler.class, "progress").setFloat(null, .6f);
                renderToolTip(subject, width / 2, height / 2);
                draws++;
            } catch (ReflectiveOperationException error) { fail(error); }
        }
    }
}
