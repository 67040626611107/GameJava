package map;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;


public class MapData {
    public int width;
    public int height;
    public int tileSize = 64;
    public int waterTopY;

    public final List<MapObject> objects = new ArrayList<>();

    public static class MapObject {
        public String src;      // พาธภาพ (manifest-relative เช่น "Buildings/...") หรือ path ที่แก้แมปได้
        public int x;
        public int y;
        public boolean collide = true;   // สร้างกล่องชนไหม
        public int footH = 16;           // ความสูงกล่องชน (ช่วงเท้า)
        public int layer = 0;            // 0=default

        public Rectangle colliderRect(int imgW, int imgH) {
            if (!collide || footH <= 0) return null;
            int cx = x;
            int cy = y + imgH - footH;
            return new Rectangle(cx, cy, imgW, Math.max(4, footH));
        }
    }
}