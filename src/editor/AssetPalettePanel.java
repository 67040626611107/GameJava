package editor;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * แสดงรายการรูป “เดี่ยว” จาก manifest และพรีวิว (คัดขนาด <=256x256)
 */
public class AssetPalettePanel extends JPanel {
    private final String manifestRoot;
    private final java.util.List<String> assetPaths = new ArrayList<>();
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private final JLabel preview = new JLabel("Preview", SwingConstants.CENTER);
    private final JTextField search = new JTextField();

    private BufferedImage selectedImage;
    private String selectedPath;

    public AssetPalettePanel(String manifestJsonPath) {
        setLayout(new BorderLayout(6,6));
        String root = parseRoot(manifestJsonPath);
        this.manifestRoot = root == null ? "" : root;

        JPanel top = new JPanel(new BorderLayout(6, 6));
        top.add(new JLabel("Search:"), BorderLayout.WEST);
        top.add(search, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) updatePreview(); });

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(list), new JScrollPane(preview));
        split.setDividerLocation(420);
        add(split, BorderLayout.CENTER);

        search.addActionListener(e -> applyFilter());

        loadAssets(manifestJsonPath);
        applyFilter();
        updatePreview();
    }

    public String getSelectedAssetPath() { return selectedPath; } // manifest-relative
    public BufferedImage getSelectedImage() { return selectedImage; }
    public void refreshSelected() { updatePreview(); }
    public String getManifestRoot() { return manifestRoot; }

    private void applyFilter() {
        String q = search.getText()==null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        model.clear();
        for (String p : assetPaths) {
            String name = new File(p).getName();
            if (q.isEmpty() || name.toLowerCase(Locale.ROOT).contains(q)) {
                model.addElement(p);
            }
        }
        if (!model.isEmpty()) list.setSelectedIndex(0);
    }

    private void updatePreview() {
        selectedPath = list.getSelectedValue();
        if (selectedPath == null) { preview.setIcon(null); preview.setText("Preview"); selectedImage = null; return; }

        try {
            File f = new File(manifestRoot, selectedPath);
            if (!f.exists()) f = new File("src/" + selectedPath);
            BufferedImage img = ImageIO.read(f);
            selectedImage = img;

            if (img != null) {
                int max = 256;
                double scale = Math.min(1.0, (double)max / Math.max(img.getWidth(), img.getHeight()));
                Image sc = img.getScaledInstance((int)(img.getWidth()*scale), (int)(img.getHeight()*scale), Image.SCALE_SMOOTH);
                preview.setIcon(new ImageIcon(sc));
                preview.setText("");
            } else {
                preview.setIcon(null);
                preview.setText("Cannot load");
            }
        } catch (Exception ex) {
            preview.setIcon(null);
            preview.setText("Cannot load");
            selectedImage = null;
        }
    }

    private void loadAssets(String manifestJsonPath) {
        try {
            String txt = Files.readString(Path.of(manifestJsonPath), StandardCharsets.UTF_8);
            Matcher m = Pattern.compile("\"path\"\\s*:\\s*\"([^\"]+)\"").matcher(txt);
            Set<String> all = new LinkedHashSet<>();
            while (m.find()) all.add(m.group(1).replace("\\\\","/"));

            java.util.List<String> filtered = new ArrayList<>();
            for (String rel : all) {
                if (!rel.toLowerCase(Locale.ROOT).endsWith(".png")) continue;

                File f = new File(manifestRoot, rel);
                if (!f.exists()) f = new File("src/" + rel);
                if (!f.exists()) continue;

                try {
                    BufferedImage img = ImageIO.read(f);
                    if (img == null) continue;
                    if (img.getWidth() <= 256 && img.getHeight() <= 256) {
                        filtered.add(rel);
                    }
                } catch (Exception ignore) {}
            }

            filtered.sort(Comparator.comparing(s -> new File(s).getName().toLowerCase(Locale.ROOT)));
            assetPaths.clear();
            assetPaths.addAll(filtered);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String parseRoot(String manifestJsonPath) {
        try {
            String txt = Files.readString(Path.of(manifestJsonPath), StandardCharsets.UTF_8);
            Matcher mRoot = Pattern.compile("\"root\"\\s*:\\s*\"([^\"]+)\"").matcher(txt);
            if (mRoot.find()) return mRoot.group(1).replace("\\\\","/");
        } catch (Exception ignore) {}
        return null;
    }
}