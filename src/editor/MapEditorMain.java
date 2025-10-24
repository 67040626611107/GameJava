package editor;

import javax.swing.*;

public class MapEditorMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MapEditorFrame f = new MapEditorFrame(
                    "src/assets/Cute_Fantasy/manifest.files.json",
                    "src/assets/maps/map.json"
            );
            f.setVisible(true);
        });
    }
}