package tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;


public class CuteFantasyManifestBuilder {

    private static final String ROOT = "src/assets/Cute_Fantasy";
    private static final Set<String> IMAGE_EXT = new HashSet<>(Arrays.asList("png","jpg","jpeg","webp","gif"));

    public static void main(String[] args) throws Exception {
        Path root = Paths.get(ROOT);
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            System.err.println("ไม่พบโฟลเดอร์: " + root.toAbsolutePath());
            return;
        }

        System.out.println("🔎 Scanning: " + root.toAbsolutePath());

        List<FileEntry> files = new ArrayList<>();
        Map<String, List<FileEntry>> byDir = new TreeMap<>();

        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (attrs.isRegularFile()) {
                    String rel = root.relativize(file).toString().replace('\\','/');
                    String name = file.getFileName().toString();
                    long size = attrs.size();
                    String ext = extOf(name).toLowerCase(Locale.ROOT);
                    if (IMAGE_EXT.contains(ext) || "txt".equals(ext)) {
                        FileEntry fe = new FileEntry(rel, name, size);
                        files.add(fe);
                        String topDir = topDirOf(rel);
                        byDir.computeIfAbsent(topDir, k -> new ArrayList<>()).add(fe);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        files.sort(Comparator.comparing(f -> f.rel));

        String filesJson = buildFilesJson(files, byDir);
        writeText(ROOT + "/manifest.files.json", filesJson);
        System.out.println("✅ Wrote: " + ROOT + "/manifest.files.json");

        Map<String,String> suggested = suggestSceneKeys(files);
        String suggestedJson = buildSuggestedJson(suggested);
        writeText(ROOT + "/manifest.suggested.json", suggestedJson);
        System.out.println("✅ Wrote: " + ROOT + "/manifest.suggested.json");

        System.out.println("Done. Total files indexed: " + files.size());
    }

    // ---------- helpers ----------

    private static String extOf(String name) {
        int i = name.lastIndexOf('.');
        return (i < 0) ? "" : name.substring(i+1);
    }

    private static String topDirOf(String rel) {
        int idx = rel.indexOf('/');
        return idx >= 0 ? rel.substring(0, idx) : ".";
    }

    private static String esc(String s) {
        return s.replace("\\","\\\\").replace("\"","\\\"");
    }

    private static void writeText(String path, String content) throws IOException {
        File f = new File(path);
        f.getParentFile().mkdirs();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, false))) {
            bw.write(content);
        }
    }

    private static String buildFilesJson(List<FileEntry> files, Map<String, List<FileEntry>> byDir) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"root\": \"").append(esc(ROOT)).append("\",\n");
        sb.append("  \"count\": ").append(files.size()).append(",\n");
        sb.append("  \"groups\": {\n");
        int gd = 0;
        for (Map.Entry<String, List<FileEntry>> e : byDir.entrySet()) {
            if (gd++ > 0) sb.append(",\n");
            sb.append("    \"").append(esc(e.getKey())).append("\": [\n");
            List<FileEntry> list = e.getValue();
            for (int i = 0; i < list.size(); i++) {
                FileEntry fe = list.get(i);
                sb.append("      {");
                sb.append("\"path\": \"").append(esc(fe.rel)).append("\", ");
                sb.append("\"name\": \"").append(esc(fe.name)).append("\", ");
                sb.append("\"size\": ").append(fe.size);
                sb.append("}");
                if (i < list.size()-1) sb.append(",");
                sb.append("\n");
            }
            sb.append("    ]");
        }
        sb.append("\n  ]".replace(']', '}')); // close groups
        sb.append(",\n  \"files\": [\n");
        for (int i = 0; i < files.size(); i++) {
            FileEntry fe = files.get(i);
            sb.append("    {");
            sb.append("\"path\": \"").append(esc(fe.rel)).append("\", ");
            sb.append("\"name\": \"").append(esc(fe.name)).append("\", ");
            sb.append("\"size\": ").append(fe.size);
            sb.append("}");
            if (i < files.size()-1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static Map<String,String> suggestSceneKeys(List<FileEntry> files) {
        Map<String,String> out = new LinkedHashMap<>();
        out.put("grass", findFirst(files, Arrays.asList("grass", "ground_grass", "tile_grass")));
        out.put("sand",  findFirst(files, Arrays.asList("sand", "beach")));
        out.put("water", findFirst(files, Arrays.asList("water", "river", "lake")));

        out.put("tree1", findFirst(files, Arrays.asList("tree_big","tree1","tree")));
        out.put("tree2", findFirst(files, Arrays.asList("tree_big_2","tree2")));
        out.put("rock",  findFirst(files, Arrays.asList("rock","stone")));
        out.put("flower",findFirst(files, Arrays.asList("flower","bush")));
        out.put("house", findFirst(files, Arrays.asList("house","home","hut")));
        out.put("dock",  findFirst(files, Arrays.asList("dock","pier","bridge")));

        out.entrySet().removeIf(e -> e.getValue() == null);
        return out;
    }

    private static String findFirst(List<FileEntry> files, List<String> keywords) {
        for (String kw : keywords) {
            String low = kw.toLowerCase(Locale.ROOT);
            for (FileEntry fe : files) {
                String lowName = fe.name.toLowerCase(Locale.ROOT);
                String lowPath = fe.rel.toLowerCase(Locale.ROOT);
                if (lowName.contains(low) || lowPath.contains(low)) {
                    return ROOT + "/" + fe.rel;
                }
            }
        }
        return null;
    }

    private static String buildSuggestedJson(Map<String,String> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"tileSize\": 64,\n");
        sb.append("  \"paths\": {\n");
        int i = 0;
        for (Map.Entry<String,String> e : map.entrySet()) {
            if (i++ > 0) sb.append(",\n");
            sb.append("    \"").append(esc(e.getKey())).append("\": \"").append(esc(e.getValue())).append("\"");
        }
        sb.append("\n  }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static class FileEntry {
        final String rel;
        final String name;
        final long size;
        FileEntry(String rel, String name, long size) {
            this.rel = rel; this.name = name; this.size = size;
        }
    }
}