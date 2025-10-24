package assets.waves;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * WaveAssetGenerator
 *
 * สร้างไฟล์ PNG สำหรับลอนคลื่น 2 แบบ:
 * 1) quest_wave_strip.png  แถบคลื่นสำหรับวางที่ "ขอบบน" ของพื้นที่น้ำ (กว้างเท่า HUD โดยดีฟอลต์ 860x40)
 * 2) water_wave_row_60x30.png  ไทล์คลื่นขนาด 60x30 สำหรับปูซ้ำแนวนอนในพื้นที่น้ำ
 *
 * วิธีรันจากโฟลเดอร์โปรเจกต์ (สมมติ JDK พร้อมใช้งาน):
 *   javac -d out src/assets/waves/WaveAssetGenerator.java
 *   java -cp out assets.waves.WaveAssetGenerator
 *
 * ไฟล์เอาต์พุตจะถูกบันทึกไว้ที่: src/assets/waves/
 */
public class WaveAssetGenerator {

    public static void main(String[] args) throws IOException {
        File outDir = new File("src/assets/waves");
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new IOException("Cannot create output directory: " + outDir.getAbsolutePath());
        }

        // 1) แถบคลื่นสำหรับหัวน้ำใต้ HUD
        int hudWidth = 860;     // ให้ตรงกับ HUD_W ของเกมคุณ
        int stripHeight = 40;   // ความสูงแถบ (ลอน 30 + margin เล็กน้อย)
        int waveW = 60;         // ความกว้างลอน (สอดคล้องกับที่เคยใช้วาด)
        int waveH = 30;         // ความสูงลอน
        int strokePx = 3;       // ความหนาเส้น
        int alpha = 150;        // ความโปร่งแสงของเส้น 0..255

        BufferedImage strip = createWaveStrip(hudWidth, stripHeight, waveW, waveH, strokePx, alpha, Color.WHITE);
        ImageIO.write(strip, "png", new File(outDir, "quest_wave_strip.png"));

        // 2) ไทล์คลื่น 60x30 สำหรับปูซ้ำ
        BufferedImage tile = createWaveTile(60, 30, 60, 30, 3, 100, Color.WHITE);
        ImageIO.write(tile, "png", new File(outDir, "water_wave_row_60x30.png"));

        System.out.println("✅ Generated:");
        System.out.println(" - " + new File(outDir, "quest_wave_strip.png").getPath());
        System.out.println(" - " + new File(outDir, "water_wave_row_60x30.png").getPath());
    }

    /**
     * สร้างภาพแถบคลื่นสำหรับวางที่ขอบบนของพื้นที่น้ำ
     * - โปร่งใสทั้งหมด ยกเว้นเส้นคลื่นสีขาวบาง ๆ
     * - จุดอ้างอิง y=0 คือขอบบนของพื้นที่น้ำ
     */
    public static BufferedImage createWaveStrip(int width, int stripHeight, int waveW, int waveH,
                                                int strokePx, int alpha, Color crestColor) {
        BufferedImage img = new BufferedImage(width, stripHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // พื้นหลังโปร่งใส
            g.setBackground(new Color(0, 0, 0, 0));
            g.clearRect(0, 0, width, stripHeight);

            // วาดเส้นคลื่นโปร่งบาง
            g.setColor(new Color(crestColor.getRed(), crestColor.getGreen(), crestColor.getBlue(), clamp(alpha, 0, 255)));
            g.setStroke(new BasicStroke(Math.max(1f, strokePx), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            for (int x = -waveW / 2; x < width + waveW; x += waveW) {
                Arc2D.Double arc = new Arc2D.Double(x, 0, waveW, waveH, 0, 180, Arc2D.OPEN);
                g.draw(arc);
            }
        } finally {
            g.dispose();
        }
        return img;
    }

    /**
     * สร้างไทล์ลอนสำหรับปูแนวนอน (tile)
     * - ขนาดแนะนำ 60x30 เพื่อให้ต่อกับความถี่ลอนเดิม
     */
    public static BufferedImage createWaveTile(int tileW, int tileH, int waveW, int waveH,
                                               int strokePx, int alpha, Color crestColor) {
        BufferedImage img = new BufferedImage(tileW, tileH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // โปร่งใสทั้งภาพ
            g.setBackground(new Color(0, 0, 0, 0));
            g.clearRect(0, 0, tileW, tileH);

            // วาดลอนเดียวเต็มไทล์ เพื่อให้ต่อเนื่องเมื่อ tile
            g.setColor(new Color(crestColor.getRed(), crestColor.getGreen(), crestColor.getBlue(), clamp(alpha, 0, 255)));
            g.setStroke(new BasicStroke(Math.max(1f, strokePx), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Arc2D.Double arc = new Arc2D.Double(0, 0, waveW, waveH, 0, 180, Arc2D.OPEN);
            g.draw(arc);
        } finally {
            g.dispose();
        }
        return img;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}