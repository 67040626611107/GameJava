import java.awt.*;
import java.awt.image.BufferedImage;
import sprites.NPCSpriteSheet;

public class Player {
    public int x, y;
    private int speed = 6;

    public int money = 0;

    private CharacterConfig config;

    private NPCSpriteSheet npc;
    private Direction dir = Direction.DOWN;
    private boolean moving = false;

    private long lastAnimTime = System.currentTimeMillis();

    private NPCSpriteSheet.Action overrideAction = null;

    public enum Direction { DOWN, LEFT, RIGHT, UP }

    public Player(int x, int y){
        this.x = x; this.y = y;
    }

    public void setCharacter(CharacterConfig cfg) {
        this.config = cfg;
        if (cfg != null && cfg.isNPC()) {
            try {
                this.npc = new NPCSpriteSheet(cfg.npcSpritePath, cfg.npcCell, "src/assets/npc_animations.json");
                System.out.println("✅ NPC loaded: " + cfg.npcSpritePath + " cell=" + cfg.npcCell);
            } catch (Exception e) {
                System.out.println("❌ NPC load failed: " + cfg.npcSpritePath + " (" + e.getMessage() + ")");
                e.printStackTrace();
                this.npc = null;
            }
        } else {
            this.npc = null;
        }
    }

    public void addMoney(int v){ money += v; }

    public void moveUp(){ setDirection(Direction.UP); setMoving(true); }
    public void moveDown(){ setDirection(Direction.DOWN); setMoving(true); }
    public void moveLeft(){ setDirection(Direction.LEFT); setMoving(true); }
    public void moveRight(){ setDirection(Direction.RIGHT); setMoving(true); }

    public void setDirection(Direction d){ this.dir = d; }
    public void setMoving(boolean m){ this.moving = m; }
    public Direction getDirection(){ return dir; }
    public boolean isMoving(){ return moving; }
    public int getSpeed(){ return speed; }

    public void setOverrideAction(NPCSpriteSheet.Action a) {
        this.overrideAction = a;
    }

    public void draw(Graphics2D g2d, BufferedImage legacySpriteSheet, Component cmp) {
        long now = System.currentTimeMillis();
        if (npc != null) {
            NPCSpriteSheet.Action act = pickAction();
            BufferedImage frame = npc.get(act, now);
            int drawW = frame.getWidth();
            int drawH = frame.getHeight();
            g2d.drawImage(frame, x - drawW/2, y - drawH + 8, null);
        } else {
            g2d.setColor(new Color(255, 255, 0, 200));
            g2d.fillOval(x-10, y-20, 20, 20);
        }
        moving = false;
        lastAnimTime = now;
    }

    private NPCSpriteSheet.Action pickAction() {
        if (overrideAction != null) return overrideAction;
        if (moving) {
            switch (dir) {
                case UP:    return NPCSpriteSheet.Action.WALK_UP;
                case DOWN:  return NPCSpriteSheet.Action.WALK_DOWN;
                case LEFT:  return NPCSpriteSheet.Action.WALK_LEFT;
                case RIGHT: return NPCSpriteSheet.Action.WALK_RIGHT;
            }
        } else {
            switch (dir) {
                case UP:    return NPCSpriteSheet.Action.IDLE_UP;
                case DOWN:  return NPCSpriteSheet.Action.IDLE_DOWN;
                case LEFT:  return NPCSpriteSheet.Action.IDLE_LEFT;
                case RIGHT: return NPCSpriteSheet.Action.IDLE_RIGHT;
            }
        }
        return NPCSpriteSheet.Action.IDLE_DOWN;
    }
}