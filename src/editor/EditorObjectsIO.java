package editor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EditorObjectsIO {

    public static String worldObjectsPath(int worldId) {
        return "editor/objects/world_" + worldId + ".json";
    }

    public static java.util.List<EditorObject> loadForWorld(int worldId) {
        String path = worldObjectsPath(worldId);
        if (!Files.exists(Path.of(path))) return new ArrayList<>();
        try {
            String s = Files.readString(Path.of(path), StandardCharsets.UTF_8);
            return parseObjects(s);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static boolean saveForWorld(int worldId, java.util.List<EditorObject> list) {
        try {
            String json = buildJson(worldId, list);
            Path p = Path.of(worldObjectsPath(worldId));
            if (p.getParent() != null && !Files.exists(p.getParent())) Files.createDirectories(p.getParent());
            Files.writeString(p, json, StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    private static java.util.List<EditorObject> parseObjects(String json) {
        ArrayList<EditorObject> out = new ArrayList<>();
        String arr = extractArray(json, "objects");
        if (arr == null) return out;
        for (String obj : splitObjects(arr)) {
            EditorObject eo = new EditorObject();
            eo.id = readString(obj, "id", UUID.randomUUID().toString());
            eo.spritePath = readString(obj, "spritePath", "");
            eo.x = readInt(obj, "x", 0);
            eo.y = readInt(obj, "y", 0);
            eo.w = readInt(obj, "w", 0);
            eo.h = readInt(obj, "h", 0);
            eo.collides = readBool(obj, "collides", true);
            String coll = extractObject(obj, "coll");
            if (coll != null) {
                int cx = readInt(coll, "x", 0);
                int cy = readInt(coll, "y", 0);
                int cw = readInt(coll, "w", 0);
                int ch = readInt(coll, "h", 0);
                eo.coll = new java.awt.Rectangle(cx, cy, cw, ch);
            }
            out.add(eo);
        }
        return out;
    }

    private static String buildJson(int worldId, java.util.List<EditorObject> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"worldId\": ").append(worldId).append(",\n");
        sb.append("  \"objects\": [\n");
        for (int i = 0; i < list.size(); i++) {
            EditorObject o = list.get(i);
            sb.append("    {\n");
            sb.append("      \"id\": \"").append(esc(o.id)).append("\",\n");
            sb.append("      \"spritePath\": \"").append(esc(o.spritePath)).append("\",\n");
            sb.append("      \"x\": ").append(o.x).append(",\n");
            sb.append("      \"y\": ").append(o.y).append(",\n");
            sb.append("      \"w\": ").append(o.w).append(",\n");
            sb.append("      \"h\": ").append(o.h).append(",\n");
            sb.append("      \"collides\": ").append(o.collides).append(",\n");
            if (o.coll != null) {
                sb.append("      \"coll\": { \"x\": ").append(o.coll.x)
                  .append(", \"y\": ").append(o.coll.y)
                  .append(", \"w\": ").append(o.coll.width)
                  .append(", \"h\": ").append(o.coll.height).append(" }\n");
            } else {
                sb.append("      \"coll\": null\n");
            }
            sb.append("    }");
            if (i < list.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String extractArray(String json, String key) {
        int k = json.indexOf("\"" + key + "\"");
        if (k < 0) return null;
        int lb = json.indexOf('[', k);
        if (lb < 0) return null;
        int rb = findMatching(json, lb, '[', ']');
        if (rb < 0) return null;
        return json.substring(lb, rb + 1);
    }

    private static String extractObject(String json, String key) {
        int k = json.indexOf("\"" + key + "\"");
        if (k < 0) return null;
        int lb = json.indexOf('{', k);
        if (lb < 0) return null;
        int rb = findMatching(json, lb, '{', '}');
        if (rb < 0) return null;
        return json.substring(lb, rb + 1);
    }

    private static int findMatching(String s, int start, char open, char close) {
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == open) depth++;
            else if (c == close) {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static java.util.List<String> splitObjects(String arrayBlock) {
        ArrayList<String> list = new ArrayList<>();
        int i = 0;
        while (i < arrayBlock.length()) {
            int s = arrayBlock.indexOf('{', i);
            if (s < 0) break;
            int e = findMatching(arrayBlock, s, '{', '}');
            if (e < 0) break;
            list.add(arrayBlock.substring(s, e + 1));
            i = e + 1;
        }
        return list;
    }

    private static String readString(String obj, String key, String def) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(obj);
        return m.find() ? m.group(1) : def;
    }

    private static int readInt(String obj, String key, int def) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)");
        Matcher m = p.matcher(obj);
        return m.find() ? Integer.parseInt(m.group(1)) : def;
    }

    private static boolean readBool(String obj, String key, boolean def) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)");
        Matcher m = p.matcher(obj);
        return m.find() ? Boolean.parseBoolean(m.group(1)) : def;
    }

    private EditorObjectsIO() {}
}