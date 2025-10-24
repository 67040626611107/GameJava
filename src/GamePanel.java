import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import sprites.NPCSpriteSheet;
import map.MapBackground;
import map.WorldObject;
import map.CollisionWorld;

// Quest
import quest.QuestManager;

public class GamePanel extends JPanel implements KeyListener, MouseMotionListener, MouseListener {
    private Player player;
    private GameState gameState;
    private FishingSequence fishingSequence;
    private java.util.List<Fish> caughtFish;
    private Set<Integer> keysPressed = new HashSet<>();
    private BufferedImage spriteSheet;
    private CharacterConfig currentCharacter;

    private static final int WIDTH = 1400;
    private static final int HEIGHT = 800;

    // Map
    private MapBackground mapBg;
    private int waterTopY;
    private CollisionWorld collisionWorld;
    private java.util.List<WorldObject> worldObjects = new ArrayList<>();
    private int worldW, worldH; // ขนาดโลก

    // Reel
    private ReelMinigame reelMinigame;

    // Quests
    private final QuestManager questManager = new QuestManager();

    // Waves
    private BufferedImage waveTile;

    // กันขอบน้ำ
    private static final int FOOT_MARGIN = 6;

    // HUD constants (สำหรับ overlay)
    private static final int HUD_W = 860;
    private static final int HUD_H = 160;
    private static final int HUD_MARGIN_BOTTOM = 40;
    private static final int HUD_RADIUS = 16;
    private static final int HUD_ALPHA = 140;
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

        currentCharacter = new CharacterConfig("Fisherman_Fin", "src/assets/Cute_Fantasy/NPCs (Premade)/Fisherman_Fin.png", 64);
        player.setCharacter(currentCharacter);

        loadSpriteSheetLegacyIfAny();
        loadWaveTile();

        mapBg = new MapBackground(WIDTH, HEIGHT, "src/assets/Cute_Fantasy/manifest.files.json");
        waterTopY = mapBg.getWaterTopY();
        collisionWorld = mapBg.getCollisionWorld();
        worldObjects = new ArrayList<>(mapBg.getObjects());
        worldW = mapBg.getWorldWidth();
        worldH = mapBg.getWorldHeight();

        questManager.load("src/assets/quests.json");

