import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class CharacterSelectPanel extends JPanel {
    private GamePanel gamePanel;
    private CharacterConfig[] characters = {
            new CharacterConfig("Hero 1", 0, 6),
            new CharacterConfig("Hero 2", 1, 6),
            new CharacterConfig("Hero 3", 0, 7),
            new CharacterConfig("Hero 4", 1, 7),
            new CharacterConfig("Hero 5", 0, 8),
            new CharacterConfig("Hero 6", 1, 8),
            new CharacterConfig("Hero 7", 0, 9),
            new CharacterConfig("Hero 8", 1, 9),
            new CharacterConfig("Hero 9", 0, 10),
            new CharacterConfig("Hero 10", 1, 10),
            new CharacterConfig("Hero 11", 0, 11),
            new CharacterConfig("Hero 12", 1, 11),
    };

    CharacterSelectPanel(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
        setPreferredSize(new Dimension(1400, 800));
        setBackground(new Color(34, 139, 34));
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        g2d.drawString("เลือกตัวละคร", 500, 80);

        int x = 100;
        int y = 150;
        for (int i = 0; i < characters.length; i++) {
            drawCharacterOption(g2d, characters[i], x, y, i);
            x += 180;
            if (x > 1200) {
                x = 100;
                y += 220;
            }
        }

        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        g2d.drawString("กดเลขเพื่อเลือก (1-9, 0 สำหรับ Hero 10, Q=11, W=12)", 380, 750);
    }

    private void drawCharacterOption(Graphics2D g2d, CharacterConfig config, int x, int y, int index) {
        BufferedImage sprite = gamePanel.getCharacterSprite(config.col, config.row);
        
        g2d.setColor(new Color(100, 150, 100));
        g2d.fillRect(x - 40, y - 40, 150, 150);
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(x - 40, y - 40, 150, 150);

        if (sprite != null) {
            g2d.drawImage(sprite, x, y, 100, 100, null);
        } else {
            g2d.setColor(Color.BLACK);
            g2d.drawString("?", x + 40, y + 50);
        }

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString(config.name, x - 30, y + 120);
        
        String keyLabel = "";
        if (index < 9) keyLabel = "[" + (index + 1) + "]";
        else if (index == 9) keyLabel = "[0]";
        else if (index == 10) keyLabel = "[Q]";
        else if (index == 11) keyLabel = "[W]";
        
        g2d.drawString(keyLabel, x + 25, y + 120);
    }

    public void handleKeyPress(KeyEvent e) {
        int index = -1;
        
        if (e.getKeyChar() >= '1' && e.getKeyChar() <= '9') {
            index = e.getKeyChar() - '1';
        } else if (e.getKeyChar() == '0') {
            index = 9;
        } else if (e.getKeyChar() == 'q' || e.getKeyChar() == 'Q') {
            index = 10;
        } else if (e.getKeyChar() == 'w' || e.getKeyChar() == 'W') {
            index = 11;
        }
        
        if (index >= 0 && index < characters.length) {
            gamePanel.setCharacter(characters[index]);
            CardLayout layout = (CardLayout) getParent().getLayout();
            layout.show(getParent(), "game");
            gamePanel.requestFocus();
        }
    }
}