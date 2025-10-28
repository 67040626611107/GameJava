import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WorldSelectDialog extends JDialog {

    public interface OnWorldChosen {
        void onChosen(int worldId);
    }

    private final DefaultListModel<GameplayTuning.WorldParams> listModel = new DefaultListModel<>();
    private final JList<GameplayTuning.WorldParams> list = new JList<>(listModel);
    private final PreviewPanel preview = new PreviewPanel();
    private final OnWorldChosen callback;

    public WorldSelectDialog(Window owner, OnWorldChosen cb) {
        super(owner, "เลือก World", ModalityType.MODELESS);
        this.callback = cb;

        setLayout(new BorderLayout(12, 12));
        JPanel main = new JPanel(new BorderLayout(8, 8));
        add(main, BorderLayout.CENTER);

        // world list
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                GameplayTuning.WorldParams wp = (GameplayTuning.WorldParams) value;
                l.setText("#" + wp.id + " - " + wp.name);
                return l;
            }
        });

        // load items (sorted by id)
        List<GameplayTuning.WorldParams> worlds = new ArrayList<>(GameplayTuning.worlds());
        worlds.sort(Comparator.comparingInt(w -> w.id));
        for (GameplayTuning.WorldParams w : worlds) listModel.addElement(w);
        if (!listModel.isEmpty()) list.setSelectedIndex(0);

        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                GameplayTuning.WorldParams wp = list.getSelectedValue();
                preview.setWorld(wp);
            }
        });

        JScrollPane sp = new JScrollPane(list);
        sp.setPreferredSize(new Dimension(260, 320));
        main.add(sp, BorderLayout.WEST);

        // preview
        preview.setPreferredSize(new Dimension(420, 320));
        main.add(preview, BorderLayout.CENTER);

        // buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("ไปโลกนี้");
        JButton cancel = new JButton("ปิด");
        btns.add(ok);
        btns.add(cancel);
        add(btns, BorderLayout.SOUTH);

        ok.addActionListener(e -> {
            GameplayTuning.WorldParams wp = list.getSelectedValue();
            if (wp != null && callback != null) {
                callback.onChosen(wp.id);
            }
            setVisible(false);
            dispose();
        });
        cancel.addActionListener(e -> {
            setVisible(false);
            dispose();
        });

        pack();

        // initial preview
        if (!listModel.isEmpty()) preview.setWorld(listModel.get(0));
    }

    private static class PreviewPanel extends JPanel {
        private GameplayTuning.WorldParams world;
        private BufferedImage img;

        public void setWorld(GameplayTuning.WorldParams wp) {
            this.world = wp;
            this.img = WorldPreviewRenderer.render(wp, 400, 300);
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(25, 25, 25));
            g2.fillRect(0, 0, w, h);

            if (img != null) {
                int ix = (w - img.getWidth()) / 2;
                int iy = (h - img.getHeight()) / 2;
                g2.drawImage(img, ix, iy, null);
            }

            if (world != null) {
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 16));
                g2.drawString(world.name + " (id: " + world.id + ")", 12, 20);
                g2.setFont(new Font("Arial", Font.PLAIN, 12));
                g2.drawString("Map: centerWater=" + (world.map != null && world.map.centerWater) + ", radius=" + (world.map != null ? world.map.waterRadius : 0), 12, 40);
                g2.drawString("ReelRate=" + world.reelProgressRate + ", Wiggle=" + world.fishWiggleStrength, 12, 58);
            }
        }
    }
}