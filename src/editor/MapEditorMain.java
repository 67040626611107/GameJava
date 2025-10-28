package editor;

import javax.swing.*;

public class MapEditorMain {

    // เส้นทางไฟล์ที่ใช้กับแต่ละโลก
    private static final String MANIFEST_PATH   = "src/assets/Cute_Fantasy/manifest.files.json";
    private static final String MAP_WORLD1_PATH = "src/assets/maps/map.json";
    private static final String MAP_WORLD2_PATH = "src/assets/maps/map_world2.json"; // แผนที่ของ World 2

    public static void main(String[] args) {
        int worldId = parseWorldId(args);
        if (worldId <= 0) {
            worldId = promptWorldId();
            if (worldId < 0) return; // กดยกเลิก
        }
        openEditorForWorld(worldId);
    }

    // เปิด Map Editor สำหรับ world ที่เลือก พร้อมเลือกไฟล์แผนที่ให้ตรง
    private static void openEditorForWorld(int worldId) {
        final String mapPath = mapPathForWorld(worldId);
        SwingUtilities.invokeLater(() -> {
            MapEditorFrame f = new MapEditorFrame(MANIFEST_PATH, mapPath);
            f.setVisible(true);
        });
    }

    // แปลง world id -> เส้นทางไฟล์แผนที่
    private static String mapPathForWorld(int worldId) {
        return (worldId == 2) ? MAP_WORLD2_PATH : MAP_WORLD1_PATH;
    }

    // รองรับรับ world id จาก argument เช่น "--world=2" หรือ "2"
    private static int parseWorldId(String[] args) {
        if (args == null || args.length == 0) return 0;
        for (String a : args) {
            if (a == null) continue;
            String s = a.trim();
            if (s.matches("^--?world=\\d+$")) {
                try { return Integer.parseInt(s.substring(s.indexOf('=') + 1)); } catch (Exception ignored) {}
            } else if (s.matches("^\\d+$")) {
                try { return Integer.parseInt(s); } catch (Exception ignored) {}
            }
        }
        return 0;
    }

    // ถ้าไม่ได้ส่ง args เข้ามา ให้ขึ้นกล่องถามว่าจะเปิดโลกไหน
    private static int promptWorldId() {
        Object[] options = { "World 1", "World 2", "Cancel" };
        int choice = JOptionPane.showOptionDialog(
                null,
                "Select world to edit:",
                "Map Editor",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
        if (choice == 0) return 1;
        if (choice == 1) return 2;
        return -1; // cancel
    }
}