package map;

import java.awt.*;
import java.awt.image.BufferedImage;


public class WorldObject {
    public final BufferedImage image;
    public final int x;
    public final int y;
    public final Rectangle collider; 

    public WorldObject(BufferedImage image, int x, int y, Rectangle collider) {
        this.image = image;
        this.x = x;
        this.y = y;
        this.collider = collider;
    }

    public int footY() {
        return y + image.getHeight(); 
    }

    public void draw(Graphics2D g) {
        g.drawImage(image, x, y, null);
    }
}