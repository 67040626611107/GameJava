package editor;

import map.MapData;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.AlphaComposite;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * แคนวาสวางออบเจ็กต์แบบ snap-to-grid + กล้องแพนได้ + ปรับ snap ได้
 * - คลิกซ้าย: วางออบเจ็กต์ (Shift = ไม่ชน, Ctrl = half-snap, Alt = free place)
 * - คลิกขวา: ลบออบเจ็กต์ที่อยู่ใกล้
 * - Space: วางที่ตำแหน่งเมาส์ (Shift = ไม่ชน, Ctrl = half-snap, Alt = free)
 * - G: toggle grid
 * - [ / ]: ลด/เพิ่ม snap (เช่น 64 -> 32 -> 16 -> 8 ... และย้อนกลับ)
 * - Middle mouse drag: แพนแผนที่
 * - Wheel: แพนแนวตั้ง (Shift+Wheel = แพนแนวนอน)
 * - Arrow keys / WASD: แพน (32px ต่อครั้ง, กด Shift = 8px)
 */
public class CanvasPanel extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    private final AssetPalettePanel palette;
    private final String manifestRoot;
    private MapData data;
    private boolean showGrid = true;

    // ตำแหน่งเมาส์ล่าสุด (จอ)
    private int lastMouseX = 0;
    private int lastMouseY = 0;

    // กล้อง
    private int camX = 0;
    private int camY = 0;

    // แพนด้วยเมาส์กลาง
    private boolean panning = false;
    private int panStartScreenX = 0, panStartScreenY = 0;
    private int panStartCamX = 0, panStartCamY = 0;
    private Cursor savedCursor = null;

    // ปรับ snap
    private int placementSnap = 64;     // หน่วย snap ที่ใช้วาง (ปรับได้)
    private static final int MIN_SNAP = 2;
    private static final int MAX_SNAP = 128;

    // สถานะคีย์ modifier
    private boolean altDown = false;
    private boolean ctrlDown = false;
    private boolean shiftDown = false;

    public CanvasPanel(AssetPalettePanel palette, int width, int height) {
        this.palette = palette;
        this.manifestRoot = palette.getManifestRoot();

        this.data = new MapData();
        this.data.width = width;
        this.data.height = height;
        this.data.tileSize = 64;
        this.data.waterTopY = height - 220;

        setBackground(new Color(40, 120, 40));
        setPreferredSize(new Dimension(width, height));

        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        setFocusable(true);
        addKeyListener(this);

        lastMouseX = width / 2;
        lastMouseY = height / 2;

        // Key bindings: Space (วาง), G (grid), [ ] (snap), แพนด้วยคีย์
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "placeCollide");
        am.put("placeCollide", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { placeAtMouse(!shiftDown); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.SHIFT_DOWN_MASK), "placeNoCollide");
        am.put("placeNoCollide", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { placeAtMouse(false); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_G, 0), "toggleGrid");
        am.put("toggleGrid", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { toggleGrid(); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, 0), "snapDown");
        am.put("snapDown", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { adjustSnapDown(); } });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, 0), "snapUp");
        am.put("snapUp", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { adjustSnapUp(); } });

        am.put("panLeft", new AbstractAction() { @Override public void actionPerformed(ActionEvent e){ panBy(step(e, -32), 0); }});
        am.put("panRight", new AbstractAction(){ @Override public void actionPerformed(ActionEvent e){ panBy(step(e, 32), 0); }});
        am.put("panUp", new AbstractAction()   { @Override public void actionPerformed(ActionEvent e){ panBy(0, step(e, -32)); }});
        am.put("panDown", new AbstractAction() { @Override public void actionPerformed(ActionEvent e){ panBy(0, step(e, 32)); }});

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "panLeft");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "panRight");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "panUp");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "panDown");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "panLeft");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "panRight");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), "panUp");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "panDown");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK), "panLeft");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK), "panRight");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.SHIFT_DOWN_MASK), "panUp");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.SHIFT_DOWN_MASK), "panDown");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.SHIFT_DOWN_MASK), "panLeft");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.SHIFT_DOWN_MASK), "panRight");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.SHIFT_DOWN_MASK), "panUp");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.SHIFT_DOWN_MASK), "panDown");
    }

    private int step(ActionEvent e, int base){
        boolean shift = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;
        return shift ? (base < 0 ? -8 : 8) : base;
    }

    private void adjustSnapDown() {
        placementSnap = Math.max(MIN_SNAP, placementSnap / 2);
        repaint();
    }

    private void adjustSnapUp() {
        placementSnap = Math.min(MAX_SNAP, placementSnap * 2);
        repaint();
    }

    @Override public void addNotify() {
        super.addNotify();
        palette.refreshSelected();
    }

    public void newMap(int w, int h) {
        data = new MapData();
        data.width = w;
        data.height = h;
        data.tileSize = 64;
        data.waterTopY = h - 220;
        camX = camY = 0;
        setPreferredSize(new Dimension(w, h));
        revalidate();
        repaint();
    }

    public void load(MapData in) {
        this.data = in;
        camX = camY = 0;
        setPreferredSize(new Dimension(in.width, in.height));
        revalidate();
        repaint();
    }

    public MapData getMapData() { return data; }

    public void toggleGrid(){ showGrid = !showGrid; repaint(); }
    public boolean isGrid(){ return showGrid; }

    private void panBy(int dx, int dy){
        camX += dx; camY += dy;
        clampCamera();
        repaint();
    }

    private void clampCamera(){
        int maxX = Math.max(0, data.width - getWidth());
        int maxY = Math.max(0, data.height - getHeight());
        camX = Math.max(0, Math.min(camX, maxX));
        camY = Math.max(0, Math.min(camY, maxY));
    }

    private Point screenToWorld(int sx, int sy){
        return new Point(sx + camX, sy + camY);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        // world transform ตามกล้อง
        g2.translate(-camX, -camY);

        // พื้น
        g2.setColor(new Color(70, 180, 220));
        g2.fillRect(0, data.waterTopY, data.width, data.height - data.waterTopY);
        int shoreH = Math.max(28, data.tileSize/2);
        int shoreY = data.waterTopY - shoreH;
        g2.setColor(new Color(210,180,120));
        g2.fillRect(0, shoreY, data.width, shoreH);
        g2.setColor(new Color(34, 139, 34));
        g2.fillRect(0, 0, data.width, shoreY);

        // grid (ใหญ่)
        if (showGrid) {
            g2.setColor(new Color(255,255,255,32));
            for (int x=0;x<=data.width;x+=data.tileSize) g2.drawLine(x,0,x,data.height);
            for (int y=0;y<=data.height;y+=data.tileSize) g2.drawLine(0,y,data.width,y);
        }

        // objects
        ArrayList<MapData.MapObject> sorted = new ArrayList<>(data.objects);
        sorted.sort(Comparator.comparingInt(o -> (o.y + getImageHeightSafe(o))));
        for (MapData.MapObject o : sorted) {
            BufferedImage img = readImage(o.src);
            if (img == null) continue;
            g2.drawImage(img, o.x, o.y, null);

            if (o.collide) {
                int imgH = img.getHeight();
                int imgW = img.getWidth();
                int cy = o.y + imgH - Math.max(4, o.footH);
                g2.setColor(new Color(255, 0, 0, 80));
                g2.fillRect(o.x, cy, imgW, Math.max(4, o.footH));
                g2.setColor(new Color(255, 0, 0, 120));
                g2.drawRect(o.x, cy, imgW, Math.max(4, o.footH));
            }
        }

        // Ghost preview
        BufferedImage sel = palette.getSelectedImage();
        if (sel == null) {
            palette.refreshSelected();
            sel = palette.getSelectedImage();
        }
        if (sel != null) {
            int worldMouseX = lastMouseX + camX;
            int worldMouseY = lastMouseY + camY;

            int effSnap = effectiveSnap();
            int gx, gy;
            if (altDown) {
                gx = worldMouseX;
                gy = worldMouseY;
            } else {
                gx = snap(worldMouseX, effSnap);
                gy = snap(worldMouseY, effSnap);
            }

            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.SrcOver.derive(0.8f));
            g2.drawImage(sel, gx, gy, null);
            g2.setComposite(old);

            g2.setColor(new Color(255, 255, 255, 140));
            g2.drawRect(gx, gy, sel.getWidth(), sel.getHeight());
            g2.setColor(new Color(0,0,0,150));
            g2.fillRoundRect(gx, Math.max(0, gy-18), 220, 16, 8, 8);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.drawString(shortName() + "  |  Snap: " + (altDown ? "Free" : (effSnap + "px")) +
                            (ctrlDown ? " (half)" : ""), gx + 6, Math.max(10, gy-6));
        }

        g2.dispose();

        // HUD (หน้าจอ) แสดงค่าสแนปมุมซ้ายบน
        Graphics2D hud = (Graphics2D) g;
        hud.setColor(new Color(0,0,0,120));
        hud.fillRoundRect(8,8, 160, 24, 8,8);
        hud.setColor(Color.WHITE);
        hud.setFont(new Font("SansSerif", Font.PLAIN, 12));
        hud.drawString("Snap: " + (altDown ? "Free" : (effectiveSnap() + " px")) + "   ([ / ])", 16, 24);
    }

    private int effectiveSnap() {
        if (altDown) return 1; // free
        if (ctrlDown) return Math.max(MIN_SNAP, placementSnap / 2);
        return placementSnap;
    }

    private String shortName() {
        String p = palette.getSelectedAssetPath();
        if (p == null) return "";
        return new File(p).getName();
    }

    private BufferedImage readImage(String rel) {
        try {
            if (rel == null) return null;
            File f = new File(rel);
            if (!f.exists()) f = new File("src/" + rel);
            if (!f.exists() && manifestRoot != null && !manifestRoot.isEmpty()) {
                f = new File(manifestRoot, rel);
            }
            if (!f.exists() && rel.startsWith("src/") && manifestRoot != null) {
                String tail = rel.substring(4);
                f = new File(manifestRoot, tail);
            }
            if (!f.exists()) return null;
            return ImageIO.read(f);
        } catch (Exception e) { return null; }
    }

    private int getImageHeightSafe(MapData.MapObject o) {
        BufferedImage img = readImage(o.src);
        return (img!=null ? img.getHeight() : 0);
    }

    private int snap(int v, int grid) {
        if (grid <= 1) return v; // free
        return Math.max(0, (v / grid) * grid);
    }

    // วาง ณ ตำแหน่งเมาส์ (world) โดยอ่าน alt/ctrl/shift ปัจจุบัน
    private void placeAtMouse(boolean collide) {
        String rel = palette.getSelectedAssetPath();       // manifest-relative
        BufferedImage img = palette.getSelectedImage();
        if (rel == null || img == null) {
            palette.refreshSelected();
            rel = palette.getSelectedAssetPath();
            img = palette.getSelectedImage();
            if (rel == null || img == null) return;
        }

        int worldMouseX = lastMouseX + camX;
        int worldMouseY = lastMouseY + camY;

        int effSnap = effectiveSnap();
        int gx, gy;
        if (altDown) {
            gx = worldMouseX;
            gy = worldMouseY;
        } else {
            gx = snap(worldMouseX, effSnap);
            gy = snap(worldMouseY, effSnap);
        }

        MapData.MapObject o = new MapData.MapObject();
        o.src = rel;
        o.x = gx;
        o.y = gy;
        o.collide = collide;
        o.footH = Math.max(10, img.getHeight()/6);
        o.layer = 0;

        data.objects.add(o);
        repaint();
    }

    // ---------- input ----------
    @Override public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        requestFocusInWindow();

        altDown = e.isAltDown();
        ctrlDown = e.isControlDown();
        shiftDown = e.isShiftDown();

        if (SwingUtilities.isMiddleMouseButton(e)) {
            panning = true;
            panStartScreenX = e.getX();
            panStartScreenY = e.getY();
            panStartCamX = camX;
            panStartCamY = camY;
            savedCursor = getCursor();
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            return;
        }

        if (SwingUtilities.isLeftMouseButton(e)) {
            String rel = palette.getSelectedAssetPath();
            BufferedImage img = palette.getSelectedImage();
            if (rel == null || img == null) {
                palette.refreshSelected();
                rel = palette.getSelectedAssetPath();
                img = palette.getSelectedImage();
                if (rel == null || img == null) return;
            }

            Point w = screenToWorld(e.getX(), e.getY());
            int effSnap = e.isAltDown() ? 1 : (e.isControlDown() ? Math.max(MIN_SNAP, placementSnap/2) : placementSnap);
            int gx = effSnap <= 1 ? w.x : snap(w.x, effSnap);
            int gy = effSnap <= 1 ? w.y : snap(w.y, effSnap);

            MapData.MapObject o = new MapData.MapObject();
            o.src = rel;
            o.x = gx;
            o.y = gy;
            o.collide = !e.isShiftDown();
            o.footH = Math.max(10, img.getHeight()/6);
            o.layer = 0;

            data.objects.add(o);
            repaint();
        } else if (SwingUtilities.isRightMouseButton(e)) {
            Point w = screenToWorld(e.getX(), e.getY());
            int mx = w.x, my = w.y;
            MapData.MapObject hit = null;
            double best = Double.MAX_VALUE;
            for (MapData.MapObject o : data.objects) {
                BufferedImage img = readImage(o.src);
                if (img == null) continue;
                Rectangle r = new Rectangle(o.x, o.y, img.getWidth(), img.getHeight());
                if (r.contains(mx, my)) {
                    double d = Math.hypot(mx - (o.x + r.width/2.0), my - (o.y + r.height/2.0));
                    if (d < best) { best = d; hit = o; }
                }
            }
            if (hit != null) {
                data.objects.remove(hit);
                repaint();
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isMiddleMouseButton(e) && panning) {
            panning = false;
            if (savedCursor != null) setCursor(savedCursor);
            savedCursor = null;
        }
        altDown = e.isAltDown();
        ctrlDown = e.isControlDown();
        shiftDown = e.isShiftDown();
    }

    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e) { }

    @Override
    public void mouseMoved(MouseEvent e) {
        lastMouseX = e.getX();
        lastMouseY = e.getY();
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        lastMouseX = e.getX();
        lastMouseY = e.getY();

        if (panning && SwingUtilities.isMiddleMouseButton(e)) {
            int dxScreen = e.getX() - panStartScreenX;
            int dyScreen = e.getY() - panStartScreenY;
            camX = panStartCamX - dxScreen;
            camY = panStartCamY - dyScreen;
            clampCamera();
        }
        repaint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int notches = e.getWheelRotation();
        boolean shift = (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
        if (shift) {
            panBy(notches * 32, 0);
        } else {
            panBy(0, notches * 32);
        }
    }

    // Track modifier keys (เพื่อใช้กับ ghost/space)
    @Override public void keyTyped(KeyEvent e) { }
    @Override public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_G) toggleGrid();
        if (e.getKeyCode() == KeyEvent.VK_ALT) altDown = true;
        if (e.getKeyCode() == KeyEvent.VK_CONTROL) ctrlDown = true;
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) shiftDown = true;
        repaint();
    }
    @Override public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ALT) altDown = false;
        if (e.getKeyCode() == KeyEvent.VK_CONTROL) ctrlDown = false;
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) shiftDown = false;
        repaint();
    }
}