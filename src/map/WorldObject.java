package map;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * วัตถุ 1 ชิ้นบนแผนที่ (มีภาพ, ตำแหน่ง และกล่องชนช่วงเท้า)
 */
public class WorldObject {
    public final BufferedImage image;
    public final int x;
    public final int y;
    public final Rectangle collider; // อาจเป็น null

    public WorldObject(BufferedImage image, int x, int y, Rectangle collider) {
        this.image = image;
        this.x = x;
        this.y = y;
        this.collider = collider;
    }

    public int footY() {
        return y + image.getHeight(); // ใช้ท้ายรูปเพื่อจัดลำดับวาด (depth)
    }

    public void draw(Graphics2D g) {
        g.drawImage(image, x, y, null);
    }
}