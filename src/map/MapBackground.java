package map;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;


public class MapBackground {

    private final int width;
    private final int height;
    private final int tileSize;

    private String manifestRoot = "";
    private final java.util.List<String> manifestFiles = new java.util.ArrayList<>();

    private BufferedImage groundImage;
    private final java.util.List<WorldObject> objects = new java.util.ArrayList<>();
    private final CollisionWorld collisionWorld = new CollisionWorld();

    private int waterTopY;

    private static final int DEFAULT_TILE_SIZE = 64;

    private BufferedImage grassTile;
    private BufferedImage waterTile;

    private static final String[] MAP_CANDIDATES = new String[] {
            "src/assets/maps/map.json",
            "assets/maps/map.json",
            "maps/map.json",
            "./src/assets/maps/map.json"
    };

    public MapBackground(int width, int height, String manifestPath) {
        this(width, height, manifestPath, DEFAULT_TILE_SIZE);
    }

    public MapBackground(int width, int height, String manifestPath, int tileSize) {
        this.width = width;
        this.height = height;
        this.tileSize = Math.max(1, tileSize);

        loadManifest(manifestPath);

        if (!loadFromMapJsonCandidates()) {
            System.out.println("ℹ️ MapBackground: map.json not found. Fallback to basic ground.");
            pickBaseTiles();
            buildGround(height - 220);
        }
    }

    public BufferedImage getGroundImage() { return groundImage; }
    public java.util.List<WorldObject> getObjects() { return objects; }
    public CollisionWorld getCollisionWorld() { return collisionWorld; }
    public int getWaterTopY() { return waterTopY; }

    public int getWorldWidth() { return width; }
    public int getWorldHeight() { return height; }

