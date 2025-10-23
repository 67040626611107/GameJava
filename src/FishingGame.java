import javax.swing.*;
import java.awt.*;

public class FishingGame extends JFrame {
    public FishingGame() {
        setTitle("Fishing Adventure 2D");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        JPanel mainPanel = new JPanel(new CardLayout());
        GamePanel gamePanel = new GamePanel();
        mainPanel.add(new CharacterSelectPanel(gamePanel), "select");
        mainPanel.add(gamePanel, "game");
        
        add(mainPanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FishingGame::new);
    }
}