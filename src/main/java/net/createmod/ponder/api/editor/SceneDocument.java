package net.createmod.ponder.api.editor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/** Editable JSON pack with bounded, whole-document undo. Unrecognized metadata is preserved. */
public final class SceneDocument {
    private static final int HISTORY_LIMIT = 100;
    private JsonObject document;
    private JsonObject saved;
    private final Deque<JsonObject> undo = new ArrayDeque<>();
    private final Deque<JsonObject> redo = new ArrayDeque<>();
    private int sceneIndex;
    private long revision;

    public SceneDocument(JsonObject document) {
        if (document == null || !document.has("scenes") || document.getAsJsonArray("scenes").isEmpty())
            throw new IllegalArgumentException("Pack must contain at least one scene");
        this.document = document.deepCopy();
        saved = this.document.deepCopy();
    }

    public JsonObject snapshot() { return document.deepCopy(); }
    public JsonObject scene() { return document.getAsJsonArray("scenes").get(sceneIndex).getAsJsonObject().deepCopy(); }
    public JsonArray steps() { return scene().getAsJsonArray("instructions"); }
    public int sceneIndex() { return sceneIndex; }
    public int sceneCount() { return document.getAsJsonArray("scenes").size(); }
    public long revision() { return revision; }
    public boolean isDirty() { return !document.equals(saved); }
    public boolean canUndo() { return !undo.isEmpty(); }
    public boolean canRedo() { return !redo.isEmpty(); }
    public void markSaved() { saved = document.deepCopy(); }
    public void selectScene(int index) {
        if (index < 0 || index >= sceneCount()) throw new IndexOutOfBoundsException(index);
        sceneIndex = index;
        revision++;
    }

    public void edit(Consumer<JsonObject> mutation) {
        JsonObject replacement = document.deepCopy();
        mutation.accept(replacement);
        if (replacement.equals(document)) return;
        undo.addLast(document);
        if (undo.size() > HISTORY_LIMIT) undo.removeFirst();
        document = replacement;
        redo.clear();
        revision++;
    }

    public void editScene(Consumer<JsonObject> mutation) {
        edit(pack -> mutation.accept(pack.getAsJsonArray("scenes").get(sceneIndex).getAsJsonObject()));
    }

    public void insertStep(int index, JsonObject instruction) {
        int insertion = Math.max(0, Math.min(index, steps().size()));
        editScene(scene -> {
            JsonArray old = scene.getAsJsonArray("instructions");
            JsonArray result = new JsonArray();
            for (int i = 0; i <= old.size(); i++) {
                if (i == insertion) result.add(instruction.deepCopy());
                if (i < old.size()) result.add(old.get(i));
            }
            scene.add("instructions", result);
        });
    }

    public void replaceStep(int index, JsonObject instruction) {
        editScene(scene -> scene.getAsJsonArray("instructions").set(index, instruction.deepCopy()));
    }

    public void removeStep(int index) {
        editScene(scene -> scene.getAsJsonArray("instructions").remove(index));
    }

    public void moveStep(int from, int to) {
        if (from == to || from < 0 || to < 0 || from >= steps().size() || to >= steps().size()) return;
        editScene(scene -> {
            JsonArray steps = scene.getAsJsonArray("instructions");
            JsonObject moving = steps.get(from).getAsJsonObject();
            steps.remove(from);
            JsonArray replacement = new JsonArray();
            for (int i = 0; i <= steps.size(); i++) {
                if (i == to) replacement.add(moving);
                if (i < steps.size()) replacement.add(steps.get(i));
            }
            scene.add("instructions", replacement);
        });
    }

    public void undo() {
        if (!canUndo()) return;
        redo.addLast(document); document = undo.removeLast();
        sceneIndex = Math.min(sceneIndex, sceneCount() - 1); revision++;
    }

    public void redo() {
        if (!canRedo()) return;
        undo.addLast(document); document = redo.removeLast();
        sceneIndex = Math.min(sceneIndex, sceneCount() - 1); revision++;
    }

    /** Ponder actions overlap; only idle instructions advance the storyboard cursor. */
    public int stepStartTick(int index) {
        JsonArray steps = steps();
        int ticks = 0;
        for (int i = 0; i < Math.min(index, steps.size()); i++) {
            JsonObject step = steps.get(i).getAsJsonObject();
            if ("scene.idle".equals(step.get("op").getAsString()))
                ticks = Math.addExact(ticks, step.get("ticks").getAsInt());
        }
        return ticks;
    }

    public static SceneDocument create(String id, String component, String title, String structure) {
        JsonObject scene = new JsonObject();
        scene.addProperty("id", id); scene.addProperty("component", component);
        scene.addProperty("title", title); scene.addProperty("structure", structure);
        scene.add("tags", new JsonArray());
        JsonArray instructions = new JsonArray();
        JsonObject show = new JsonObject(); show.addProperty("op", "world.show_section");
        JsonObject all = new JsonObject(); all.addProperty("type", "everywhere");
        show.add("selection", all); show.addProperty("direction", "down"); instructions.add(show);
        JsonObject idle = new JsonObject(); idle.addProperty("op", "scene.idle");
        idle.addProperty("ticks", 20); instructions.add(idle);
        JsonObject finish = new JsonObject(); finish.addProperty("op", "scene.finish"); instructions.add(finish);
        scene.add("instructions", instructions);
        JsonArray scenes = new JsonArray(); scenes.add(scene);
        JsonObject pack = new JsonObject(); pack.addProperty("format", 1); pack.addProperty("id", id + "_pack");
        pack.add("scenes", scenes);
        return new SceneDocument(pack);
    }
}
