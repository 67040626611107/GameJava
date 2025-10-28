import java.awt.*;
import java.awt.image.BufferedImage;

public class WorldPreviewRenderer {

    // วาดภาพพรีวิว world แบบง่าย: สีเขียว = ดิน, น้ำเงิน = น้ำ
    public static BufferedImage render(GameplayTuning.WorldParams wp, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // พื้นดิน
        g.setColor(new Color(60, 140, 60));
        g.fillRect(0, 0, width, height);

        if (wp != null && wp.map != null && wp.map.centerWater) {
            // บ่อน้ำตรงกลาง
            int r = Math.max(10, Math.min(Math.min(width, height) / 3, wp.map.waterRadius * 8));
            int cx = width / 2;
            int cy = height / 2;
            g.setColor(new Color(20, 180, 200));
            g.fillOval(cx - r, cy - r, r * 2, r * 2);

            // ขอบน้ำ
            g.setColor(new Color(255, 255, 255, 90));
            g.drawOval(cx - r, cy - r, r * 2, r * 2);
        }

        // กรอบ
        g.setColor(new Color(255, 255, 255, 180));
        g.drawRect(0, 0, width - 1, height - 1);

        g.dispose();
        return img;
    }

    // สำหรับ Map editor: คืน mask ว่าน้ำตรงไหนบ้าง (true = น้ำ)
    public static boolean[][] waterMask(GameplayTuning.MapSpec map) {
        boolean[][] mask = new boolean[Math.max(1, map.height)][Math.max(1, map.width)];
        int w = map.width;
        int h = map.height;
        if (map.centerWater) {
            int cx = w / 2;
            int cy = h / 2;
            int r = Math.max(1, map.waterRadius);
            int r2 = r * r;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int dx = x - cx;
                    int dy = y - cy;
                    mask[y][x] = dx * dx + dy * dy <= r2;
                }
            }
        }
        return mask;
    }
}