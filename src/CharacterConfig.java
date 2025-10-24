public class CharacterConfig {
    public final String displayName;
    public final int col;
    public final int row;

    // รองรับไฟล์สไปรต์ NPC
    public final String npcSpritePath; // เช่น "src/assets/Cute_Fantasy/NPCs (Premade)/Fisherman_Fin.png"
    public final int npcCell;          // ขนาดเฟรม (ปกติ 64)

    public CharacterConfig(String displayName, int col, int row) {
        this.displayName = displayName;
        this.col = col;
        this.row = row;
        this.npcSpritePath = null;
        this.npcCell = 0;
    }

    public CharacterConfig(String displayName, String npcSpritePath, int npcCell) {
        this.displayName = displayName;
        this.col = 0;
        this.row = 0;
        this.npcSpritePath = npcSpritePath;
        this.npcCell = npcCell;
    }

    public boolean isNPC() {
        return npcSpritePath != null;
    }
}