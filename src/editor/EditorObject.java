package editor;

import java.awt.*;

public class EditorObject {
    public String id;
    public String spritePath;
    public int x;
    public int y;
    public int w;
    public int h;
    public boolean collides = true;
    // คอลลิชันภายในกรอบ (โลคัลจากซ้ายบนของรูป)
    public Rectangle coll; // อาจเป็น null = auto bottom 20%

    public int footY() {
        return y + h;
    }

    public Rectangle worldCollisionRect() {
        if (!collides) return null;
        if (coll != null) return new Rectangle(x + coll.x, y + coll.y, coll.width, coll.height);
        int ch = Math.max(6, (int)Math.round(h * 0.2));
        return new Rectangle(x + 6, y + h - ch, Math.max(4, w - 12), ch);
    }
}