    private void loadManifest(String manifestPath) {
        try {
            File f = resolveFile(manifestPath);
            if (f == null || !f.exists()) {
                System.out.println("⚠️ Manifest not found at: " + manifestPath);
                return;
            }

            String json = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            Matcher mRoot = Pattern.compile("\"root\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
            if (mRoot.find()) {
                manifestRoot = mRoot.group(1).replace("\\\\", "\\");
            } else {
                manifestRoot = new File(f.getParentFile(), "").getPath().replace("\\", "/");
            }
            Matcher mPath = Pattern.compile("\"path\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
            while (mPath.find()) {
                String rel = mPath.group(1).replace("\\\\", "\\");
                manifestFiles.add(rel);
            }
            System.out.println("✅ Loaded manifest: root=" + manifestRoot + " files=" + manifestFiles.size());
        } catch (Exception e) {
            System.out.println("⚠️ อ่าน manifest ไม่สำเร็จ: " + e.getMessage());
        }
    }

    private File resolveFile(String path) {
        File f = new File(path);
        if (!f.exists()) f = new File("src/" + path);
        if (!f.exists()) f = new File("./" + path);
        return f.exists() ? f : null;
    }

    private File resolveAsset(String rel) {
        if (rel == null || rel.isEmpty()) return null;

        File f = new File(rel);
        if (f.exists()) return f;

        if (!manifestRoot.isEmpty()) {
            f = new File(manifestRoot, rel);
            if (f.exists()) return f;
        }

        f = new File("src/" + rel);
        if (f.exists()) return f;

        if (rel.startsWith("src/")) {
            String tail = rel.substring(4); 
            if (!manifestRoot.isEmpty()) {
                File alt = new File(manifestRoot, tail);
                if (alt.exists()) {
                    System.out.println("↪️ mapped legacy path '" + rel + "' -> '" + alt.getPath() + "'");
                    return alt;
                }
            }
            File alt2 = new File("src/assets/Cute_Fantasy/" + tail);
            if (alt2.exists()) {
                System.out.println("↪️ mapped legacy path '" + rel + "' -> '" + alt2.getPath() + "'");
                return alt2;
            }
        }

        File alt3 = new File("src/assets/Cute_Fantasy/" + rel);
        if (alt3.exists()) return alt3;

        return null;
    }

    private BufferedImage loadImage(String rel) {
        try {
            File f = resolveAsset(rel);
            if (f != null) return ImageIO.read(f);
        } catch (Exception ignore) {}
        return null;
    }

    private boolean loadFromMapJsonCandidates() {
        for (String cand : MAP_CANDIDATES) {
            System.out.println("🔎 MapBackground: looking for map.json at: " + cand);
            if (loadFromMapJson(cand)) return true;
            else System.out.println("… not found or failed: " + cand);
        }
        return false;
    }

    private boolean loadFromMapJson(String path) {
        try {
            File f = resolveFile(path);
            if (f == null || !f.exists()) return false;

            System.out.println("📄 Found map.json at: " + f.getPath());
            String txt = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            MapData data = MapIO.fromJson(txt);

            pickBaseTiles();
            buildGround(data.waterTopY);

            objects.clear();
            collisionWorld.getSolids().clear();

            int ok = 0, fail = 0;
            for (MapData.MapObject o : data.objects) {
                BufferedImage img = loadImage(o.src);
                if (img == null) {
                    fail++;
                    System.out.println("⚠️ Could not load object image: " + o.src);
                    continue;
                }
                Rectangle coll = o.collide
                        ? new Rectangle(o.x, o.y + img.getHeight() - Math.max(4, o.footH), img.getWidth(), Math.max(4, o.footH))
                        : null;
                WorldObject wo = new WorldObject(img, o.x, o.y, coll);
                objects.add(wo);
                if (coll != null) collisionWorld.add(coll);
                ok++;
            }
            objects.sort(Comparator.comparingInt(WorldObject::footY));
            System.out.println("✅ Loaded map.json objects: ok=" + ok + " fail=" + fail);
            return true;
        } catch (Exception ex) {
            System.out.println("⚠️ Load map.json failed: " + ex.getMessage());
            return false;
        }
    }

    private void pickBaseTiles() {
        String g = firstExisting(java.util.Arrays.asList(
                "Tiles/Grass/Grass_1_Middle.png",
                "Tiles/Grass/Grass_2_Middle.png",
                "Tiles/Grass/Grass_3_Middle.png",
                "Tiles/Grass/Grass_4_Middle.png"
        ));
        String w = firstExisting(java.util.Arrays.asList(
                "Tiles/Water/Water_Middle.png",
                "Tiles/Water/Water_Tile_2.png",
                "Tiles/Water/Water_Tile_1.png"
        ));
        grassTile = g != null ? loadImage(g) : null;
        waterTile = w != null ? loadImage(w) : null;
    }

    private String firstExisting(java.util.List<String> candidates) {
        for (String want : candidates) {
            String wantName = new File(want).getName();
            for (String f : manifestFiles) {
                if (f.equals(want) || f.endsWith("/" + wantName)) return f;
            }
        }
        return null;
    }

    private void buildGround(int waterTopYInput) {
        groundImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = groundImage.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            this.waterTopY = waterTopYInput;

            fillTiled(g, grassTile, 0, 0, width, height, new Color(34, 139, 34));
            fillTiled(g, waterTile, 0, waterTopY, width, height - waterTopY, new Color(70, 180, 220));

            int shoreH = Math.max(28, tileSize / 2);
            int shoreY = waterTopY - shoreH;
            g.setColor(new Color(210,180,120));
            g.fillRect(0, shoreY, width, shoreH);

            g.setPaint(new GradientPaint(0, shoreY, new Color(255,255,255,80),
                    0, shoreY - shoreH/2, new Color(255,255,255,0)));
            g.fillRect(0, shoreY - shoreH/2, width, shoreH/2);
        } finally {
            g.dispose();
        }
    }

    private void fillTiled(Graphics2D g, BufferedImage tile, int x, int y, int w, int h, Color fallback) {
        if (tile != null) {
            for (int xx = x; xx < x + w; xx += tileSize)
                for (int yy = y; yy < y + h; yy += tileSize)
                    g.drawImage(tile, xx, yy, tileSize, tileSize, null);
        } else {
            g.setColor(fallback);
            g.fillRect(x, y, w, h);
        }
    }
}