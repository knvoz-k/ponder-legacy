package net.createmod.ponder.api.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.util.math.BlockPos;

/** Inclusive block selection shared by the world capture tool and the editor. */
public final class CuboidSelection {
    public static final long MAX_CAPTURE_BLOCKS = 32768;
    private final BlockPos min;
    private final BlockPos max;

    public CuboidSelection(BlockPos first, BlockPos second) {
        if (first == null || second == null) throw new IllegalArgumentException("Select both corners");
        min = new BlockPos(Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()),
            Math.min(first.getZ(), second.getZ()));
        max = new BlockPos(Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()),
            Math.max(first.getZ(), second.getZ()));
    }

    public BlockPos min() { return min; }
    public BlockPos max() { return max; }
    public long volume() {
        try {
            return Math.multiplyExact(Math.multiplyExact((long) max.getX() - min.getX() + 1,
                (long) max.getY() - min.getY() + 1), (long) max.getZ() - min.getZ() + 1);
        } catch (ArithmeticException overflow) {
            return Long.MAX_VALUE;
        }
    }

    public void checkCaptureSize() {
        if ((long) max.getX() - min.getX() >= 256 || (long) max.getY() - min.getY() >= 256
            || (long) max.getZ() - min.getZ() >= 256 || volume() > MAX_CAPTURE_BLOCKS)
            throw new IllegalArgumentException("Selection exceeds " + MAX_CAPTURE_BLOCKS + " blocks or 256 per axis");
    }

    public CuboidSelection relativeTo(BlockPos origin) {
        return new CuboidSelection(min.subtract(origin), max.subtract(origin));
    }

    public JsonObject toJson() {
        JsonObject result = new JsonObject();
        result.addProperty("type", "from_to");
        result.add("from", vector(min));
        result.add("to", vector(max));
        return result;
    }

    private static JsonArray vector(BlockPos pos) {
        JsonArray result = new JsonArray();
        result.add(pos.getX()); result.add(pos.getY()); result.add(pos.getZ());
        return result;
    }

    @Override public String toString() {
        return min.getX() + "," + min.getY() + "," + min.getZ() + " -> "
            + max.getX() + "," + max.getY() + "," + max.getZ();
    }
}
