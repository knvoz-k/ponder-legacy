package net.createmod.ponder.guide;

import java.util.*;
import com.google.gson.*;

/** Deliberately small YAML frontmatter subset: scalars, string lists and navigation maps. */
final class GuideMetadata {
    private final Map<String, List<String>> values = new LinkedHashMap<>();
    static GuideMetadata parse(String text) {
        GuideMetadata result = new GuideMetadata();
        String group = "", list = "";
        for (String raw : text.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (raw.indexOf('\t') >= 0) throw new IllegalArgumentException("Frontmatter uses spaces for indentation");
            if (line.startsWith("- ")) {
                if (list.isEmpty()) throw new IllegalArgumentException("List has no frontmatter key: " + line);
                result.values.get(list).add(scalar(line.substring(2)));
                continue;
            }
            int colon = line.indexOf(':');
            if (colon < 1) throw new IllegalArgumentException("Expected frontmatter key: value: " + line);
            String key = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();
            boolean indented = raw.startsWith(" ");
            if (!indented) group = "";
            if (indented && !group.isEmpty()) key = group + "." + key;
            if (key.equals("navigation") && value.isEmpty()) { group = key; list = ""; continue; }
            if (!Set.of("id", "title", "item_ids", "structure", "scene", "presentation", "step_ticks",
                "tags", "categories", "navigation.title", "navigation.icon", "navigation.parent", "navigation.position").contains(key))
                throw new IllegalArgumentException("Unsupported frontmatter field: " + key);
            if (result.values.containsKey(key)) throw new IllegalArgumentException("Duplicate frontmatter field: " + key);
            List<String> entries = new ArrayList<>();
            if (value.startsWith("[")) {
                if (!value.endsWith("]")) throw new IllegalArgumentException("Unclosed frontmatter list: " + key);
                // YAML's plain scalar lists are allowed; resource identifiers cannot contain commas.
                for (String part : value.substring(1, value.length() - 1).split(","))
                    if (!part.trim().isEmpty()) entries.add(scalar(part.trim()));
            } else if (!value.isEmpty()) entries.add(scalar(value));
            result.values.put(key, entries);
            list = value.isEmpty() ? key : "";
        }
        return result;
    }
    private static String scalar(String value) {
        if (value.startsWith("\"")) {
            try { return new JsonParser().parse(value).getAsString(); }
            catch (RuntimeException e) { throw new IllegalArgumentException("Invalid quoted frontmatter scalar: " + value); }
        }
        if (value.startsWith("'") && value.endsWith("'")) return value.substring(1, value.length()-1).replace("''", "'");
        if (value.startsWith("{") || value.equals("|") || value.equals(">"))
            throw new IllegalArgumentException("Use scalars, lists and an indented navigation map in frontmatter");
        return value;
    }
    String get(String key, String fallback) {
        List<String> v = values.get(key);
        if (v == null || v.isEmpty()) return fallback;
        if (v.size() != 1) throw new IllegalArgumentException(key + " must be a single scalar");
        return v.get(0);
    }
    List<String> list(String key) { return values.getOrDefault(key, List.of()); }
}
