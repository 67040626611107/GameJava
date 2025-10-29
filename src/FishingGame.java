import javax.swing.*;
import java.awt.*;

public class FishingGame extends JFrame implements CharacterSelectPanel.Listener {
    private CardLayout cards;
    private JPanel mainPanel;
    private GamePanel gamePanel;

    public FishingGame() {
        setTitle("Fishing Adventure 2D");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        cards = new CardLayout();
        mainPanel = new JPanel(cards);

        gamePanel = new GamePanel();

        CharacterSelectPanel selectPanel = new CharacterSelectPanel(gamePanel, this);

        mainPanel.add(selectPanel, "select");
        mainPanel.add(gamePanel, "game");

        add(mainPanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        cards.show(mainPanel, "select");
    }

    @Override
    public void onCharacterSelected(CharacterConfig cfg) {
        cards.show(mainPanel, "game");
        SwingUtilities.invokeLater(() -> gamePanel.requestFocusInWindow());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FishingGame::new);
    }
}