import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GamePanel extends JPanel implements KeyListener, MouseMotionListener {
    private Player player;
    private GameState gameState;
    private FishingSequence fishingSequence;
    private java.util.List<Fish> caughtFish;
    private int mouseX = 0;
    private Set<Integer> keysPressed = new HashSet<>();
    private BufferedImage spriteSheet;
    private CharacterConfig currentCharacter;
    private static final int SPRITE_SIZE = 16;
    private static final int MARGIN = 1;
    
    private static final int WIDTH = 1400;
    private static final int HEIGHT = 800;
    private static final int TILE_SIZE = 40;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(34, 139, 34));
        setFocusable(true);
        addKeyListener(this);
        addMouseMotionListener(this);

        player = new Player(WIDTH / 2, HEIGHT / 2);
        gameState = GameState.EXPLORATION;
        caughtFish = new ArrayList<>();
        fishingSequence = null;
        currentCharacter = new CharacterConfig("Default", 1, 6);
        
        loadSpriteSheet();
        new javax.swing.Timer(50, e -> update()).start();
    }

    private void loadSpriteSheet() {
        try {
            File file = new File("spritesheet.png");
            if (!file.exists()) {
                file = new File("src/spritesheet.png");
            }
            if (!file.exists()) {
                file = new File("./src/spritesheet.png");
            }
            
            if (file.exists()) {
                spriteSheet = ImageIO.read(file);
                System.out.println("✅ โหลด spritesheet สำเร็จจาก: " + file.getAbsolutePath());
            } else {
                throw new Exception("ไม่พบไฟล์");
            }
        } catch (Exception e) {
            System.out.println("❌ ไม่พบไฟล์ spritesheet.png");
            System.out.println("📁 Working directory: " + System.getProperty("user.dir"));
            System.out.println("🎨 จะใช้ asset ที่วาดเองแทน");
            spriteSheet = null;
        }
    }

    void setCharacter(CharacterConfig config) {
        currentCharacter = config;
        player.setCharacter(config);
    }

    BufferedImage getCharacterSprite(int col, int row) {
        if (spriteSheet == null) return null;
        int x = col * (SPRITE_SIZE + MARGIN);
        int y = row * (SPRITE_SIZE + MARGIN);
        try {
            return spriteSheet.getSubimage(x, y, SPRITE_SIZE, SPRITE_SIZE);
        } catch (Exception e) {
            return null;
        }
    }

    private void update() {
        if (gameState == GameState.EXPLORATION) {
            if (keysPressed.contains(KeyEvent.VK_UP)) player.moveUp();
            if (keysPressed.contains(KeyEvent.VK_DOWN)) player.moveDown();
            if (keysPressed.contains(KeyEvent.VK_LEFT)) player.moveLeft();
            if (keysPressed.contains(KeyEvent.VK_RIGHT)) player.moveRight();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (gameState) {
            case EXPLORATION:
                drawExploration(g2d);
                break;
            case CASTING:
                drawCasting(g2d);
                break;
            case SNAG:
                drawSnag(g2d);
                break;
            case REELING:
                drawReeling(g2d);
                break;
            case RESULT:
                drawResult(g2d);
                break;
            case INVENTORY:
                drawInventory(g2d);
                break;
        }

        drawUI(g2d);
    }

    private void drawExploration(Graphics2D g2d) {
        for (int x = 0; x < WIDTH; x += TILE_SIZE) {
            for (int y = 0; y < HEIGHT; y += TILE_SIZE) {
                g2d.setColor(new Color(34, 139, 34));
                g2d.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                g2d.setColor(new Color(25, 120, 25));
                g2d.drawRect(x, y, TILE_SIZE, TILE_SIZE);
            }
        }

        g2d.setColor(new Color(70, 180, 220));
        g2d.fillRect(0, HEIGHT - 200, WIDTH, 200);

        g2d.setColor(new Color(100, 200, 255, 100));
        for (int i = 0; i < WIDTH; i += 60) {
            g2d.drawArc(i, HEIGHT - 200, 50, 30, 0, 180);
        }

        player.draw(g2d, spriteSheet, this);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("ลูกศร = เคลื่อนที่ | SPACE = ตกปลา | I = Inventory", 20, 30);
        g2d.drawString("ไปยังน้ำเพื่อตกปลา", 20, 50);
    }

    private void drawCasting(Graphics2D g2d) {
        drawWater(g2d);
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        g2d.setColor(Color.WHITE);
        g2d.drawString("ขั้นที่ 1: CASTING", WIDTH / 2 - 200, 80);

        g2d.setColor(Color.BLACK);
        g2d.fillRect(150, 200, WIDTH - 300, 80);
        
        float castProgress = (float) fishingSequence.castTimeRemaining / fishingSequence.castMaxTime;
        g2d.setColor(new Color(100, 200, 50));
        g2d.fillRect(150, 200, (int)((WIDTH - 300) * castProgress), 80);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("กดปุ่มเว้นวรรค ให้พอดีที่ระดับ 'S'", WIDTH / 2 - 180, 245);

        g2d.setColor(new Color(255, 50, 50));
        g2d.fillRect(150 + (int)((WIDTH - 300) * 0.7), 200, 8, 80);
    }

    private void drawSnag(Graphics2D g2d) {
        drawWater(g2d);
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        g2d.setColor(Color.WHITE);
        g2d.drawString("ขั้นที่ 2: SNAG (ปลากัด!)", WIDTH / 2 - 250, 80);

        g2d.setColor(Color.BLACK);
        g2d.fillRect(150, 200, WIDTH - 300, 80);

        float snagProgress = (float) fishingSequence.snagTimeRemaining / fishingSequence.snagMaxTime;
        g2d.setColor(new Color(50, 150, 255));
        g2d.fillRect(150, 200, (int)((WIDTH - 300) * snagProgress), 80);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("กดปุ่มเว้นวรรค เมื่อปลากัด", WIDTH / 2 - 180, 245);

        if (fishingSequence.shaking) {
            g2d.setColor(new Color(255, 100, 100));
            g2d.setFont(new Font("Arial", Font.BOLD, 28));
            g2d.drawString("🐟 ปลากัด! 🐟", WIDTH / 2 - 100, 350);
        }
    }

    private void drawReeling(Graphics2D g2d) {
        drawWater(g2d);
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        g2d.setColor(Color.WHITE);
        g2d.drawString("ขั้นที่ 3: REELING", WIDTH / 2 - 200, 80);

        g2d.setColor(Color.BLACK);
        g2d.fillRect(150, 200, WIDTH - 300, 50);

        float reelingProgress = (float) fishingSequence.reelingValue / fishingSequence.reelingMaxValue;
        g2d.setColor(new Color(50, 200, 50));
        g2d.fillRect(150, 200, (int)((WIDTH - 300) * reelingProgress), 50);

        int barX = 150;
        int barY = 350;
        int barWidth = WIDTH - 300;
        g2d.setColor(Color.BLACK);
        g2d.fillRect(barX, barY, barWidth, 40);

        g2d.setColor(new Color(100, 150, 100));
        int controlZoneStart = (int)(barWidth * 0.35);
        int controlZoneEnd = (int)(barWidth * 0.65);
        g2d.fillRect(barX + controlZoneStart, barY, controlZoneEnd - controlZoneStart, 40);

        g2d.setColor(new Color(255, 100, 100));
        int tension = (int)(barWidth * fishingSequence.tension);
        g2d.fillRect(barX + tension, barY, 8, 40);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.drawString("Tension: " + String.format("%.0f%%", fishingSequence.tension * 100), WIDTH / 2 - 100, 430);
    }

    private void drawResult(Graphics2D g2d) {
        drawWater(g2d);

        if (fishingSequence.success) {
            g2d.setColor(new Color(0, 200, 0, 150));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            g2d.drawString("ตกปลาสำเร็จ!", WIDTH / 2 - 180, 150);

            Fish caughtFishObj = fishingSequence.caughtFish;
            g2d.setFont(new Font("Arial", Font.PLAIN, 28));
            g2d.drawString("ชนิด: " + caughtFishObj.name, WIDTH / 2 - 120, 300);
            g2d.drawString("ราคา: " + caughtFishObj.price + " บาท", WIDTH / 2 - 120, 350);
        } else {
            g2d.setColor(new Color(200, 0, 0, 150));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            g2d.drawString("ปลาหนีไป!", WIDTH / 2 - 150, 200);
        }

        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.drawString("กดเว้นวรรค เพื่อกลับเมนู", WIDTH / 2 - 150, 550);
    }

    private void drawInventory(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 32));
        g2d.drawString("Inventory", WIDTH / 2 - 80, 50);

        int y = 120;
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        Map<String, Integer> fishCount = new HashMap<>();
        for (Fish fish : caughtFish) {
            fishCount.put(fish.name, fishCount.getOrDefault(fish.name, 0) + 1);
        }

        for (Map.Entry<String, Integer> entry : fishCount.entrySet()) {
            g2d.drawString(entry.getKey() + " x" + entry.getValue(), 100, y);
            y += 40;
        }

        if (caughtFish.isEmpty()) {
            g2d.drawString("ยังไม่มีปลา", 100, 150);
        }

        g2d.drawString("กดเว้นวรรค เพื่อกลับเมนู", WIDTH / 2 - 150, HEIGHT - 50);
    }

    private void drawWater(Graphics2D g2d) {
        GradientPaint gp = new GradientPaint(0, 200, new Color(70, 180, 220), 0, HEIGHT, new Color(30, 100, 180));
        g2d.setPaint(gp);
        g2d.fillRect(0, 200, WIDTH, HEIGHT - 200);

        g2d.setColor(new Color(100, 200, 255, 100));
        for (int i = 0; i < WIDTH; i += 50) {
            g2d.drawArc(i, 200, 40, 20, 0, 180);
        }
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, 400, 100);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("เงิน: " + player.money + " บาท", 20, 35);
        g2d.drawString("ปลาที่ตกได้: " + caughtFish.size(), 20, 65);
    }

    private void startFishing() {
        if (player.y < HEIGHT - 200) {
            JOptionPane.showMessageDialog(this, "ต้องอยู่ใกล้น้ำเพื่อตกปลา!");
            return;
        }

        gameState = GameState.CASTING;
        Fish randomFish = Fish.getRandomFish();
        fishingSequence = new FishingSequence(randomFish);
        
        javax.swing.Timer timer = new javax.swing.Timer(50, e -> {
            fishingSequence.update();
            
            if (fishingSequence.phase == FishingPhase.CASTING && fishingSequence.castFinished) {
                fishingSequence.phase = FishingPhase.SNAG;
            } else if (fishingSequence.phase == FishingPhase.SNAG && fishingSequence.snagFinished) {
                fishingSequence.phase = FishingPhase.REELING;
            } else if (fishingSequence.phase == FishingPhase.REELING && fishingSequence.reelingFinished) {
                ((javax.swing.Timer) e.getSource()).stop();
                gameState = GameState.RESULT;
                if (fishingSequence.success) {
                    caughtFish.add(fishingSequence.caughtFish);
                    player.addMoney(fishingSequence.caughtFish.price);
                }
            }
            repaint();
        });
        timer.start();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        keysPressed.add(e.getKeyCode());

        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            switch (gameState) {
                case EXPLORATION:
                    startFishing();
                    break;
                case CASTING:
                    fishingSequence.castPress();
                    break;
                case SNAG:
                    fishingSequence.snagPress();
                    break;
                case RESULT:
                    gameState = GameState.EXPLORATION;
                    break;
                case INVENTORY:
                    gameState = GameState.EXPLORATION;
                    break;
                case REELING:
                    break;
            }
        }

        if (e.getKeyCode() == KeyEvent.VK_I && gameState == GameState.EXPLORATION) {
            gameState = GameState.INVENTORY;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keysPressed.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        if (gameState == GameState.REELING) {
            float normalizedX = (float) mouseX / WIDTH;
            fishingSequence.updateTension(normalizedX);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }
}