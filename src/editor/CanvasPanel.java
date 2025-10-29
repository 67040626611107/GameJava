package editor;

import map.MapData;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.AlphaComposite;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;


public class CanvasPanel extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    private final AssetPalettePanel palette;
    private MapData data;
    private boolean showGrid = true;

    private int lastMouseX = 0, lastMouseY = 0;
    private int camX = 0, camY = 0;

    // middle-pan
    private boolean panning = false;
    private int panStartScreenX = 0, panStartScreenY = 0;
    private int panStartCamX = 0, panStartCamY = 0;
    private Cursor savedCursor = null;

    // snap control
    private int placementSnap = 64;
    private static final int MIN_SNAP = 2;
    private static final int MAX_SNAP = 128;

    // modifiers
    private boolean altDown = false;
    private boolean ctrlDown = false;
    private boolean shiftDown = false;

    // center-water overlay (World 2)
    private boolean centerWaterPreview = false;
    private Rectangle centerWaterRectPx = null;
    private int centerWaterRadiusTiles = 6;
    private Integer centerTilesW = null, centerTilesH = null;

    // image cache for drawing
    private final Map<String, BufferedImage> imageCache = new HashMap<>();

    public CanvasPanel(AssetPalettePanel palette, int width, int height) {
        this.palette = palette;

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
    }

    // center-water overlay control
    public void setCenterWaterPreview(boolean enabled, int radiusTiles, Integer tilesW, Integer tilesH) {
        this.centerWaterPreview = enabled;
        this.centerWaterRadiusTiles = Math.max(1, radiusTiles);
        this.centerTilesW = tilesW;
        this.centerTilesH = tilesH;

        if (!enabled) {
            this.centerWaterRectPx = null;
            repaint();
            return;
        }
        int tile = (data != null ? data.tileSize : 64);
        int mapW = (tilesW != null ? tilesW * tile : (data != null ? data.width : 1400));
        int mapH = (tilesH != null ? tilesH * tile : (data != null ? data.height : 800));

        int half = this.centerWaterRadiusTiles * tile;
        int cx = mapW / 2;
        int cy = mapH / 2;
        int ww = half * 2;
        int wh = (int)Math.round(half * 1.6);
        int wx = cx - half;
        int wy = cy - (int)Math.round(half * 0.8);

        this.centerWaterRectPx = new Rectangle(wx, wy, ww, wh);
        repaint();
    }

    public MapData getMapData() {
        return data;
    }

    public void load(MapData data) {
        this.data = data != null ? data : new MapData();
        setPreferredSize(new Dimension(this.data.width, this.data.height));
        revalidate();

        if (centerWaterPreview) {
            setCenterWaterPreview(true, centerWaterRadiusTiles, centerTilesW, centerTilesH);
        }
        repaint();
    }

    public void newMap(int width, int height) {
        MapData d = new MapData();
        d.width = width;
        d.height = height;
        d.tileSize = (data != null ? data.tileSize : 64);
        d.waterTopY = Math.max(0, height - 220);
        load(d);
    }

    public void toggleGrid() {
        showGrid = !showGrid;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;

        // ground
        g.setColor(new Color(60, 140, 60));
        g.fillRect(0, 0, getWidth(), getHeight());

        // center-water overlay if enabled
        if (centerWaterPreview && centerWaterRectPx != null) {
            Rectangle r = centerWaterRectPx;
            int x = r.x - camX;
            int y = r.y - camY;
            g.setColor(new Color(26, 168, 208));
            g.fillRect(x, y, r.width, r.height);
            g.setColor(new Color(255, 255, 255, 90));
            g.drawRect(x, y, r.width, r.height);
        } else if (data != null) {
            // world 1 style: horizontal water
            int y = data.waterTopY - camY;
            g.setColor(new Color(26, 168, 208));
            g.fillRect(-camX, y, getWidth(), getHeight() - y);
        }

        // draw grid
        if (showGrid && data != null) {
            g.setColor(new Color(255, 255, 255, 30));
            int step = data.tileSize <= 0 ? 64 : data.tileSize;
            for (int x = -camX % step; x < getWidth(); x += step) g.drawLine(x, 0, x, getHeight());
            for (int y = -camY % step; y < getHeight(); y += step) g.drawLine(0, y, getWidth(), y);
        }

        // draw placed objects (depth-sort by y)
        if (data != null && data.objects != null && !data.objects.isEmpty()) {
            List<MapData.MapObject> list = new ArrayList<>(data.objects);
            list.sort(Comparator.comparingInt(o -> o.y));
            for (MapData.MapObject o : list) {
                BufferedImage img = loadImageFor(o.src);
                if (img != null) {
                    g.drawImage(img, o.x - camX, o.y - camY, null);
                } else {
                    // fallback block
                    g.setColor(new Color(0,0,0,120));
                    g.fillRect(o.x - camX, o.y - camY, 64, 64);
                    g.setColor(Color.WHITE);
                    g.drawRect(o.x - camX, o.y - camY, 64, 64);
                }
                // show collider
                if (o.collide) {
                    int imgH = (img != null ? img.getHeight() : 64);
                    int footH = Math.max(4, o.footH);
                    Rectangle cr = new Rectangle(o.x, o.y + imgH - footH, (img != null ? img.getWidth() : 64), footH);
                    g.setColor(new Color(255,0,0,70));
                    g.fillRect(cr.x - camX, cr.y - camY, cr.width, cr.height);
                    g.setColor(new Color(255,0,0,160));
                    g.drawRect(cr.x - camX, cr.y - camY, cr.width, cr.height);
                }
            }
        }

        // draw hover preview (selected asset)
        drawHoverPreview(g);

        // hint snap
        g.setColor(new Color(0,0,0,120));
        g.fillRoundRect(8, 8, 170, 26, 8, 8);
        g.setColor(new Color(255,255,255,210));
        g.setFont(getFont().deriveFont(Font.PLAIN, 12f));
        g.drawString("Snap: " + placementSnap + " px  (+/-, [, ])", 16, 26);
    }

    private void drawHoverPreview(Graphics2D g) {
        SelectedSprite sel = getSelectedSprite();
        Point place = computePlacementXY(sel);
        int px = place.x - camX;
        int py = place.y - camY;

        if (sel.img != null) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.65f));
            g.drawImage(sel.img, px, py, null);
            g.setComposite(old);
            g.setColor(new Color(255,255,255,120));
            g.drawRect(px, py, sel.img.getWidth(), sel.img.getHeight());
        } else {
            int sz = Math.max(placementSnap, 32);
            g.setColor(new Color(255,255,255,40));
            g.fillRect(px, py, sz, sz);
            g.setColor(new Color(255,255,255,140));
            g.drawRect(px, py, sz, sz);
        }
    }

    private record SelectedSprite(String src, BufferedImage img) {}

    private SelectedSprite getSelectedSprite() {
        String src = palette.getSelectedAssetPath();  
        BufferedImage img = palette.getSelectedImage();
        if (src == null && img == null) return new SelectedSprite(null, null);
        if (img == null && src != null) img = loadImageFor(src);
        return new SelectedSprite(src, img);
    }

    private Point computePlacementXY(SelectedSprite sel) {
        int worldX = lastMouseX + camX;
        int worldY = lastMouseY + camY;

        int snap = placementSnap;
        if (ctrlDown) snap = Math.max(MIN_SNAP, placementSnap / 2);
        if (altDown) snap = 1;  

        int px = (snap <= 1) ? worldX : (worldX / snap) * snap;
        int py = (snap <= 1) ? worldY : (worldY / snap) * snap;

        return new Point(px, py);
    }

    private BufferedImage loadImageFor(String path) {
        if (path == null || path.isEmpty()) return null;
        try {
            if (imageCache.containsKey(path)) return imageCache.get(path);
            File f0 = new File(palette.getManifestRoot(), path);
            File f1 = new File("src/" + path);
            File f2 = new File(path);
            File use = f0.exists() ? f0 : (f1.exists() ? f1 : (f2.exists() ? f2 : null));
            if (use == null) { imageCache.put(path, null); return null; }
            BufferedImage img = ImageIO.read(use);
            imageCache.put(path, img);
            return img;
        } catch (Exception e) {
            imageCache.put(path, null);
            return null;
        }
    }

    // ===== Mouse/Key handlers =====
    @Override public void mouseClicked(MouseEvent e) {}

    @Override public void mousePressed(MouseEvent e) {
        requestFocusInWindow();
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

        lastMouseX = e.getX();
        lastMouseY = e.getY();

        if (SwingUtilities.isLeftMouseButton(e)) {
            SelectedSprite sel = getSelectedSprite();
            if (sel.src == null && sel.img == null) return;
            Point p = computePlacementXY(sel);

            MapData.MapObject o = new MapData.MapObject();
            o.src = sel.src != null ? sel.src : "placeholder";
            o.x = p.x;
            o.y = p.y;
            o.collide = !shiftDown; 
            o.footH = 16;
            o.layer = 0;

            data.objects.add(o); 
            repaint();
        } else if (SwingUtilities.isRightMouseButton(e)) {
            if (data != null && data.objects != null && !data.objects.isEmpty()) {
                int worldX = e.getX() + camX, worldY = e.getY() + camY;
                MapData.MapObject best = null;
                int bestD = Integer.MAX_VALUE;
                for (MapData.MapObject o : data.objects) {
                    int dx = (o.x - worldX), dy = (o.y - worldY);
                    int d = dx*dx + dy*dy;
                    if (d < bestD) { bestD = d; best = o; }
                }
                if (best != null && bestD < 64*64) {
                    data.objects.remove(best);
                    repaint();
                }
            }
        }
    }

    @Override public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isMiddleMouseButton(e)) {
            panning = false;
            setCursor(savedCursor != null ? savedCursor : Cursor.getDefaultCursor());
        }
    }

    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseDragged(MouseEvent e) {
        if (panning) {
            camX = panStartCamX - (e.getX() - panStartScreenX);
            camY = panStartCamY - (e.getY() - panStartScreenY);
            repaint();
        } else {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            repaint();
        }
    }
    @Override public void mouseMoved(MouseEvent e) {
        lastMouseX = e.getX();
        lastMouseY = e.getY();
        repaint();
    }
    @Override public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.isShiftDown()) camX += e.getWheelRotation() * 40;
        else camY += e.getWheelRotation() * 40;
        repaint();
    }

    @Override public void keyTyped(KeyEvent e) {
        char c = e.getKeyChar();
        if (c == '+') {
            placementSnap = Math.min(MAX_SNAP, placementSnap + 1);
            repaint();
        } else if (c == '-') {
            placementSnap = Math.max(MIN_SNAP, placementSnap - 1);
            repaint();
        }
    }

    @Override public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_G -> toggleGrid();
            case KeyEvent.VK_OPEN_BRACKET -> { placementSnap = Math.max(MIN_SNAP, placementSnap / 2); repaint(); }
            case KeyEvent.VK_CLOSE_BRACKET -> { placementSnap = Math.min(MAX_SNAP, placementSnap * 2); repaint(); }
            case KeyEvent.VK_ADD -> { placementSnap = Math.min(MAX_SNAP, placementSnap + 1); repaint(); }
            case KeyEvent.VK_SUBTRACT -> { placementSnap = Math.max(MIN_SNAP, placementSnap - 1); repaint(); }
            case KeyEvent.VK_ALT -> altDown = true;
            case KeyEvent.VK_CONTROL -> ctrlDown = true;
            case KeyEvent.VK_SHIFT -> shiftDown = true;
        }
    }

    @Override public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ALT -> altDown = false;
            case KeyEvent.VK_CONTROL -> ctrlDown = false;
            case KeyEvent.VK_SHIFT -> shiftDown = false;
        }
    }
}