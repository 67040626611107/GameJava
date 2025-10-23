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

public class GamePanel extends JPanel implements KeyListener, MouseMotionListener, MouseListener {
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

    // Reel minigame (pasted.txt-like)
    private ReelMinigame reelMinigame;

    // HUD layout (โปร่งแสง ไม่บังจอทั้งหมด)
    private static final int HUD_W = 860;
    private static final int HUD_H = 160;
    private static final int HUD_MARGIN_BOTTOM = 40;
    private static final int HUD_RADIUS = 16;
    private static final int HUD_ALPHA = 140; // โปร่งแสง (0-255)
    private static final int HUD_BORDER_ALPHA = 190;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(34, 139, 34));
        setFocusable(true);
        addKeyListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);

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

        // Drive reel minigame tick during REELING
        if (gameState == GameState.REELING && reelMinigame != null && fishingSequence != null && !reelMinigame.isFinished()) {
            int barWidth = HUD_W - 80; // main bar width inside HUD
            reelMinigame.update(0.05, barWidth); // timer ~50ms
            if (reelMinigame.isFinished()) {
                fishingSequence.reelingFinished = true;
                fishingSequence.success = reelMinigame.isSuccess();
            }
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

    // วาดโลกและผู้เล่น (ใช้เป็นฉากหลังสำหรับทุกเฟสที่ต้องการเห็นเกมปกติ)
    private void drawWorld(Graphics2D g2d) {
        // พื้นหญ้าแบบกริด
        for (int x = 0; x < WIDTH; x += TILE_SIZE) {
            for (int y = 0; y < HEIGHT; y += TILE_SIZE) {
                g2d.setColor(new Color(34, 139, 34));
                g2d.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                g2d.setColor(new Color(25, 120, 25));
                g2d.drawRect(x, y, TILE_SIZE, TILE_SIZE);
            }
        }
        // น้ำด้านล่าง
        g2d.setColor(new Color(70, 180, 220));
        g2d.fillRect(0, HEIGHT - 200, WIDTH, 200);
        g2d.setColor(new Color(100, 200, 255, 100));
        for (int i = 0; i < WIDTH; i += 60) {
            g2d.drawArc(i, HEIGHT - 200, 50, 30, 0, 180);
        }
        // ตัวละคร
        player.draw(g2d, spriteSheet, this);
    }

    private void drawExploration(Graphics2D g2d) {
        drawWorld(g2d);

        // ข้อความเฉพาะโหมดสำรวจ
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("ลูกศร = เคลื่อนที่ | SPACE = ตกปลา | I = Inventory", 20, 30);
        g2d.drawString("ไปยังน้ำเพื่อตกปลา", 20, 50);
    }

    private void drawCasting(Graphics2D g2d) {
        // แสดงเกมปกติด้านหลัง
        drawWorld(g2d);

        // กล่อง HUD โปร่งแสงด้านล่าง
        Rectangle hud = hudRect();
        drawHudBox(g2d, hud, HUD_ALPHA, HUD_BORDER_ALPHA);

        // หัวข้อ
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        g2d.drawString("ขั้นที่ 1: รอปลากินเหยื่อ", hud.x + 24, hud.y + 42);

        // แถบความคืบหน้า (เล็กลง ไม่กินเต็มจอ)
        int progX = hud.x + 24;
        int progY = hud.y + 70;
        int progW = hud.width - 48;
        int progH = 26;

        g2d.setColor(new Color(0,0,0,120));
        g2d.fillRoundRect(progX, progY, progW, progH, 12, 12);
        g2d.setColor(new Color(255,255,255,40));
        g2d.drawRoundRect(progX, progY, progW, progH, 12, 12);

        float p = fishingSequence == null ? 0f : (1f - (float)fishingSequence.castTimeRemaining / Math.max(1, fishingSequence.castMaxTime));
        p = Math.max(0, Math.min(1, p));
        int fill = (int)(progW * p);
        g2d.setColor(new Color(50, 150, 255));
        g2d.fillRoundRect(progX, progY, fill, progH, 12, 12);

        // คำอธิบาย
        g2d.setColor(new Color(240, 240, 240));
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.drawString("กำลังรอ... เมื่อปลากัด จะเข้าสู่ REELING อัตโนมัติ", hud.x + 24, hud.y + 110);
    }

    private void drawReeling(Graphics2D g2d) {
        // แสดงเกมปกติด้านหลัง
        drawWorld(g2d);

        if (reelMinigame == null) {
            reelMinigame = new ReelMinigame();
        }

        // ข้อความคำแนะนำ (ลอยด้านบน โปร่งแสง เห็นฉากหลัง)
        drawTopHint(g2d, "ค้าง SPACE หรือ คลิกเมาส์ซ้ายค้าง เพื่อดึง");

        // กล่อง HUD โปร่งแสงด้านล่าง
        Rectangle hud = hudRect();
        drawHudBox(g2d, hud, HUD_ALPHA, HUD_BORDER_ALPHA);

        // หัวข้อ
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        g2d.drawString("ขั้นที่ 2: REELING", hud.x + 24, hud.y + 42);

        // ค่าสีตาม progress (แดง -> เขียว)
        float pt = (float) Math.max(0, Math.min(1, reelMinigame.getProgress() / 100.0));
        Color from = new Color(99, 42, 42);
        Color to   = new Color(89, 126, 89);
        Color mix  = new Color(
            (int)(from.getRed()   + (to.getRed()   - from.getRed())   * pt),
            (int)(from.getGreen() + (to.getGreen() - from.getGreen()) * pt),
            (int)(from.getBlue()  + (to.getBlue()  - from.getBlue())  * pt)
        );

        // ตำแหน่งภายใน HUD
        int innerPad = 24;
        int barX = hud.x + innerPad;
        int barY = hud.y + 70;
        int barWidth = hud.width - innerPad * 2;
        int barHeight = 36;

        // แถบหลัก
        g2d.setColor(new Color(0,0,0,120));
        g2d.fillRoundRect(barX, barY, barWidth, barHeight, 12, 12);
        g2d.setColor(new Color(0,0,0,160));
        g2d.drawRoundRect(barX, barY, barWidth, barHeight, 12, 12);

        // player window (white)
        int pbW = (int)Math.round(barWidth * reelMinigame.getControlWidth());
        int pbX = (int)Math.round(barX + reelMinigame.getPlayerBarCenter() * barWidth - pbW/2.0);
        boolean overlap = overlaps(
            reelMinigame.getPlayerBarCenter(),
            reelMinigame.getControlWidth(),
            reelMinigame.getFishCenter(),
            8.0 / barWidth
        );
        g2d.setColor(overlap ? new Color(255,255,255,230) : new Color(255,255,255,170));
        g2d.fillRoundRect(pbX, barY, pbW, barHeight, 10, 10);

        // fish indicator
        int fishPx = (int)Math.round(barX + reelMinigame.getFishCenter()*barWidth - 4);
        g2d.setColor(new Color(67,75,91));
        g2d.fillRoundRect(fishPx, barY, 8, barHeight, 10, 10);

        // progress bar (เล็ก) ด้านบนของ HUD
        int progX = hud.x + innerPad;
        int progY = hud.y + 120;
        int progW = hud.width - innerPad * 2;
        int progH = 22;

        g2d.setColor(new Color(0,0,0,120));
        g2d.fillRoundRect(progX, progY, progW, progH, 10, 10);
        g2d.setColor(new Color(0,0,0,160));
        g2d.drawRoundRect(progX, progY, progW, progH, 10, 10);

        int fill = (int)Math.round(Math.max(0, Math.min(1, reelMinigame.getProgress()/100.0)) * progW);
        g2d.setColor(new Color(255,255,255,230));
        g2d.fillRoundRect(progX, progY, fill, progH, 10, 10);

        // accent line
        g2d.setColor(mix);
        g2d.drawLine(barX, barY-2, barX+barWidth, barY-2);
    }

    private void drawResult(Graphics2D g2d) {
        // ยังใช้ overlay โปร่งแสง แต่ไม่ทึบทั้งจอ
        drawWorld(g2d);

        if (fishingSequence.success) {
            g2d.setColor(new Color(0, 200, 0, 120)); // โปร่งแสง เห็นฉากหลัง
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            g2d.drawString("ตกปลาสำเร็จ!", WIDTH / 2 - 180, 150);

            Fish caughtFishObj = fishingSequence.caughtFish;
            g2d.setFont(new Font("Arial", Font.PLAIN, 28));
            g2d.drawString("ชนิด: " + caughtFishObj.name, WIDTH / 2 - 120, 300);
            g2d.drawString("ราคา: " + caughtFishObj.price + " บาท", WIDTH / 2 - 120, 350);
        } else {
            g2d.setColor(new Color(200, 0, 0, 120)); // โปร่งแสง
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            g2d.drawString("ปลาหนีไป!", WIDTH / 2 - 150, 200);
        }

        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.drawString("กดเว้นวรรค เพื่อกลับเมนู", WIDTH / 2 - 150, 550);
    }

    private void drawInventory(Graphics2D g2d) {
        // ให้เห็นฉากหลัง แล้ว overlay โปร่งแสง
        drawWorld(g2d);

        g2d.setColor(new Color(0, 0, 0, 180)); // โปร่งแสง
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

    private void drawUI(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, 400, 100);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("เงิน: " + player.money + " บาท", 20, 35);
        g2d.drawString("ปลาที่ตกได้: " + caughtFish.size(), 20, 65);
    }

    private Rectangle hudRect() {
        int x = (WIDTH - HUD_W) / 2;
        int y = HEIGHT - HUD_H - HUD_MARGIN_BOTTOM;
        return new Rectangle(x, y, HUD_W, HUD_H);
    }

    private void drawHudBox(Graphics2D g2d, Rectangle r, int alphaFill, int alphaBorder) {
        // เงาอ่อนๆ
        g2d.setColor(new Color(0, 0, 0, 60));
        g2d.fillRoundRect(r.x + 2, r.y + 4, r.width, r.height, HUD_RADIUS + 4, HUD_RADIUS + 4);

        // กล่องโปร่งแสง
        g2d.setColor(new Color(20, 20, 25, Math.max(0, Math.min(255, alphaFill))));
        g2d.fillRoundRect(r.x, r.y, r.width, r.height, HUD_RADIUS, HUD_RADIUS);

        // เส้นขอบโปร่ง ๆ
        g2d.setColor(new Color(255, 255, 255, Math.max(0, Math.min(255, alphaBorder))));
        g2d.drawRoundRect(r.x, r.y, r.width, r.height, HUD_RADIUS, HUD_RADIUS);
    }

    // วาดข้อความลอยด้านบน (โปร่งแสง + เงาเล็กน้อย) โดยไม่มีกล่องพื้นหลัง
    private void drawTopHint(Graphics2D g2d, String text) {
        Font font = new Font("Arial", Font.BOLD, 18);
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int textW = fm.stringWidth(text);
        int x = (WIDTH - textW) / 2;
        int y = 42; // ระยะจากขอบบน

        // เงาเบาๆ
        g2d.setColor(new Color(0, 0, 0, 110));
        g2d.drawString(text, x + 1, y + 1);

        // ตัวอักษรโปร่งแสง
        g2d.setColor(new Color(255, 255, 255, 230));
        g2d.drawString(text, x, y);
    }

    private void startFishing() {
        if (player.y < HEIGHT - 200) {
            JOptionPane.showMessageDialog(this, "ต้องอยู่ใกล้น้ำเพื่อตกปลา!");
            return;
        }

        gameState = GameState.CASTING;
        Fish randomFish = Fish.getRandomFish();
        fishingSequence = new FishingSequence(randomFish);
        reelMinigame = null;
        
        javax.swing.Timer timer = new javax.swing.Timer(50, e -> {
            fishingSequence.update();

            // เมื่อถึงเวลา ปลากัด -> FishingSequence จะเปลี่ยนเฟสเป็น REELING เอง
            if (fishingSequence.phase == FishingPhase.REELING && reelMinigame == null) {
                reelMinigame = new ReelMinigame();
                gameState = GameState.REELING;
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
                    // รอเฉยๆ ไม่ต้องกดอะไร
                    break;
                case RESULT:
                case INVENTORY:
                    gameState = GameState.EXPLORATION;
                    break;
                case REELING:
                    if (reelMinigame != null) reelMinigame.press();
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
        if (e.getKeyCode() == KeyEvent.VK_SPACE && gameState == GameState.REELING) {
            if (reelMinigame != null) reelMinigame.release();
        }
    }

    @Override public void keyTyped(KeyEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        // ไม่ใช้เมาส์เพื่อ tension แล้วใน REELING ใหม่นี้
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (gameState == GameState.REELING && e.getButton() == MouseEvent.BUTTON1) {
            if (reelMinigame != null) reelMinigame.press();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (gameState == GameState.REELING && e.getButton() == MouseEvent.BUTTON1) {
            if (reelMinigame != null) reelMinigame.release();
        }
    }

    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    
    private static boolean overlaps(double pbCenter, double pbWidth, double fishCenter, double fishWidth) {
        double pbHalf = pbWidth / 2.0;
        double fishHalf = fishWidth / 2.0;
        double pbLeft = pbCenter - pbHalf;
        double pbRight = pbCenter + pbHalf;
        double fishLeft = fishCenter - fishHalf;
        double fishRight = fishCenter + fishHalf;
        return fishRight > pbLeft && fishLeft < pbRight;
    }
}