        new javax.swing.Timer(50, e -> update()).start();
    }

    private void loadSpriteSheetLegacyIfAny() {
        try {
            File file = new File("spritesheet.png");
            if (!file.exists()) file = new File("src/spritesheet.png");
            if (!file.exists()) file = new File("./src/spritesheet.png");
            if (file.exists()) {
                spriteSheet = ImageIO.read(file);
                System.out.println("✅ โหลด spritesheet สำเร็จจาก: " + file.getAbsolutePath());
            } else {
                spriteSheet = null;
            }
        } catch (Exception e) {
            spriteSheet = null;
        }
    }

    private void loadWaveTile() {
        try {
            File f1 = new File("src/assets/waves/water_wave_row_60x30.png");
            File f2 = new File("src/assets/water_wave_row_60x30.png");
            File use = f1.exists() ? f1 : (f2.exists() ? f2 : null);
            if (use != null) {
                waveTile = ImageIO.read(use);
                System.out.println("✅ โหลด wave tile: " + use.getAbsolutePath());
            }
        } catch (Exception ignored) {}
    }

    void setCharacter(CharacterConfig config) {
        currentCharacter = config;
        player.setCharacter(config);
        requestFocusInWindow();
    }

    private void update() {
        if (gameState == GameState.EXPLORATION) {
            int speed = player.getSpeed();
            int dx = 0, dy = 0;
            if (keysPressed.contains(KeyEvent.VK_LEFT))  dx -= speed;
            if (keysPressed.contains(KeyEvent.VK_RIGHT)) dx += speed;
            if (keysPressed.contains(KeyEvent.VK_UP))    dy -= speed;
            if (keysPressed.contains(KeyEvent.VK_DOWN))  dy += speed;

            // ตั้งทิศ/สถานะเพื่อเล่นอนิเมชัน
            if (dx == 0 && dy == 0) {
                player.setMoving(false);
            } else {
                if (Math.abs(dy) >= Math.abs(dx)) {
                    player.setDirection(dy < 0 ? Player.Direction.UP : Player.Direction.DOWN);
                } else {
                    player.setDirection(dx < 0 ? Player.Direction.LEFT : Player.Direction.RIGHT);
                }
                player.setMoving(true);
            }

            // ทดสอบชนทีละแกน + ขอบโลก + น้ำ
            if (dx != 0) {
                Rectangle feetNextX = playerFeetAt(player.x + dx, player.y);
                boolean blockX = collisionWorld != null && collisionWorld.blocks(feetNextX);
                boolean outX = feetNextX.x < 0 || (feetNextX.x + feetNextX.width) > worldW;
                if (!blockX && !outX) {
                    player.x += dx;
                }
            }
            if (dy != 0) {
                int candidateY = player.y + dy;
                Rectangle feetNextY = playerFeetAt(player.x, candidateY);
                boolean blockY = collisionWorld != null && collisionWorld.blocks(feetNextY);
                boolean outY = feetNextY.y < 0 || (feetNextY.y + feetNextY.height) > worldH;
                boolean hitsWater = candidateY > (waterTopY - FOOT_MARGIN);
                if (!blockY && !outY && !hitsWater) {
                    player.y = candidateY;
                }
            }

            // คลัมป์ตำแหน่งปลอดภัย
            player.x = Math.max(16, Math.min(worldW - 16, player.x));
            player.y = Math.max(6,  Math.min(worldH - 6,  player.y));
        }

        // REEL mini-game update
        if (gameState == GameState.REELING && reelMinigame != null && fishingSequence != null && !reelMinigame.isFinished()) {
            int barWidth = HUD_W - 80;
            reelMinigame.update(0.05, barWidth);
            if (reelMinigame.isFinished()) {
                fishingSequence.reelingFinished = true;
                fishingSequence.success = reelMinigame.isSuccess();
            }
        }

        if (gameState == GameState.CASTING) {
            player.setOverrideAction(NPCSpriteSheet.Action.FISH_CAST);
        } else if (gameState == GameState.REELING) {
            player.setOverrideAction(NPCSpriteSheet.Action.FISH_REEL);
        } else {
            player.setOverrideAction(null);
        }

        repaint();
    }

    private Rectangle playerFeetAt(int px, int py) {
        return new Rectangle(px - 16, py - 6, 32, 12);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // วาดโลก
        BufferedImage ground = mapBg.getGroundImage();
        if (ground != null) g2d.drawImage(ground, 0, 0, null);

        if (waveTile != null) {
            for (int i = 0; i < WIDTH; i += waveTile.getWidth()) {
                g2d.drawImage(waveTile, i, waterTopY, waveTile.getWidth(), waveTile.getHeight(), null);
            }
        }

        ArrayList<WorldObject> sorted = new ArrayList<>(worldObjects);
        sorted.sort(Comparator.comparingInt(WorldObject::footY));
        int playerFoot = player.y;

        for (WorldObject o : sorted) if (o.footY() < playerFoot) o.draw(g2d);
        player.draw(g2d, spriteSheet, this);
        for (WorldObject o : sorted) if (o.footY() >= playerFoot) o.draw(g2d);

        // วาด overlay ตามสถานะตกปลา
        switch (gameState) {
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
            default:
        }

        // UI มุมซ้ายบน + Quest HUD
        drawUI(g2d);
        questManager.draw(g2d, WIDTH, HEIGHT);
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, 420, 100);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("เงิน: " + player.money + " บาท", 20, 35);
        g2d.drawString("ปลาที่ตกได้: " + (caughtFish == null ? 0 : caughtFish.size()), 20, 65);
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString("กด C: เลือกตัวละคร (NPC) | SPACE: ตกปลา", 220, 35);
    }

    // -------- Fishing overlays --------
    private Rectangle hudRect() {
        int x = (WIDTH - HUD_W) / 2;
        int y = HEIGHT - HUD_H - HUD_MARGIN_BOTTOM;
        return new Rectangle(x, y, HUD_W, HUD_H);
    }

    private void drawHudBox(Graphics2D g2d, Rectangle r, int alphaFill, int alphaBorder) {
        g2d.setColor(new Color(0, 0, 0, 60));
        g2d.fillRoundRect(r.x + 2, r.y + 4, r.width, r.height, HUD_RADIUS + 4, HUD_RADIUS + 4);

        g2d.setColor(new Color(20, 20, 25, Math.max(0, Math.min(255, alphaFill))));
        g2d.fillRoundRect(r.x, r.y, r.width, r.height, HUD_RADIUS, HUD_RADIUS);

        g2d.setColor(new Color(255, 255, 255, Math.max(0, Math.min(255, alphaBorder))));
        g2d.drawRoundRect(r.x, r.y, r.width, r.height, HUD_RADIUS, HUD_RADIUS);
    }

    private void drawTopHint(Graphics2D g2d, String text) {
        Font font = new Font("Arial", Font.BOLD, 18);
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int textW = fm.stringWidth(text);
        int x = (WIDTH - textW) / 2;
        int y = 42;

        g2d.setColor(new Color(0, 0, 0, 110));
        g2d.drawString(text, x + 1, y + 1);

        g2d.setColor(new Color(255, 255, 255, 230));
        g2d.drawString(text, x, y);
    }

    private void drawCasting(Graphics2D g2d) {
        Rectangle hud = hudRect();
        drawHudBox(g2d, hud, HUD_ALPHA, HUD_BORDER_ALPHA);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        g2d.drawString("ขั้นที่ 1: รอปลากินเหยื่อ", hud.x + 24, hud.y + 42);

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

        g2d.setColor(new Color(240, 240, 240));
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.drawString("กำลังรอ... เมื่อปลากัด จะเข้าสู่ REELING อัตโนมัติ", hud.x + 24, hud.y + 110);
    }

    private void drawReeling(Graphics2D g2d) {
        if (reelMinigame == null) {
            reelMinigame = new ReelMinigame();
        }

        drawTopHint(g2d, "ค้าง SPACE หรือ คลิกเมาส์ซ้ายค้าง เพื่อดึง");

        Rectangle hud = hudRect();
        drawHudBox(g2d, hud, HUD_ALPHA, HUD_BORDER_ALPHA);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        g2d.drawString("ขั้นที่ 2: REELING", hud.x + 24, hud.y + 42);

        // แถบควบคุม
        int innerPad = 24;
        int barX = hud.x + innerPad;
        int barY = hud.y + 70;
        int barWidth = hud.width - innerPad * 2;
        int barHeight = 36;

        g2d.setColor(new Color(0,0,0,120));
        g2d.fillRoundRect(barX, barY, barWidth, barHeight, 12, 12);
        g2d.setColor(new Color(0,0,0,160));
        g2d.drawRoundRect(barX, barY, barWidth, barHeight, 12, 12);

        // player bar
        int pbW = (int)Math.round(barWidth * reelMinigame.getControlWidth());
        int pbX = (int)Math.round(barX + reelMinigame.getPlayerBarCenter() * barWidth - pbW/2.0);
        g2d.setColor(new Color(255,255,255,200));
        g2d.fillRoundRect(pbX, barY, pbW, barHeight, 10, 10);

        // fish marker
        int fishPx = (int)Math.round(barX + reelMinigame.getFishCenter()*barWidth - 4);
        g2d.setColor(new Color(67,75,91));
        g2d.fillRoundRect(fishPx, barY, 8, barHeight, 10, 10);

        // progress bar
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
    }

    private void drawResult(Graphics2D g2d) {
        if (fishingSequence == null) return;

        if (fishingSequence.success) {
            g2d.setColor(new Color(0, 200, 0, 120));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            g2d.drawString("ตกปลาสำเร็จ!", WIDTH / 2 - 180, 150);

            Fish caughtFishObj = fishingSequence.caughtFish;
            g2d.setFont(new Font("Arial", Font.PLAIN, 28));
            g2d.drawString("ชนิด: " + caughtFishObj.name, WIDTH / 2 - 120, 300);
            g2d.drawString("ราคา: " + caughtFishObj.price + " บาท", WIDTH / 2 - 120, 350);
        } else {
            g2d.setColor(new Color(200, 0, 0, 120));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            g2d.drawString("ปลาหนีไป!", WIDTH / 2 - 150, 200);
        }

        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.drawString("กดเว้นวรรค เพื่อกลับเมนู", WIDTH / 2 - 150, 550);
    }

    private void drawInventory(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 32));
        g2d.drawString("Inventory", WIDTH / 2 - 80, 50);

        int y = 120;
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        java.util.Map<String, Integer> fishCount = new java.util.HashMap<>();
        for (Fish fish : caughtFish) {
            fishCount.put(fish.name, fishCount.getOrDefault(fish.name, 0) + 1);
        }

        for (java.util.Map.Entry<String, Integer> entry : fishCount.entrySet()) {
            g2d.drawString(entry.getKey() + " x" + entry.getValue(), 100, y);
            y += 40;
        }

        if (caughtFish.isEmpty()) {
            g2d.drawString("ยังไม่มีปลา", 100, 150);
        }

        g2d.drawString("กดเว้นวรรค เพื่อกลับเมนู", WIDTH / 2 - 150, HEIGHT - 50);
    }

    // --- Key / Mouse handlers ---
    @Override public void keyPressed(KeyEvent e) {
        keysPressed.add(e.getKeyCode());
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            switch (gameState) {
                case EXPLORATION:
                    startFishing();
                    break;
                case CASTING:
                    break;
                case SNAG:
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
        if (e.getKeyCode() == KeyEvent.VK_C && gameState == GameState.EXPLORATION) {
            JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "เลือกตัวละคร (NPC)", Dialog.ModalityType.MODELESS);
            dlg.setContentPane(new CharacterSelectPanel(this));
            dlg.pack();
            dlg.setLocationRelativeTo(this);
            dlg.setVisible(true);
        }
    }
    @Override public void keyReleased(KeyEvent e) {
        keysPressed.remove(e.getKeyCode());
        if (e.getKeyCode() == KeyEvent.VK_SPACE && gameState == GameState.REELING) {
            if (reelMinigame != null) reelMinigame.release();
        }
    }
    @Override public void keyTyped(KeyEvent e) {}

    @Override public void mouseMoved(MouseEvent e) {}
    @Override public void mouseDragged(MouseEvent e) {}
    @Override public void mousePressed(MouseEvent e) {
        if (gameState == GameState.REELING && e.getButton() == MouseEvent.BUTTON1) {
            if (reelMinigame != null) reelMinigame.press();
        }
        if (questManager.handleClick(e.getX(), e.getY())) repaint();
    }
    @Override public void mouseReleased(MouseEvent e) {
        if (gameState == GameState.REELING && e.getButton() == MouseEvent.BUTTON1) {
            if (reelMinigame != null) reelMinigame.release();
        }
    }
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    // -------- Fishing flow --------
    private void startFishing() {
        boolean onShore = (player.y >= waterTopY - 28) && (player.y <= waterTopY - FOOT_MARGIN);
        if (!onShore) {
            JOptionPane.showMessageDialog(this, "ต้องยืนที่ริมฝั่ง (ชายหาด) ใกล้น้ำเพื่อตกปลา!");
            return;
        }
        gameState = GameState.CASTING;
        Fish randomFish = Fish.getRandomFish();
        fishingSequence = new FishingSequence(randomFish);
        reelMinigame = null;

        javax.swing.Timer timer = new javax.swing.Timer(50, e -> {
            fishingSequence.update();
            if (fishingSequence.phase == FishingPhase.REELING && reelMinigame == null) {
                reelMinigame = new ReelMinigame();
                gameState = GameState.REELING;
            } else if (fishingSequence.phase == FishingPhase.REELING && fishingSequence.reelingFinished) {
                ((javax.swing.Timer) e.getSource()).stop();
                gameState = GameState.RESULT;
                if (fishingSequence.success) {
                    if (caughtFish == null) caughtFish = new ArrayList<>();
                    caughtFish.add(fishingSequence.caughtFish);
                    player.addMoney(fishingSequence.caughtFish.price);
                    questManager.onFishCaught(fishingSequence.caughtFish.name, fishingSequence.caughtFish.golden);
                    java.util.List<quest.QuestManager.GoalPayout> pays = questManager.collectNewPayouts();
                    for (quest.QuestManager.GoalPayout p : pays) if (p.money > 0) player.addMoney(p.money);
                }
            }
            repaint();
        });
        timer.start();
    }
}