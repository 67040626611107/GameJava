import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sprites.NPCSpriteSheet;


public class CharacterSelectPanel extends JPanel {

    public interface Listener {
        void onCharacterSelected(CharacterConfig cfg);
    }

    private final GamePanel gamePanel;
    private final Listener listener; 

    private final DefaultListModel<String> npcModel = new DefaultListModel<>();
    private final JList<String> npcList = new JList<>(npcModel);

    private final JButton useBtn = new JButton("ใช้ตัวนี้");
    private final JLabel hint = new JLabel("เลือก NPC จาก src/assets/Cute_Fantasy/NPCs (Premade)");

    private final PreviewPanel previewPanel = new PreviewPanel();

    private static final String NPC_DIR = "src/assets/Cute_Fantasy/NPCs (Premade)";
    private static final String ANIM_MAP = "src/assets/npc_animations.json";
    private static final int NPC_CELL = 64;

    private final Map<String, ImageIcon> thumbCache = new HashMap<>();
    private NPCSpriteSheet previewNPC;
    private Timer previewTimer;
    private WindowAdapter windowFocusRestorer;

    public CharacterSelectPanel(GamePanel gamePanel) {
        this(gamePanel, null);
    }

    public CharacterSelectPanel(GamePanel gamePanel, Listener listener) {
        this.gamePanel = gamePanel;
        this.listener = listener;

        setLayout(new BorderLayout(8,8));
        JPanel top = new JPanel(new BorderLayout());
        top.add(hint, BorderLayout.WEST);
        add(top, BorderLayout.NORTH);

        npcList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        npcList.setCellRenderer(new NPCItemRenderer());
        JScrollPane leftScroll = new JScrollPane(npcList);
        leftScroll.setPreferredSize(new Dimension(260, 360));

        JPanel right = new JPanel(new BorderLayout());
        right.add(previewPanel, BorderLayout.CENTER);
        previewPanel.setPreferredSize(new Dimension(360, 360));

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(leftScroll, BorderLayout.WEST);
        center.add(right, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        bottom.add(useBtn);
        add(bottom, BorderLayout.SOUTH);

        loadNPCs();
        if (!npcModel.isEmpty()) npcList.setSelectedIndex(0);

        useBtn.addActionListener(this::onUse);
        npcList.addListSelectionListener(this::onSelectionChanged);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (previewTimer == null) {
            previewTimer = new Timer(120, e -> previewPanel.repaint());
            previewTimer.start();
        }
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w != null && windowFocusRestorer == null) {
            windowFocusRestorer = new WindowAdapter() {
                @Override public void windowClosed(WindowEvent e) { gamePanel.requestFocusInWindow(); }
                @Override public void windowClosing(WindowEvent e) { gamePanel.requestFocusInWindow(); }
            };
            w.addWindowListener(windowFocusRestorer);
        }
    }

    @Override
    public void removeNotify() {
        if (previewTimer != null) {
            previewTimer.stop();
            previewTimer = null;
        }
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w != null && windowFocusRestorer != null) {
            w.removeWindowListener(windowFocusRestorer);
        }
        windowFocusRestorer = null;
        super.removeNotify();
    }

    private void loadNPCs() {
        npcModel.clear();
        List<String> files = listPNG(NPC_DIR);
        Collections.sort(files, String.CASE_INSENSITIVE_ORDER);
        for (String f : files) npcModel.addElement(f);
    }

    private List<String> listPNG(String dir) {
        List<String> out = new ArrayList<>();
        File d = new File(dir);
        if (!d.exists()) return out;
        File[] fs = d.listFiles();
        if (fs == null) return out;
        for (File f : fs) {
            if (f.isFile() && f.getName().toLowerCase().endsWith(".png")) {
                out.add(f.getName());
            }
        }
        return out;
    }

    private void onUse(ActionEvent e) {
        String name = npcList.getSelectedValue();
        if (name == null) {
            JOptionPane.showMessageDialog(this, "เลือกไฟล์ก่อน");
            return;
        }
        String path = NPC_DIR + "/" + name;
        try {
            CharacterConfig cfg = new CharacterConfig(name, path, NPC_CELL);
            gamePanel.setCharacter(cfg);

            if (listener != null) {
                listener.onCharacterSelected(cfg);
            } else {
                Window w = SwingUtilities.getWindowAncestor(this);
                if (w instanceof JDialog) ((JDialog) w).dispose();
                gamePanel.requestFocusInWindow();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "โหลดตัวละครไม่สำเร็จ: " + ex.getMessage());
        }
    }

    private void onSelectionChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        String name = npcList.getSelectedValue();
        if (name == null) return;
        String path = NPC_DIR + "/" + name;

        try {
            previewNPC = new NPCSpriteSheet(path, NPC_CELL, ANIM_MAP);
        } catch (Exception ex) {
            previewNPC = null;
        }
        previewPanel.repaint();
    }

    private ImageIcon getThumbnail(String name) {
        return thumbCache.computeIfAbsent(name, key -> {
            String path = NPC_DIR + "/" + key;
            try {
                NPCSpriteSheet sheet = new NPCSpriteSheet(path, NPC_CELL, ANIM_MAP);
                BufferedImage frame = sheet.get(NPCSpriteSheet.Action.IDLE_DOWN, System.currentTimeMillis());
                int size = 32;
                Image scaled = frame.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            } catch (Exception ex) {
                BufferedImage stub = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = stub.createGraphics();
                g.setColor(new Color(160,160,160));
                g.fillRect(0,0,32,32);
                g.dispose();
                return new ImageIcon(stub);
            }
        });
    }

    private class NPCItemRenderer extends JLabel implements ListCellRenderer<String> {
        public NPCItemRenderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        }
        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            setText(value);
            setIcon(getThumbnail(value));
            setHorizontalTextPosition(SwingConstants.RIGHT);
            setIconTextGap(8);
            if (isSelected) {
                setBackground(new Color(35, 110, 180));
                setForeground(Color.WHITE);
            } else {
                setBackground(Color.WHITE);
                setForeground(Color.DARK_GRAY);
            }
            return this;
        }
    }

    private class PreviewPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(new Color(20, 22, 28));
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

            g2d.setColor(new Color(255,255,255,32));
            g2d.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 16, 16);

            if (previewNPC == null) {
                drawCenteredString(g2d, "ไม่มีพรีวิว", getWidth(), getHeight());
                g2d.dispose();
                return;
            }

            long now = System.currentTimeMillis();
            BufferedImage frame = previewNPC.get(NPCSpriteSheet.Action.WALK_DOWN, now);

            int scale = Math.max(2, Math.min(getWidth(), getHeight()) / 64 / 2 * 2 + 2);
            int drawW = frame.getWidth() * scale;
            int drawH = frame.getHeight() * scale;

            int x = (getWidth() - drawW) / 2;
            int y = (getHeight() - drawH) / 2 + 24;
            g2d.drawImage(frame, x, y, drawW, drawH, null);

            g2d.setColor(new Color(0, 0, 0, 60));
            int shadowW = (int)(drawW * 0.5);
            int shadowH = (int)(drawH * 0.08);
            int sx = (getWidth() - shadowW)/2;
            int sy = y + drawH - 12;
            g2d.fillOval(sx, sy, shadowW, shadowH);

            g2d.dispose();
        }

        private void drawCenteredString(Graphics2D g2d, String text, int w, int h) {
            g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
            FontMetrics fm = g2d.getFontMetrics();
            int x = (w - fm.stringWidth(text)) / 2;
            int y = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2d.setColor(new Color(255,255,255,200));
            g2d.drawString(text, x, y);
        }
    }
}