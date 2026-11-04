package net.createmod.ponder.compat.revoui;

import java.lang.reflect.Field;
import java.util.List;
import net.createmod.ponder.foundation.PonderTooltipHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;

/** No Revo classes are linked here; all geometry comes from its actual retained rows. */
@SideOnly(Side.CLIENT)
public final class RevoTooltipLayout {
    private static final ThreadLocal<Snapshot> CURRENT = new ThreadLocal<>();
    private static Access access;
    private static boolean warned;

    private RevoTooltipLayout() {}

    public static void clear() { CURRENT.remove(); }

    public static void capture(Object layout, Object content, FontRenderer font, ItemStack stack) {
        if (layout == null || content == null) return;
        try {
            if (access == null) access = new Access(layout.getClass(), content.getClass());
            @SuppressWarnings("unchecked") List<String> lines = (List<String>) access.lines.get(layout);
            int index = PonderTooltipHandler.progressRow(lines);
            if (index < 0) return;
            Bounds bounds = null;
            for (Object row : (List<?>) access.rows.get(content)) {
                if (access.index.getInt(row) != index) continue;
                int height = access.height.getInt(row);
                int barHeight = Math.min(6, Math.max(0, height));
                bounds = new Bounds(access.x.getInt(row), access.y.getInt(row) + (height - barHeight) / 2,
                    Math.max(0, access.width.getInt(row)), barHeight);
                break;
            }
            CURRENT.set(new Snapshot(lines, font, stack, bounds));
        } catch (ReflectiveOperationException | RuntimeException error) {
            clear();
            if (!warned) {
                warned = true;
                LogManager.getLogger("Ponder").warn("Cannot read this Revo UI tooltip layout", error);
            }
        }
    }

    /** Forge wraps the lines list; stack/font identity and one-shot consumption isolate tooltips. */
    public static Bounds consume(RenderTooltipEvent.PostText event) {
        Snapshot snapshot = CURRENT.get();
        clear();
        return snapshot != null && snapshot.lines.equals(event.getLines()) && snapshot.font == event.getFontRenderer()
            && snapshot.stack == event.getStack() ? snapshot.bounds : null;
    }

    public static final class Bounds {
        public final int x, y, width, height;
        Bounds(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
    }

    private static final class Snapshot {
        final List<String> lines;
        final FontRenderer font;
        final ItemStack stack;
        final Bounds bounds;
        Snapshot(List<String> lines, FontRenderer font, ItemStack stack, Bounds bounds) {
            this.lines = lines; this.font = font; this.stack = stack; this.bounds = bounds;
        }
    }

    private static final class Access {
        final Field lines, rows, index, x, y, width, height;
        Access(Class<?> layout, Class<?> content) throws ReflectiveOperationException {
            lines = field(layout, "lines");
            rows = field(content, "rows");
            Class<?> row = Class.forName(content.getName() + "$Row", false, content.getClassLoader());
            index = field(row, "index"); x = field(row, "x"); y = field(row, "y");
            width = field(row, "width"); height = field(row, "height");
        }

        private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        }
    }
}
