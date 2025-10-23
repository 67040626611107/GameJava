import java.awt.*;
import java.awt.image.BufferedImage;

public class Player {
    int x, y;
    int money = 0;
    private CharacterConfig character;
    private static final int SPEED = 5;

    Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.character = new CharacterConfig("Default", 1, 6);
    }

    void setCharacter(CharacterConfig config) {
        this.character = config;
    }

    void moveUp() {
        y = Math.max(0, y - SPEED);
    }

    void moveDown() {
        y = Math.min(800 - 40, y + SPEED);
    }

    void moveLeft() {
        x = Math.max(0, x - SPEED);
    }

    void moveRight() {
        x = Math.min(1400 - 40, x + SPEED);
    }

    void addMoney(int amount) {
        money += amount;
    }

    void draw(Graphics2D g2d, BufferedImage spriteSheet, GamePanel gamePanel) {
        if (spriteSheet != null) {
            BufferedImage sprite = gamePanel.getCharacterSprite(character.col, character.row);
            if (sprite != null) {
                g2d.drawImage(sprite, x, y, 40, 40, null);
                return;
            }
        }
        
        g2d.setColor(new Color(200, 100, 50));
        g2d.fillRect(x, y, 40, 40);
        g2d.setColor(new Color(255, 150, 100));
        g2d.fillOval(x + 5, y - 10, 30, 25);
        g2d.setColor(Color.BLACK);
        g2d.fillOval(x + 12, y - 5, 5, 5);
        g2d.fillOval(x + 23, y - 5, 5, 5);
    }
}