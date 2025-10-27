import java.util.Collections;
import java.util.Map;

/**
 * CharacterStats - เก็บ stats ของตัวละคร (เช่น luck) สำหรับใช้ใน preview / fishing logic.
 * - ฟิลด์/เมธอดเรียบง่ายเพื่อให้ UI/ส่วนอื่นใน repo สามารถเรียกได้ทันที
 */
public class CharacterStats {
    private final String characterId;
    private final String displayName;
    private final int luck; // 0..n
    private final Map<String, Integer> otherStats;

    public CharacterStats(String characterId, String displayName, int luck, Map<String, Integer> otherStats) {
        this.characterId = characterId != null ? characterId : "";
        this.displayName = displayName != null ? displayName : "";
        this.luck = Math.max(0, luck);
        this.otherStats = otherStats != null ? otherStats : Collections.emptyMap();
    }

    public String getCharacterId() { return characterId; }
    public String getDisplayName() { return displayName; }
    public int getLuck() { return luck; }
    public Map<String, Integer> getOtherStats() { return otherStats; }

    @Override
    public String toString() {
        return "CharacterStats{" +
                "id='" + characterId + '\'' +
                ", name='" + displayName + '\'' +
                ", luck=" + luck +
                ", otherStats=" + otherStats +
                '}';
    }
}