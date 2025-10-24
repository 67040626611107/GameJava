package editor;

import map.MapData;
import map.MapIO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * หน้าต่างหลักของ Map Editor
 */
public class MapEditorFrame extends JFrame {

    private final String manifestPath;
    private final String mapPath;

    private final AssetPalettePanel palette;
    private final CanvasPanel canvas;

    public MapEditorFrame(String manifestPath, String mapPath) {
        super("Map Editor");
        this.manifestPath = manifestPath;
        this.mapPath = mapPath;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1280, 860);
        setLocationRelativeTo(null);

        palette = new AssetPalettePanel(manifestPath);
        canvas = new CanvasPanel(palette, 1400, 800);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(palette), new JScrollPane(canvas));
        split.setDividerLocation(320);
        add(split, BorderLayout.CENTER);

        setJMenuBar(buildMenuBar());
        loadIfExists();
    }

    private JMenuBar buildMenuBar() {
        JMenuBar mb = new JMenuBar();

        JMenu mFile = new JMenu("File");
        mFile.add(new AbstractAction("New") {
            @Override public void actionPerformed(ActionEvent e) {
                canvas.newMap(1400, 800);
            }
        });
        mFile.add(new AbstractAction("Open…") {
            @Override public void actionPerformed(ActionEvent e) {
                loadIfExists(true);
            }
        });
        mFile.add(new AbstractAction("Save") {
            @Override public void actionPerformed(ActionEvent e) {
                save();
            }
        });
        mFile.add(new AbstractAction("Save As…") {
            @Override public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(new File(mapPath).getParentFile());
                fc.setSelectedFile(new File(mapPath));
                if (fc.showSaveDialog(MapEditorFrame.this) == JFileChooser.APPROVE_OPTION) {
                    save(fc.getSelectedFile().getPath());
                }
            }
        });

        JMenu mMap = new JMenu("Map");
        mMap.add(new AbstractAction("Set Water Height…") {
            @Override public void actionPerformed(ActionEvent e) {
                String s = JOptionPane.showInputDialog(MapEditorFrame.this, "waterTopY:", canvas.getMapData().waterTopY);
                if (s != null) {
                    try {
                        canvas.getMapData().waterTopY = Integer.parseInt(s.trim());
                        canvas.repaint();
                    } catch (Exception ignore) {}
                }
            }
        });
        mMap.add(new AbstractAction("Show Grid (G)") {
            {
                putValue(Action.SELECTED_KEY, Boolean.TRUE);
            }
            @Override public void actionPerformed(ActionEvent e) {
                canvas.toggleGrid();
            }
        });

        mb.add(mFile);
        mb.add(mMap);
        return mb;
    }

    private void loadIfExists() { loadIfExists(false); }

    private void loadIfExists(boolean ask) {
        File f = new File(mapPath);
        if (ask || f.exists()) {
            try {
                if (ask) {
                    JFileChooser fc = new JFileChooser(f.getParentFile());
                    fc.setSelectedFile(f);
                    if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
                    f = fc.getSelectedFile();
                }
                String txt = Files.readString(f.toPath(), StandardCharsets.UTF_8);
                MapData data = MapIO.fromJson(txt);
                canvas.load(data);
                setTitle("Map Editor - " + f.getName());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Load failed: " + ex.getMessage());
            }
        }
    }

    private void save() { save(mapPath); }

    private void save(String path) {
        try {
            MapData data = canvas.getMapData();
            File out = new File(path);
            out.getParentFile().mkdirs();
            Files.writeString(out.toPath(), MapIO.toJson(data), StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this, "Saved: " + out.getPath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
        }
    }
}