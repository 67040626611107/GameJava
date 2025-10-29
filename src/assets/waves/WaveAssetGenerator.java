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

public class WaveAssetGenerator {

    public static void main(String[] args) throws IOException {
        File outDir = new File("src/assets/waves");
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new IOException("Cannot create output directory: " + outDir.getAbsolutePath());
        }

        int hudWidth = 860;     // ให้ตรงกับ HUD_W ของเกมคุณ
        int stripHeight = 40;   // ความสูงแถบ (ลอน 30 + margin เล็กน้อย)
        int waveW = 60;         // ความกว้างลอน (สอดคล้องกับที่เคยใช้วาด)
        int waveH = 30;         // ความสูงลอน
        int strokePx = 3;       // ความหนาเส้นss
        int alpha = 150;        // ความโปร่งแสงของเส้น 0..255

        BufferedImage strip = createWaveStrip(hudWidth, stripHeight, waveW, waveH, strokePx, alpha, Color.WHITE);
        ImageIO.write(strip, "png", new File(outDir, "quest_wave_strip.png"));

        BufferedImage tile = createWaveTile(60, 30, 60, 30, 3, 100, Color.WHITE);
        ImageIO.write(tile, "png", new File(outDir, "water_wave_row_60x30.png"));

        System.out.println("✅ Generated:");
        System.out.println(" - " + new File(outDir, "quest_wave_strip.png").getPath());
        System.out.println(" - " + new File(outDir, "water_wave_row_60x30.png").getPath());
    }

    public static BufferedImage createWaveStrip(int width, int stripHeight, int waveW, int waveH,
                                                int strokePx, int alpha, Color crestColor) {
        BufferedImage img = new BufferedImage(width, stripHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setBackground(new Color(0, 0, 0, 0));
            g.clearRect(0, 0, width, stripHeight);

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


    public static BufferedImage createWaveTile(int tileW, int tileH, int waveW, int waveH,
                                               int strokePx, int alpha, Color crestColor) {
        BufferedImage img = new BufferedImage(tileW, tileH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g.setBackground(new Color(0, 0, 0, 0));
            g.clearRect(0, 0, tileW, tileH);

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