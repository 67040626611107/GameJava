import java.awt.*;
import java.awt.image.BufferedImage;

public class Player {
    // ตำแหน่งสำหรับวาด (คงไว้เป็น int เพื่อความเข้ากันได้)
    int x, y;
    int money = 0;
    private CharacterConfig character;

    // ขนาดสปรাইটผู้เล่น
    private static final int SIZE = 40;

    // ค่าดั้งเดิม (ยังคงไว้ แต่จะไม่ใช้สำหรับการเดินแบบลื่น)
    private static final int SPEED = 5;

    // ตำแหน่ง/ความเร็วแบบลอยตัว เพื่อความลื่นไหล
    private double px, py;     // ตำแหน่งจริง (double)
    private double vx, vy;     // ความเร็ว (px/s)
    private double maxSpeed = 220.0;   // ความเร็วสูงสุด (px/s) — ปรับได้
    private double accel    = 1200.0;  // ความเร่งเมื่อกดปุ่ม (px/s^2)
    private double friction = 1400.0;  // แรงเสียดทานเมื่อไม่กด (px/s^2)

    Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.px = x;
        this.py = y;
        this.character = new CharacterConfig("Default", 1, 6);
    }

    void setCharacter(CharacterConfig config) {
        this.character = config;
    }

    // เมธอดเดิม (ไม่ใช้ในโหมดลื่นไหล แต่คงไว้เพื่อความเข้ากันได้)
    void moveUp()    { y = Math.max(0, y - SPEED); }
    void moveDown()  { y = Math.min(800 - SIZE, y + SPEED); }
    void moveLeft()  { x = Math.max(0, x - SPEED); }
    void moveRight() { x = Math.min(1400 - SIZE, x + SPEED); }

    // การเคลื่อนที่แบบลื่นไหล (อัปเดตด้วย dt)
    // worldW/worldH ใช้จำกัดไม่ให้ออกนอกจอ
    void updateMovement(double dt, boolean up, boolean down, boolean left, boolean right, int worldW, int worldH) {
        // คำนวณทิศอินพุต
        double ix = (right ? 1 : 0) - (left ? 1 : 0);
        double iy = (down ? 1 : 0) - (up ? 1 : 0);

        // ปกติค่า dt อาจเป็น 0 ได้ในบางเฟรม
        if (dt < 0) dt = 0;
        if (dt > 0.1) dt = 0.1; // กันเฟรมกระตุก

        // ถ้ามีอินพุต ให้เร่งความเร็วไปตามทิศ (normalize เพื่อกัน diagonal เร็วเกิน)
        double len = Math.hypot(ix, iy);
        if (len > 0) {
            ix /= len; iy /= len;
            vx += ix * accel * dt;
            vy += iy * accel * dt;
        } else {
            // ไม่มีอินพุต: ใช้แรงเสียดทานเพื่อลดความเร็วจนหยุด
            double speed = Math.hypot(vx, vy);
            if (speed > 0) {
                double decel = friction * dt;
                double newSpeed = Math.max(0, speed - decel);
                double scale = (speed == 0) ? 0 : (newSpeed / speed);
                vx *= scale;
                vy *= scale;
            }
        }

        // จำกัดความเร็วสูงสุด
        double speed = Math.hypot(vx, vy);
        if (speed > maxSpeed) {
            double s = maxSpeed / speed;
            vx *= s;
            vy *= s;
        }

        // อัปเดตตำแหน่งจริง
        px += vx * dt;
        py += vy * dt;

        // จำกัดไม่ให้ออกนอกขอบเขต (อิงกับ SIZE)
        if (px < 0) { px = 0; vx = 0; }
        if (py < 0) { py = 0; vy = 0; }
        if (px > worldW - SIZE) { px = worldW - SIZE; vx = 0; }
        if (py > worldH - SIZE) { py = worldH - SIZE; vy = 0; }

        // sync ไปที่ตำแหน่งจำนวนเต็มสำหรับการวาด
        x = (int)Math.round(px);
        y = (int)Math.round(py);
    }

    void addMoney(int amount) {
        money += amount;
    }

    void draw(Graphics2D g2d, BufferedImage spriteSheet, GamePanel gamePanel) {
        if (spriteSheet != null) {
            BufferedImage sprite = gamePanel.getCharacterSprite(character.col, character.row);
            if (sprite != null) {
                g2d.drawImage(sprite, x, y, SIZE, SIZE, null);
                return;
            }
        }
        
        g2d.setColor(new Color(200, 100, 50));
        g2d.fillRect(x, y, SIZE, SIZE);
        g2d.setColor(new Color(255, 150, 100));
        g2d.fillOval(x + 5, y - 10, 30, 25);
        g2d.setColor(Color.BLACK);
        g2d.fillOval(x + 12, y - 5, 5, 5);
        g2d.fillOval(x + 23, y - 5, 5, 5);
    }
}