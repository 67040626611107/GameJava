import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class SpriteSheetLoader {
    private BufferedImage sheet;
    private int tileWidth, tileHeight, margin;

    public SpriteSheetLoader(String path, int tileWidth, int tileHeight, int margin) throws Exception {
        this.sheet = ImageIO.read(new File(path));
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.margin = margin;
    }

    public BufferedImage getTile(int col, int row) {
        int x = margin + col * (tileWidth + margin);
        int y = margin + row * (tileHeight + margin);
        return sheet.getSubimage(x, y, tileWidth, tileHeight);
    }
}