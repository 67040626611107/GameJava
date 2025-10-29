package world2;

import map.MapData;
import map.MapIO;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;


public class World2MapObjects {

    public static class Obj {
        public String src;
        public int x, y;
        public boolean collide = true;
        public int footH = 16;

        public int footY(BufferedImage img) {
            int h = img != null ? img.getHeight() : 64;
            return y + h;
        }

        public Rectangle collider(BufferedImage img) {
            if (!collide) return null;
            int w = img != null ? img.getWidth() : 64;
            int h = img != null ? img.getHeight() : 64;
            int fh = Math.max(4, footH);
            return new Rectangle(x, y + h - fh, Math.max(4, w), fh);
        }
    }

    private final java.util.List<Obj> objs = new ArrayList<>();
    private final Map<String, BufferedImage> imageCache = new HashMap<>();

    public void clear() {
        objs.clear();
        imageCache.clear();
    }

    public void loadFrom(String mapPath) {
        clear();
        try {
            File f = new File(mapPath);
            if (!f.exists()) return;
            String json = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            MapData data = MapIO.fromJson(json);
            if (data == null || data.objects == null) return;

            for (MapData.MapObject o : data.objects) {
                if (o == null || o.src == null || o.src.isEmpty()) continue;
                Obj v = new Obj();
                v.src = o.src;
                v.x = o.x;
                v.y = o.y;
                v.collide = o.collide;
                v.footH = o.footH;
                objs.add(v);
            }
        } catch (Exception ignored) {}
    }

    public boolean isEmpty() { return objs.isEmpty(); }

    public boolean blocks(Rectangle feetRect) {
        for (Obj o : objs) {
            BufferedImage img = loadImage(o.src);
            Rectangle cr = o.collider(img);
            if (cr != null && cr.intersects(feetRect)) return true;
        }
        return false;
    }

    public void drawBefore(Graphics2D g2d, int playerFootY) {
        ArrayList<Obj> list = new ArrayList<>(objs);
        list.sort(Comparator.comparingInt(o -> o.footY(loadImage(o.src))));
        for (Obj o : list) {
            BufferedImage img = loadImage(o.src);
            if (o.footY(img) < playerFootY) drawOne(g2d, o, img);
        }
    }

    public void drawAfter(Graphics2D g2d, int playerFootY) {
        ArrayList<Obj> list = new ArrayList<>(objs);
        list.sort(Comparator.comparingInt(o -> o.footY(loadImage(o.src))));
        for (Obj o : list) {
            BufferedImage img = loadImage(o.src);
            if (o.footY(img) >= playerFootY) drawOne(g2d, o, img);
        }
    }

    private void drawOne(Graphics2D g2d, Obj o, BufferedImage img) {
        if (img != null) {
            g2d.drawImage(img, o.x, o.y, null);
        } else {
            g2d.setColor(new Color(0,0,0,120));
            g2d.fillRect(o.x, o.y, 64, 64);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(o.x, o.y, 64, 64);
        }
    }

    private BufferedImage loadImage(String src) {
        if (src == null || src.isEmpty()) return null;
        if (imageCache.containsKey(src)) return imageCache.get(src);
        try {
            File f1 = new File("src/" + src);
            File f2 = new File(src);
            File use = f1.exists() ? f1 : (f2.exists() ? f2 : null);
            BufferedImage img = use != null ? ImageIO.read(use) : null;
            imageCache.put(src, img);
            return img;
        } catch (Exception e) {
            imageCache.put(src, null);
            return null;
        }
    }
}