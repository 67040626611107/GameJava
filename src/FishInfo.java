import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class FishInfo {
    public final String id;
    public final String name;
    public final String difficulty; // easy/normal/hard/...
    public final double struggleStrength;
    public final int biteTimeSeconds;
    public final int price;
    public final String imagePath; // relative filesystem path, e.g. "src/assets/images/fish/x.png"

    private Image image; // cached runtime image (nullable)

    public FishInfo(String id,
                    String name,
                    String difficulty,
                    double struggleStrength,
                    int biteTimeSeconds,
                    int price,
                    String imagePath) {
        this.id = id;
        this.name = name;
        this.difficulty = difficulty;
        this.struggleStrength = struggleStrength;
        this.biteTimeSeconds = biteTimeSeconds;
        this.price = price;
        this.imagePath = imagePath;
    }


    public static FishInfo fromConfigEntry(WorldConfigLoader.World2Config.FishEntry e) {
        if (e == null) return null;
        // map config image path (config may reference assets/...). Keep as-is.
        return new FishInfo(
                e.id != null ? e.id : "",
                e.name != null ? e.name : "Unknown",
                e.difficulty != null ? e.difficulty : "normal",
                e.struggleStrength,
                e.biteTimeSeconds,
                e.price,
                e.image != null ? e.image : ""
        );
    }

    public synchronized Image getImage() {
        if (image == null) loadImageOrPlaceholder();
        return image;
    }

    private void loadImageOrPlaceholder() {
        Image img = null;
        if (imagePath != null && !imagePath.trim().isEmpty()) {
            img = loadFromPath(imagePath);
        }
        if (img == null) {
            // fallback placeholder path expected in repo
            img = loadFromPath("src/assets/images/no_preview.png");
            if (img == null) {
                // another common asset location
                img = loadFromPath("assets/images/no_preview.png");
            }
        }
        image = img;
    }

    private Image loadFromPath(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) return null;
            BufferedImage bi = ImageIO.read(f);
            return bi;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "FishInfo{id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", diff='" + difficulty + '\'' +
                ", struggle=" + struggleStrength +
                ", biteTime=" + biteTimeSeconds +
                ", price=" + price +
                '}';
    }
}