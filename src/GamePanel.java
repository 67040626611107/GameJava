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
import map.MapData;
import map.MapIO;

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

    private static final String MANIFEST_JSON = "src/assets/Cute_Fantasy/manifest.files.json";
    private String assetRoot = "src/assets/Cute_Fantasy"; 

    private MapBackground mapBg;
    private int waterTopY;
    private CollisionWorld collisionWorld;
    private java.util.List<WorldObject> worldObjects = new ArrayList<>();
    private int worldW, worldH; 

    private BufferedImage generatedWorldBg = null;
    private GameplayTuning.MapSpec currentMapSpec = null;
    private Rectangle pondRectPx = null; 

    private static final String WORLD2_MAP_PATH = "src/assets/maps/map_world2.json"; 
    private final java.util.List<World2Obj> world2Objects = new java.util.ArrayList<>();
    private final java.util.Map<String, BufferedImage> world2ImageCache = new java.util.HashMap<>();

    private ReelMinigame reelMinigame;

    private final QuestManager questManager = new QuestManager();

    private BufferedImage waveTile;

    private static final int FOOT_MARGIN = 6;

    private static final int HUD_W = 860;
    private static final int HUD_H = 160;
    private static final int HUD_MARGIN_BOTTOM = 40;
    private static final int HUD_RADIUS = 16;
    private static final int HUD_ALPHA = 140;
    private static final int HUD_BORDER_ALPHA = 190;

    private int currentWorldId = 1;

    private final java.util.Map<String, BufferedImage> fishImageCache = new java.util.HashMap<>();

    private double reelBarScaleUI = 1.0;

    private final java.util.LinkedHashMap<Integer, String> rodIndexToId = new java.util.LinkedHashMap<>();
    private final java.util.Set<String> ownedRods = new java.util.HashSet<>();
    private String currentRodId = "starter_rod";

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(34, 139, 34));
        setFocusable(true);
        addKeyListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);

        String detected = detectAssetRoot(MANIFEST_JSON);
        if (detected != null && !detected.isEmpty()) {
            assetRoot = detected;
        }
        System.out.println("ℹ️ assetRoot = " + assetRoot);

        GameplayTuning.loadAll();

        int idx = 1;
        for (GameplayTuning.RodParams r : GameplayTuning.rods()) {
            if (idx <= 9) {
                rodIndexToId.put(idx, r.id);
                if ("starter_rod".equals(r.id)) ownedRods.add(r.id);
            }
            idx++;
        }
        if (!ownedRods.contains(currentRodId)) currentRodId = rodIndexToId.getOrDefault(1, "starter_rod");
        ownedRods.add(currentRodId);

        player = new Player(WIDTH / 2, HEIGHT / 2);
        gameState = GameState.EXPLORATION;
        caughtFish = new ArrayList<>();
        fishingSequence = null;

        currentCharacter = new CharacterConfig("Fisherman_Fin", "src/assets/Cute_Fantasy/NPCs (Premade)/Fisherman_Fin.png", 64);
        player.setCharacter(currentCharacter);

        loadSpriteSheetLegacyIfAny();
        loadWaveTile();

        mapBg = new MapBackground(WIDTH, HEIGHT, MANIFEST_JSON);
        waterTopY = mapBg.getWaterTopY();
        collisionWorld = mapBg.getCollisionWorld();
        worldObjects = new ArrayList<>(mapBg.getObjects());
        worldW = mapBg.getWorldWidth();
        worldH = mapBg.getWorldHeight();

        questManager.load("src/assets/quests.json");

        refreshWorldVisuals();

        loadWorld2ObjectsIfNeeded();

        new javax.swing.Timer(50, e -> update()).start();
    }

    private String detectAssetRoot(String manifestPath) {
        try {
            File f = resolveFileLoose(manifestPath);
            if (f == null || !f.exists()) return null;
            String json = java.nio.file.Files.readString(f.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            java.util.regex.Matcher mRoot = java.util.regex.Pattern
                    .compile("\"root\"\\s*:\\s*\"([^\"]+)\"")
                    .matcher(json);
            if (mRoot.find()) {
                String root = mRoot.group(1).replace("\\\\", "\\").replace("\\", "/");
                if (!root.endsWith("/") && !root.endsWith("\\")) root = root + "/";
                System.out.println("✅ manifest.root = " + root);
                return root;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private File resolveFileLoose(String p) {
        File f0 = new File(p);
        File f1 = new File("src/" + p);
        File f2 = new File("./" + p);
        if (f0.exists()) return f0;
        if (f1.exists()) return f1;
        if (f2.exists()) return f2;
        return null;
    }

    private void loadSpriteSheetLegacyIfAny() {
        try {
            File file = resolveFileLoose("spritesheet.png");
            if (file == null) file = resolveFileLoose("src/spritesheet.png");
            if (file != null && file.exists()) {
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
            File use = resolveFileLoose("src/assets/waves/water_wave_row_60x30.png");
            if (use == null) use = resolveFileLoose("src/assets/water_wave_row_60x30.png");
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

    private GameplayTuning.WorldParams currentWorld() {
        return GameplayTuning.world(currentWorldId);
    }

    private GameplayTuning.CharStats currentCharStats() {
        String id = currentCharacter != null ? currentCharacter.displayName : "starter";
        return GameplayTuning.charStats(id);
    }

    private GameplayTuning.RodParams currentRod() {
        GameplayTuning.RodParams r = GameplayTuning.rod(currentRodId);
        if (r == null) r = GameplayTuning.rod("starter_rod");
        return r;
    }

    private void update() {
        boolean centerLake = currentWorld().map != null && currentWorld().map.centerWater;

        if (gameState == GameState.EXPLORATION) {
            int speed = player.getSpeed();
            int dx = 0, dy = 0;
            if (keysPressed.contains(KeyEvent.VK_LEFT))  dx -= speed;
            if (keysPressed.contains(KeyEvent.VK_RIGHT)) dx += speed;
            if (keysPressed.contains(KeyEvent.VK_UP))    dy -= speed;
            if (keysPressed.contains(KeyEvent.VK_DOWN))  dy += speed;

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

            if (dx != 0) {
                Rectangle feetNextX = playerFeetAt(player.x + dx, player.y);
                boolean blockX = false;
                if (!centerLake && collisionWorld != null) {
                    blockX = collisionWorld.blocks(feetNextX);
                } else {
                    blockX = world2Blocks(feetNextX) || blockX;
                }
                boolean outX = feetNextX.x < 0 || (feetNextX.x + feetNextX.width) > worldW;
                boolean inWaterX = isInWater(feetNextX);
                if (!blockX && !outX && !inWaterX) {
                    player.x += dx;
                }
            }
            if (dy != 0) {
                int candidateY = player.y + dy;
                Rectangle feetNextY = playerFeetAt(player.x, candidateY);
                boolean blockY = false;
                if (!centerLake && collisionWorld != null) {
                    blockY = collisionWorld.blocks(feetNextY);
                } else {
                    blockY = world2Blocks(feetNextY) || blockY;
                }
                boolean outY = feetNextY.y < 0 || (feetNextY.y + feetNextY.height) > worldH;
                boolean inWaterY = isInWater(feetNextY);
                if (!blockY && !outY && !inWaterY) {
                    player.y = candidateY;
                }
            }


            player.x = Math.max(16, Math.min(worldW - 16, player.x));
            player.y = Math.max(6,  Math.min(worldH - 6,  player.y));
        }


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

    private Rectangle expand(Rectangle r, int m) {
        return new Rectangle(r.x - m, r.y - m, r.width + m * 2, r.height + m * 2);
    }
    private Rectangle shrink(Rectangle r, int m) {
        return new Rectangle(r.x + m, r.y + m, Math.max(0, r.width - m * 2), Math.max(0, r.height - m * 2));
    }

    private boolean isInWater(Rectangle feetRect) {
        GameplayTuning.WorldParams wp = currentWorld();
        if (wp.map != null && wp.map.centerWater && pondRectPx != null) {
            Rectangle inside = shrink(pondRectPx, 4);
            return feetRect.intersects(inside);
        } else {
            int candidateY = feetRect.y + feetRect.height;
            return candidateY > (waterTopY - FOOT_MARGIN);
        }
    }

    private boolean isNearWater(int px, int py) {
        GameplayTuning.WorldParams wp = currentWorld();
        if (wp.map != null && wp.map.centerWater && pondRectPx != null) {
            Rectangle feet = playerFeetAt(px, py);
            Rectangle inside = shrink(pondRectPx, 4);
            if (feet.intersects(inside)) return false;
            Rectangle near = expand(pondRectPx, 10);
            return feet.intersects(near);
        } else {
            return (py >= waterTopY - 28) && (py <= waterTopY - FOOT_MARGIN);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        GameplayTuning.WorldParams wp = currentWorld();
        boolean useCenterLake = wp.map != null && wp.map.centerWater;

        if (useCenterLake && generatedWorldBg != null) {
            g2d.drawImage(generatedWorldBg, 0, 0, null);
        } else {
            BufferedImage ground = mapBg.getGroundImage();
            if (ground != null) g2d.drawImage(ground, 0, 0, null);
        }


        if (!useCenterLake && waveTile != null) {
            for (int i = 0; i < WIDTH; i += waveTile.getWidth()) {
                g2d.drawImage(waveTile, i, waterTopY, waveTile.getWidth(), waveTile.getHeight(), null);
            }
        }

        int playerFoot = player.y;

        if (!useCenterLake) {
            ArrayList<WorldObject> sorted = new ArrayList<>(worldObjects);
            sorted.sort(Comparator.comparingInt(WorldObject::footY));
            for (WorldObject o : sorted) if (o.footY() < playerFoot) o.draw(g2d);
            player.draw(g2d, spriteSheet, this);
            for (WorldObject o : sorted) if (o.footY() >= playerFoot) o.draw(g2d);
        } else {
            drawWorld2Before(g2d, playerFoot);
            player.draw(g2d, spriteSheet, this);
            drawWorld2After(g2d, playerFoot);
        }

        switch (gameState) {
            case CASTING -> drawCasting(g2d);
            case REELING -> drawReeling(g2d);
            case RESULT -> drawResult(g2d);
            case INVENTORY -> drawInventory(g2d);
            default -> { }
        }

        drawUI(g2d);
        questManager.draw(g2d, WIDTH, HEIGHT);
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, 420, 110);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("เงิน: " + player.money + " บาท", 20, 35);
        g2d.drawString("ปลาที่ตกได้: " + (caughtFish == null ? 0 : caughtFish.size()), 20, 65);
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString("กด C: เลือกตัวละคร (NPC) | SPACE: ตกปลา", 220, 35);
        g2d.drawString("World: " + currentWorld().name + " (คลิกกรอบนี้เพื่อสลับ | P เลือก)", 220, 65);

        GameplayTuning.RodParams r = currentRod();
        g2d.drawString("Rod: " + (r != null ? r.displayName : "None"), 20, 95);
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
            if (fishingSequence != null && fishingSequence.caughtFish != null) {
                setupReelForCurrent(fishingSequence.caughtFish);
            } else {
                reelMinigame.setResilience(0.9);
                reelMinigame.setProgressEfficiency(1.0);
                reelMinigame.setControlWidth(0.18);
                reelMinigame.setMovementFactor(1.0);
                reelBarScaleUI = 1.0;
            }
        }

        drawTopHint(g2d, "ค้าง SPACE หรือ คลิกเมาส์ซ้ายค้าง เพื่อดึง");

        Rectangle hud = hudRect();
        drawHudBox(g2d, hud, HUD_ALPHA, HUD_BORDER_ALPHA);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        g2d.drawString("ขั้นที่ 2: REELING", hud.x + 24, hud.y + 42);

        int innerPad = 24;
        int barX = hud.x + innerPad;
        int barY = hud.y + 70;
        int barWidth = hud.width - innerPad * 2;
        int barHeight = (int)Math.round(36 * Math.max(0.75, Math.min(1.8, reelBarScaleUI)));

        g2d.setColor(new Color(0,0,0,120));
        g2d.fillRoundRect(barX, barY, barWidth, barHeight, 12, 12);
        g2d.setColor(new Color(0,0,0,160));
        g2d.drawRoundRect(barX, barY, barWidth, barHeight, 12, 12);

        int pbW = (int)Math.round(barWidth * reelMinigame.getControlWidth());
        int pbX = (int)Math.round(barX + reelMinigame.getPlayerBarCenter() * barWidth - pbW/2.0);
        g2d.setColor(new Color(255,255,255,200));
        g2d.fillRoundRect(pbX, barY, pbW, barHeight, 10, 10);

        int fishPx = (int)Math.round(barX + reelMinigame.getFishCenter()*barWidth - 4);
        g2d.setColor(new Color(67,75,91));
        g2d.fillRoundRect(fishPx, barY, 8, barHeight, 10, 10);

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

        g2d.setColor(new Color(0, fishingSequence.success ? 200 : 0, fishingSequence.success ? 0 : 200, 120));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        String title = fishingSequence.success ? "ตกปลาสำเร็จ!" : "ปลาหนีไป!";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (WIDTH - fm.stringWidth(title)) / 2, 150);

        if (fishingSequence.success) {
            Fish f = fishingSequence.caughtFish;

            int textX = WIDTH / 2 - 260;
            int y1 = 280;
            g2d.setFont(new Font("Arial", Font.PLAIN, 28));
            g2d.drawString("ชนิด: " + f.name, textX, y1);
            g2d.drawString("ราคา: " + f.price + " บาท", textX, y1 + 50);

            int imgW = 140, imgH = 140;
            int imgX = WIDTH / 2 + 220;
            int imgY = y1 - imgH + 20;
            drawFishPreviewBox(g2d, f, imgX, imgY, imgW, imgH);
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
        java.util.Map<String, java.util.List<Fish>> fishByName = new java.util.HashMap<>();
        for (Fish fish : caughtFish) {
            fishByName.computeIfAbsent(fish.name, k -> new java.util.ArrayList<>()).add(fish);
        }

        int xText = 240;
        int perRow = 3;
        int col = 0;
        int rowY = y;

        for (java.util.Map.Entry<String, java.util.List<Fish>> entry : fishByName.entrySet()) {
            Fish any = entry.getValue().get(0);
            int px = 80 + col * 160;
            int py = rowY;
            drawFishPreviewTile(g2d, any, px, py, 120, 80);
            g2d.setColor(Color.WHITE);
            g2d.drawString(entry.getKey() + " x" + entry.getValue().size(), xText, py + 28);
            g2d.drawString("ราคา/ชิ้น: " + any.price, xText, py + 56);

            col++;
            if (col >= perRow) {
                col = 0;
                rowY += 100;
            }
        }

        if (caughtFish.isEmpty()) {
            g2d.drawString("ยังไม่มีปลา", 100, 150);
        }

        drawRodShop(g2d);

        g2d.drawString("กด S เพื่อขายทั้งหมด | กดเลข 1-9 เพื่อ ซื้อ/ใส่ คันเบ็ด", WIDTH / 2 - 240, HEIGHT - 80);
        g2d.drawString("กดเว้นวรรค เพื่อกลับเมนู", WIDTH / 2 - 150, HEIGHT - 50);
    }

    private void drawRodShop(Graphics2D g2d) {
        int panelX = WIDTH - 420;
        int panelY = 90;
        int panelW = 360;
        int panelH = HEIGHT - 180;

        g2d.setColor(new Color(0, 0, 0, 140));
        g2d.fillRoundRect(panelX, panelY, panelW, panelH, 16, 16);
        g2d.setColor(new Color(255, 255, 255, 180));
        g2d.drawRoundRect(panelX, panelY, panelW, panelH, 16, 16);

        g2d.setFont(new Font("Arial", Font.BOLD, 22));
        g2d.drawString("Rod Shop", panelX + 16, panelY + 32);

        int y = panelY + 60;
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));

        for (int i = 1; i <= 9; i++) {
            String rodId = rodIndexToId.get(i);
            if (rodId == null) break;
            GameplayTuning.RodParams r = GameplayTuning.rod(rodId);
            String owned = ownedRods.contains(rodId) ? (rodId.equals(currentRodId) ? " (Equipped)" : " (Owned)") : "";
            g2d.setColor(Color.WHITE);
            g2d.drawString(i + ". " + r.displayName + owned, panelX + 16, y);
            g2d.setColor(new Color(220, 220, 220));
            g2d.drawString("ราคา: " + r.price, panelX + 220, y);

            y += 18;
            g2d.setColor(new Color(190, 255, 190));
            g2d.drawString("+Bar: " + (int)Math.round(r.reelBarScaleBonus * 100) + "%, +Luck: " + (int)Math.round(r.luckBonus * 100) + "%", panelX + 16, y);
            y += 18;
            g2d.setColor(new Color(190, 220, 255));
            g2d.drawString("+Bite: " + (int)Math.round(r.biteSpeedBonus * 100) + "%, +Golden: " + (int)Math.round(r.goldenChanceBonus * 100) + "%", panelX + 16, y);
            y += 26;
        }
    }

    private void drawFishPreviewBox(Graphics2D g2d, Fish fish, int x, int y, int w, int h) {
        BufferedImage img = loadFishImage(fish.imagePath);
        if (img != null) {
            g2d.drawImage(img, x, y, w, h, null);
            g2d.setColor(new Color(255,255,255,140));
            g2d.drawRoundRect(x, y, w, h, 10, 10);
        } else {
            g2d.setColor(new Color(255,255,255,40));
            g2d.fillRoundRect(x, y, w, h, 10, 10);
            g2d.setColor(new Color(255,255,255,140));
            g2d.drawRoundRect(x, y, w, h, 10, 10);
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            g2d.drawString("no preview", x + 12, y + h/2);
        }
    }

    private void drawFishPreviewTile(Graphics2D g2d, Fish fish, int x, int y, int w, int h) {
        BufferedImage img = loadFishImage(fish.imagePath);
        g2d.setColor(new Color(0,0,0,120));
        g2d.fillRoundRect(x, y, w, h, 10, 10);
        g2d.setColor(new Color(255,255,255,80));
        g2d.drawRoundRect(x, y, w, h, 10, 10);
        int pad = 8;
        if (img != null) {
            g2d.drawImage(img, x + pad, y + pad, h - pad*2, h - pad*2, null);
        } else {
            g2d.setColor(new Color(255,255,255,40));
            g2d.fillRoundRect(x + pad, y + pad, h - pad*2, h - pad*2, 8, 8);
            g2d.setColor(new Color(255,255,255,140));
            g2d.drawString("no preview", x + pad + 6, y + h/2);
        }
    }

    private BufferedImage loadFishImage(String path) {
        if (path == null || path.isEmpty()) return null;
        try {
            if (fishImageCache.containsKey(path)) return fishImageCache.get(path);
            File use = resolveAssetFile(path);
            if (use == null) {
                System.out.println("⚠️ Fish image not found: " + path);
                fishImageCache.put(path, null);
                return null;
            }
            BufferedImage img = ImageIO.read(use);
            fishImageCache.put(path, img);
            return img;
        } catch (Exception e) {
            fishImageCache.put(path, null);
            return null;
        }
    }

    private File resolveAssetFile(String rel) {
        File f0 = new File(assetRoot, rel);
        if (f0.exists()) return f0;
        File f1 = new File("src/" + rel);
        if (f1.exists()) return f1;
        File f2 = new File(rel);
        if (f2.exists()) return f2;
        return null;
    }

    // --- World 2 object support ---

    private static final class World2Obj {
        String src;
        int x, y;
        boolean collide = true;
        int footH = 16;
    }

    private void loadWorld2ObjectsIfNeeded() {
        GameplayTuning.WorldParams wp = currentWorld();
        boolean centerLake = (wp != null && wp.map != null && wp.map.centerWater);
        world2Objects.clear();
        world2ImageCache.clear();
        if (!centerLake) return;

        try {
            File f = resolveFileLoose(WORLD2_MAP_PATH);
            if (f == null || !f.exists()) {
                System.out.println("⚠️ World2 map not found at: " + WORLD2_MAP_PATH);
                return;
            }
            String txt = java.nio.file.Files.readString(f.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            MapData data = MapIO.fromJson(txt);
            if (data == null || data.objects == null) return;

            for (MapData.MapObject o : data.objects) {
                if (o == null || o.src == null || o.src.isEmpty()) continue;
                World2Obj w = new World2Obj();
                w.src = o.src;
                w.x = o.x;
                w.y = o.y;
                w.collide = o.collide;
                w.footH = o.footH;
                world2Objects.add(w);
            }
            System.out.println("✅ World 2 objects loaded: " + world2Objects.size());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private boolean world2Blocks(Rectangle feetRect) {
        for (World2Obj o : world2Objects) {
            BufferedImage img = loadWorld2Image(o.src);
            Rectangle cr = world2Collider(o, img);
            if (cr != null && cr.intersects(feetRect)) return true;
        }
        return false;
    }

    private void drawWorld2Before(Graphics2D g2d, int playerFootY) {
        java.util.List<World2Obj> list = new ArrayList<>(world2Objects);
        list.sort(Comparator.comparingInt(o -> world2FootY(o, loadWorld2Image(o.src))));
        for (World2Obj o : list) {
            BufferedImage img = loadWorld2Image(o.src);
            if (world2FootY(o, img) < playerFootY) drawWorld2One(g2d, o, img);
        }
    }

    private void drawWorld2After(Graphics2D g2d, int playerFootY) {
        java.util.List<World2Obj> list = new ArrayList<>(world2Objects);
        list.sort(Comparator.comparingInt(o -> world2FootY(o, loadWorld2Image(o.src))));
        for (World2Obj o : list) {
            BufferedImage img = loadWorld2Image(o.src);
            if (world2FootY(o, img) >= playerFootY) drawWorld2One(g2d, o, img);
        }
    }

    private void drawWorld2One(Graphics2D g2d, World2Obj o, BufferedImage img) {
        if (img != null) {
            g2d.drawImage(img, o.x, o.y, null);
        } else {
            g2d.setColor(new Color(0,0,0,120));
            g2d.fillRect(o.x, o.y, 64, 64);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(o.x, o.y, 64, 64);
        }
    }

    private int world2FootY(World2Obj o, BufferedImage img) {
        int h = (img != null ? img.getHeight() : 64);
        return o.y + h;
    }

    private Rectangle world2Collider(World2Obj o, BufferedImage img) {
        if (!o.collide) return null;
        int w = (img != null ? img.getWidth() : 64);
        int h = (img != null ? img.getHeight() : 64);
        int fh = Math.max(4, o.footH);
        return new Rectangle(o.x, o.y + h - fh, Math.max(4, w), fh);
    }

    private BufferedImage loadWorld2Image(String src) {
        if (src == null || src.isEmpty()) return null;
        try {
            if (world2ImageCache.containsKey(src)) return world2ImageCache.get(src);
            File use = resolveAssetFile(src);
            if (use == null) {
                System.out.println("⚠️ World2 image not found: " + src + " (assetRoot=" + assetRoot + ")");
                world2ImageCache.put(src, null);
                return null;
            }
            BufferedImage img = ImageIO.read(use);
            world2ImageCache.put(src, img);
            return img;
        } catch (Exception e) {
            world2ImageCache.put(src, null);
            return null;
        }
    }

    // --- Key / Mouse handlers ---
    @Override public void keyPressed(KeyEvent e) {
        keysPressed.add(e.getKeyCode());
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            switch (gameState) {
                case EXPLORATION -> startFishing();
                case CASTING -> { }
                case SNAG -> { }
                case RESULT, INVENTORY -> gameState = GameState.EXPLORATION;
                case REELING -> { if (reelMinigame != null) reelMinigame.press(); }
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
        if (e.getKeyCode() == KeyEvent.VK_P && gameState == GameState.EXPLORATION) {
            openWorldSelectDialog();
        }
        if (e.getKeyCode() == KeyEvent.VK_S && gameState == GameState.INVENTORY) {
            int total = 0;
            for (Fish f : caughtFish) total += f.price;
            player.addMoney(total);
            caughtFish.clear();
            repaint();
        }
        if (gameState == GameState.INVENTORY && e.getKeyCode() >= KeyEvent.VK_1 && e.getKeyCode() <= KeyEvent.VK_9) {
            int num = e.getKeyCode() - KeyEvent.VK_1 + 1;
            String rodId = rodIndexToId.get(num);
            if (rodId != null) handleBuyOrEquipRod(rodId);
        }
        if (e.getKeyCode() == KeyEvent.VK_W && gameState == GameState.EXPLORATION) {
            toggleWorld(); 
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


        if (e.getButton() == MouseEvent.BUTTON1 && gameState == GameState.EXPLORATION) {
            if (e.getX() >= 0 && e.getX() <= 420 && e.getY() >= 0 && e.getY() <= 110) {
                toggleWorld();
            }
        }
    }
    @Override public void mouseReleased(MouseEvent e) {
        if (gameState == GameState.REELING && e.getButton() == MouseEvent.BUTTON1) {
            if (reelMinigame != null) reelMinigame.release();
        }
    }
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    private void toggleWorld() {
        int next = (currentWorldId == 1) ? 2 : 1;
        switchWorld(next);
    }

    private void openWorldSelectDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        WorldSelectDialog dlg = new WorldSelectDialog(owner, this::switchWorld);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void switchWorld(int newWorldId) {
        currentWorldId = newWorldId;

        refreshWorldVisuals();

        loadWorld2ObjectsIfNeeded();

        GameplayTuning.WorldParams wp = currentWorld();
        if (wp.map != null && wp.map.centerWater && pondRectPx != null) {
            player.x = pondRectPx.x - 40;
            player.y = pondRectPx.y + pondRectPx.height / 2;
        } else {
            player.x = WIDTH / 2;
            player.y = Math.max(6, waterTopY - FOOT_MARGIN - 2);
        }

        reelMinigame = null;
        if (gameState != GameState.EXPLORATION) gameState = GameState.EXPLORATION;
        repaint();
    }

private void refreshWorldVisuals() {
    GameplayTuning.WorldParams wp = currentWorld();
    currentMapSpec = (wp != null) ? wp.map : null;
    pondRectPx = null;

    if (currentMapSpec != null && currentMapSpec.centerWater) {
        final int TILE_SIZE_PX = 64;
        final int halfPx = Math.max(TILE_SIZE_PX, currentMapSpec.waterRadius * TILE_SIZE_PX);

        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setColor(new Color(60, 140, 60)); // ground
        g.fillRect(0, 0, WIDTH, HEIGHT);

        int cx = WIDTH / 2;
        int cy = HEIGHT / 2;
        int wx = cx - halfPx;
        int wy = cy - (int) Math.round(halfPx * 0.8);
        int ww = halfPx * 2;
        int wh = (int) Math.round(halfPx * 1.6);

        g.setColor(new Color(26, 168, 208));
        g.fillRect(wx, wy, ww, wh);
        g.setColor(new Color(255, 255, 255, 90));
        g.drawRect(wx, wy, ww, wh);

        g.dispose();
        generatedWorldBg = img;

        pondRectPx = new Rectangle(wx, wy, ww, wh);

        collisionWorld = null;
        worldObjects = new ArrayList<>();
        worldW = WIDTH;
        worldH = HEIGHT;

        System.out.println("World " + currentWorldId + " centerWater=true, waterRadius(tiles)="
                + currentMapSpec.waterRadius + ", halfPx=" + halfPx);
    } else {
        generatedWorldBg = null;
        pondRectPx = null;

        waterTopY = mapBg.getWaterTopY();
        collisionWorld = mapBg.getCollisionWorld();
        worldObjects = new ArrayList<>(mapBg.getObjects());
        worldW = mapBg.getWorldWidth();
        worldH = mapBg.getWorldHeight();
    }
}

    // -------- Fishing flow --------
    private void startFishing() {
        if (!isNearWater(player.x, player.y)) {
            JOptionPane.showMessageDialog(this, "ต้องยืนที่ริมฝั่งน้ำ เพื่อตกปลา!");
            return;
        }
        gameState = GameState.CASTING;

        Fish rolled = rollFishConsideringBonuses();
        fishingSequence = new FishingSequence(rolled, computeBiteTimeMs(rolled));
        reelMinigame = null;

        javax.swing.Timer timer = new javax.swing.Timer(50, e -> {
            fishingSequence.update();
            if (fishingSequence.phase == FishingPhase.REELING && reelMinigame == null) {
                reelMinigame = new ReelMinigame();
                setupReelForCurrent(rolled);
                gameState = GameState.REELING;
            } else if (fishingSequence.phase == FishingPhase.REELING && fishingSequence.reelingFinished) {
                ((javax.swing.Timer) e.getSource()).stop();
                gameState = GameState.RESULT;
                if (fishingSequence.success) {
                    if (caughtFish == null) caughtFish = new ArrayList<>();
                    caughtFish.add(fishingSequence.caughtFish);
                     // player.addMoney(fishingSequence.caughtFish.price);
                    questManager.onFishCaught(fishingSequence.caughtFish.name, fishingSequence.caughtFish.golden);
                    java.util.List<quest.QuestManager.GoalPayout> pays = questManager.collectNewPayouts();
                    for (quest.QuestManager.GoalPayout p : pays) if (p.money > 0) player.addMoney(p.money);
                }
            }
            repaint();
        });
        timer.start();
    }

    private int computeBiteTimeMs(Fish fish) {
        GameplayTuning.WorldParams wp = currentWorld();
        GameplayTuning.CharStats cs = currentCharStats();
        GameplayTuning.RodParams r = currentRod();
        double biteSpeed = fish.biteSpeedMul;
        double totalBonus = (cs.biteSpeedBonus) + (r != null ? r.biteSpeedBonus : 0.0);
        double totalMul = Math.max(0.25, 1.0 * (1.0 - totalBonus) / Math.max(0.25, biteSpeed));
        int castMs = (int)Math.round(wp.biteTimeBaseMs * totalMul +
                (Math.random() - 0.5) * 2 * wp.biteTimeVarianceMs * totalMul);
        return Math.max(500, castMs);
    }

    private void setupReelForCurrent(Fish fish) {
        GameplayTuning.WorldParams wp = currentWorld();
        GameplayTuning.CharStats cs = currentCharStats();
        GameplayTuning.RodParams r = currentRod();

        double progressEff = wp.reelProgressRate * fish.reelRateMul;
        double wiggleStrength = wp.fishWiggleStrength * fish.wiggleMul;
        double res = Math.max(0.2, 1.0 / Math.max(0.25, wiggleStrength));
        double movementFactor = Math.max(0.3, wiggleStrength);

        double uiScale = Math.max(0.6, wp.reelBarScale + cs.reelBarScaleBonus + (r != null ? r.reelBarScaleBonus : 0.0));
        double controlWidth = Math.max(0.2, Math.min(0.9, 0.18 * uiScale));

        reelMinigame.setResilience(res);
        reelMinigame.setProgressEfficiency(progressEff);
        reelMinigame.setMovementFactor(movementFactor);
        reelMinigame.setControlWidth(controlWidth);

        reelBarScaleUI = Math.max(0.75, Math.min(1.8, uiScale));
    }

    private Fish rollFishConsideringBonuses() {
        java.util.List<GameplayTuning.FishParams> list = new java.util.ArrayList<>(GameplayTuning.fishes());
        if (list.isEmpty()) {
            return Fish.getRandomFish();
        }
        GameplayTuning.FishParams fp = list.get((int)(Math.random() * list.size()));

        GameplayTuning.CharStats cs = currentCharStats();
        GameplayTuning.RodParams r = currentRod();
        double luck = (cs != null ? cs.luck : 0.0) + (r != null ? r.luckBonus : 0.0);
        double goldenBonus = (r != null ? r.goldenChanceBonus : 0.0);
        double goldenChance = 0.15 * (1.0 + Math.max(0, luck) + Math.max(0, goldenBonus));
        goldenChance = Math.max(0.0, Math.min(0.9, goldenChance));

        boolean golden = Math.random() < goldenChance;
        String name = fp.displayName;
        int price = fp.basePrice;
        if (golden) {
            name = "Golden " + name;
            price = (int)Math.round(price * 2.0);
        }

        return new Fish(fp.id, name, price, golden, fp.imagePath, fp.reelRateMul, fp.wiggleMul, fp.biteSpeedMul);
    }

    private void handleBuyOrEquipRod(String rodId) {
        GameplayTuning.RodParams r = GameplayTuning.rod(rodId);
        if (r == null) return;
        if (!ownedRods.contains(rodId)) {
            if (player.money >= r.price) {
                player.addMoney(-r.price);
                ownedRods.add(rodId);
                currentRodId = rodId;
            } else {
                JOptionPane.showMessageDialog(this, "เงินไม่พอซื้อ " + r.displayName);
            }
        } else {
            currentRodId = rodId; // equip
        }
        repaint();
    }
}