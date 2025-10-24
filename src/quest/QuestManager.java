package quest;

import quest.QuestModels.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

// เลี่ยง import java.awt.* เพื่อไม่ให้ List ชนกับ java.util.List
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;

public class QuestManager {
    private QuestDef def;
    private final QuestProgress progress = new QuestProgress();

    // กันจ่ายซ้ำ
    private final Set<String> paidGoals = new HashSet<>();

    // UI state
    private final Rectangle panelBounds = new Rectangle();
    private final Rectangle toggleBounds = new Rectangle();
    private boolean collapsed = false;

    // จ่ายรางวัล
    public static class GoalPayout {
        public final String goalId;
        public final int money;
        public final String rewardText;
        public GoalPayout(String goalId, int money, String rewardText) {
            this.goalId = goalId; this.money = money; this.rewardText = rewardText;
        }
    }

    // โหลด quests.json
    @SuppressWarnings("unchecked")
    public void load(String path) {
        String json = readAll(path);
        if (json == null) json = defaultJSON();
        Object root = MiniJSON.parse(json);
        if (!(root instanceof Map)) {
            def = makeDefault();
            return;
        }
        Map<String, Object> m = (Map<String, Object>) root;
        QuestDef q = new QuestDef();
        q.title = MiniJSON.getString(m, "title", "Guide");

        List<Object> goals = MiniJSON.getArray(m, "goals");
        for (Object o : goals) {
            if (!(o instanceof Map)) continue;
            Map<String, Object> gm = (Map<String, Object>) o;
            QuestModels.GoalDef gd = new QuestModels.GoalDef();
            gd.id = MiniJSON.getString(gm, "id", UUID.randomUUID().toString());
            gd.text = MiniJSON.getString(gm, "text", "");
            gd.type = MiniJSON.getString(gm, "type", "catch_count");
            gd.filter = MiniJSON.getString(gm, "filter", "any");
            gd.target = MiniJSON.getInt(gm, "target", 1);
            gd.optional = MiniJSON.getBool(gm, "optional", false);
            gd.rewardMoney = MiniJSON.getInt(gm, "rewardMoney", 0);
            gd.rewardText = MiniJSON.getString(gm, "rewardText", "");
            q.goals.add(gd);
        }
        def = q;
    }

    public QuestDef getDef() { return def; }
    public QuestProgress getProgress() { return progress; }

    // ใช้ primitive เพื่อเลี่ยงอ้าง default package
    public void onFishCaught(String fishName, boolean golden) {
        if (def == null) return;
        for (QuestModels.GoalDef g : def.goals) {
            if (!"catch_count".equalsIgnoreCase(g.type)) continue;
            if (matchFilter(g.filter, fishName, golden)) {
                progress.add(g.id, 1, g.target);
            }
        }
    }

    public List<GoalPayout> collectNewPayouts() {
        List<GoalPayout> list = new ArrayList<>();
        if (def == null) return list;
        for (QuestModels.GoalDef g : def.goals) {
            if (progress.isCompleted(g.id) && !paidGoals.contains(g.id)) {
                paidGoals.add(g.id);
                if (g.rewardMoney > 0 || (g.rewardText != null && !g.rewardText.isEmpty())) {
                    list.add(new GoalPayout(g.id, g.rewardMoney, g.rewardText));
                }
            }
        }
        return list;
    }

    // วาด HUD ด้านขวาล่าง + สเกลอัตโนมัติ + ตัดบรรทัดอัตโนมัติเมื่อข้อความยาว
    public void draw(Graphics2D g2d, int screenW, int screenH) {
        if (def == null) return;

        // scale เทียบฐาน 1400x800
        double sW = screenW / 1400.0;
        double sH = screenH / 800.0;
        double s = Math.min(sW, sH);
        if (s < 0.75) s = 0.75;
        if (s > 1.5) s = 1.5;

        int width = round(300 * s);
        int pad = round(10 * s);
        int headerH = round(10 * s);

        int titleFontPx = clamp(round(12 * s), 10, 22);
        int goalFontPx = clamp(round(13 * s), 10, 22);
        Font titleFont = new Font("Arial", Font.BOLD, titleFontPx);
        Font goalFont = new Font("Arial", Font.BOLD, goalFontPx);

        int anchorY = (int) (screenH * 0.815) - round(10 * s);

        // ขนาด toggle และ gutter ในแผง
        int toggleW = clamp(round(24 * s), 18, 36);
        int gap = clamp(round(6 * s), 4, 12);
        int gutterW = toggleW + gap;

        // ความกว้างพื้นที่ "ข้อความ" ภายในแผง (ด้านขวาของปุ่ม + padding ขวา)
        int contentWidth = width - pad - gutterW - pad;

        // เตรียมวัดฟอนต์
        g2d.setFont(goalFont);
        FontMetrics goalFM = g2d.getFontMetrics();
        int checkSize = clamp(round(18 * s), 12, 28);
        int textGap = clamp(round(8 * s), 6, 14);
        int lineStep = clamp(round(20 * s), 14, 28);
        int afterTitleSpace = clamp(round(8 * s), 6, 12);
        int betweenGoals = clamp(round(6 * s), 4, 10);

        // คำนวณความสูงจาก "จำนวนบรรทัดจริง" ของทุก goal
        int contentH = pad; // top pad
        // สูงส่วน title
        g2d.setFont(titleFont);
        FontMetrics titleFM = g2d.getFontMetrics();
        int titleHeight = titleFM.getAscent();
        contentH += titleHeight + afterTitleSpace;

        // ความกว้างพื้นที่ข้อความต่อบรรทัด (ต้องหัก checkbox + ช่องว่าง)
        int perLineTextWidth = Math.max(20, contentWidth - checkSize - textGap);

        // เก็บผล wrap เพื่อใช้ตอนวาดจริง (เลี่ยงคำนวณซ้ำ)
        List<List<String>> wrappedPerGoal = new ArrayList<>();
        List<Integer> blockHeights = new ArrayList<>();

        for (QuestModels.GoalDef g : def.goals) {
            String base = buildGoalText(g);
            List<String> lines = wrapText(base, goalFM, perLineTextWidth);
            int blockH = Math.max(checkSize, lines.size() * lineStep);
            wrappedPerGoal.add(lines);
            blockHeights.add(blockH);
            contentH += blockH + betweenGoals;
        }
        contentH += pad; // bottom pad

        // คำนวณตำแหน่งแนวตั้งแผง
        int yTop = anchorY - contentH;

        // โหมดพับ: ปุ่มชิดขวาจอ ความสูง = contentH ตรงกับแผงตอนกาง
        if (collapsed) {
            panelBounds.setBounds(screenW, yTop, width, contentH);
            int toggleX = screenW - toggleW;
            int toggleY = yTop;
            toggleBounds.setBounds(toggleX, toggleY, toggleW, contentH);

            int radius = round(10 * s);
            int arrowSize = clamp(round(6 * s), 4, 10);

            g2d.setColor(new Color(0, 0, 0, 90));
            g2d.fillRoundRect(toggleBounds.x, toggleBounds.y, toggleBounds.width, toggleBounds.height, radius, radius);
            g2d.setColor(new Color(255, 255, 255, 140));
            g2d.drawRoundRect(toggleBounds.x, toggleBounds.y, toggleBounds.width, toggleBounds.height, radius, radius);

            // ลูกศรซ้าย (กางออก)
            int cx = toggleBounds.x + toggleBounds.width / 2;
            int cy = toggleBounds.y + toggleBounds.height / 2;
            Polygon leftArrow = new Polygon();
            leftArrow.addPoint(cx - arrowSize, cy);
            leftArrow.addPoint(cx + (int)Math.round(arrowSize * 0.7), cy - arrowSize);
            leftArrow.addPoint(cx + (int)Math.round(arrowSize * 0.7), cy + arrowSize);
            g2d.setColor(new Color(255, 255, 255, 190));
            g2d.fillPolygon(leftArrow);
            return;
        }

        // โหมดเปิด: วาดแผง + ปุ่มซ้ายในแผง + เนื้อหา wrap แล้ว
        int xExpanded = (int) (screenW * 0.985) - width;
        panelBounds.setBounds(xExpanded, yTop, width, contentH);

        int radius = round(10 * s);
        g2d.setColor(new Color(0, 0, 0, 90));
        g2d.fillRoundRect(panelBounds.x, panelBounds.y, panelBounds.width, panelBounds.height, radius, radius);
        g2d.setColor(new Color(255, 255, 255, 180));
        g2d.drawRoundRect(panelBounds.x, panelBounds.y, panelBounds.width, panelBounds.height, radius, radius);

        // ปุ่ม toggle ในแผง (ซ้ายสุด)
        int toggleX = panelBounds.x;
        int toggleY = panelBounds.y;
        toggleBounds.setBounds(toggleX, toggleY, toggleW, contentH);

        g2d.setColor(new Color(0, 0, 0, 110));
        g2d.fillRoundRect(toggleBounds.x, toggleBounds.y, toggleBounds.width, toggleBounds.height, radius, radius);
        g2d.setColor(new Color(255, 255, 255, 160));
        g2d.drawRoundRect(toggleBounds.x, toggleBounds.y, toggleBounds.width, toggleBounds.height, radius, radius);

        // ลูกศรขวา (พับออกไปทางขวา)
        int arrowSize = clamp(round(6 * s), 4, 10);
        int cx = toggleBounds.x + toggleBounds.width / 2;
        int cy = toggleBounds.y + toggleBounds.height / 2;
        Polygon rightArrow = new Polygon();
        rightArrow.addPoint(cx + arrowSize, cy);
        rightArrow.addPoint(cx - (int)Math.round(arrowSize * 0.7), cy - arrowSize);
        rightArrow.addPoint(cx - (int)Math.round(arrowSize * 0.7), cy + arrowSize);
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.fillPolygon(rightArrow);

        // ตำแหน่งเริ่มเนื้อหา
        int contentX = panelBounds.x + pad + gutterW;
        int textX = contentX + checkSize + textGap;

        // วาด Title
        g2d.setFont(titleFont);
        g2d.setColor(new Color(230, 230, 230));
        g2d.drawString(def.title, contentX, panelBounds.y + pad + titleFM.getAscent());

        // วาด Goals ที่ถูก wrap
        g2d.setFont(goalFont);
        int yCursor = panelBounds.y + pad + titleHeight + afterTitleSpace;
        for (int idx = 0; idx < def.goals.size(); idx++) {
            List<String> lines = wrappedPerGoal.get(idx);
            int blockH = blockHeights.get(idx);

            // checkbox จัดตรงกลางบล็อคแนวตั้ง
            int checkY = yCursor + Math.max(0, (blockH - checkSize) / 2);
            g2d.setColor(progress.isCompleted(def.goals.get(idx).id)
                    ? new Color(125, 255, 50, 200)
                    : new Color(255, 255, 255, 80));
            g2d.fillRoundRect(contentX, checkY, checkSize, checkSize, clamp(round(6 * s), 4, 10), clamp(round(6 * s), 4, 10));

            // วาดทุกบรรทัดของข้อความ
            g2d.setColor(Color.WHITE);
            int lineY = yCursor + goalFM.getAscent();
            for (String ln : lines) {
                g2d.drawString(ln, textX, lineY);
                lineY += lineStep;
            }

            yCursor += blockH + betweenGoals;
        }
    }

    public boolean handleClick(int mx, int my) {
        if (toggleBounds.contains(mx, my)) {
            collapsed = !collapsed;
            return true;
        }
        return false;
    }

    private boolean matchFilter(String filter, String fishName, boolean golden) {
        if (filter == null || filter.isEmpty() || "any".equalsIgnoreCase(filter)) return true;
        String f = filter.toLowerCase(Locale.ROOT);
        if (f.startsWith("rarity:")) {
            String v = f.substring("rarity:".length());
            if ("golden".equals(v)) return golden;
            return false;
        }
        if (f.startsWith("name:")) {
            String v = f.substring("name:".length()).trim().toLowerCase(Locale.ROOT);
            return fishName != null && fishName.toLowerCase(Locale.ROOT).equals(v);
        }
        return false;
    }

    private String readAll(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) f = new File("./" + path);
            if (!f.exists()) f = new File("src/" + path);
            if (!f.exists()) return null;
            try (InputStream in = new FileInputStream(f)) {
                byte[] buf = in.readAllBytes();
                return new String(buf, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private QuestDef makeDefault() {
        QuestDef q = new QuestDef();
        q.title = "Fishing Guide";
        QuestModels.GoalDef g1 = new QuestModels.GoalDef();
        g1.id = "catch_any"; g1.text = "Catch 6 Fish"; g1.type = "catch_count"; g1.filter = "any"; g1.target = 6; g1.rewardMoney = 150; g1.rewardText = "";
        QuestModels.GoalDef g2 = new QuestModels.GoalDef();
        g2.id = "catch_golden"; g2.text = "Catch 2 Golden Fish"; g2.type = "catch_count"; g2.filter = "rarity:golden"; g2.target = 2; g2.rewardMoney = 250; g2.rewardText = "Bonus Chest";
        q.goals.add(g1); q.goals.add(g2);
        return q;
    }

    private String defaultJSON() {
        return "{\n" +
                "  \"title\": \"Fishing Guide\",\n" +
                "  \"goals\": [\n" +
                "    {\"id\":\"catch_any\",\"text\":\"Catch 6 Fish\",\"type\":\"catch_count\",\"filter\":\"any\",\"target\":6,\"rewardMoney\":150},\n" +
                "    {\"id\":\"catch_golden\",\"text\":\"Catch 2 Golden Fish\",\"type\":\"catch_count\",\"filter\":\"rarity:golden\",\"target\":2,\"rewardMoney\":250,\"rewardText\":\"Bonus Chest\"}\n" +
                "  ]\n" +
                "}";
    }

    private static int round(double v) { return (int)Math.round(v); }
    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    // สร้างข้อความเต็มของ goal รวม progress และ reward annotation
    private String buildGoalText(QuestModels.GoalDef g) {
        int cur = progress.get(g.id);
        String base = g.text;
        if ("catch_count".equalsIgnoreCase(g.type) && g.target > 0) {
            base += " (" + Math.min(cur, g.target) + "/" + g.target + ")";
        }
        String ann = null;
        if (g.rewardText != null && !g.rewardText.isEmpty()) {
            ann = g.rewardText;
        } else if (g.rewardMoney > 0) {
            ann = "+" + g.rewardMoney + " Coins";
        }
        if (ann != null) base += " - " + ann;
        return base;
    }

    // ตัดบรรทัดตามความกว้างที่ให้, รองรับคำยาวเกินบรรทัดด้วยการผ่าเป็นช่วงๆ
    private static List<String> wrapText(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }
        String[] words = text.split("\\s+");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            String candidate = cur.length() == 0 ? w : cur + " " + w;
            if (fm.stringWidth(candidate) <= maxWidth) {
                cur.setLength(0);
                cur.append(candidate);
            } else {
                // ถ้าใส่คำใหม่แล้วล้น: ปิดบรรทัดปัจจุบันก่อน
                if (cur.length() > 0) {
                    lines.add(cur.toString());
                    cur.setLength(0);
                }
                // ถ้าคำเดี่ยวๆ ยาวเกิน maxWidth ให้หั่นตัวอักษร
                if (fm.stringWidth(w) > maxWidth) {
                    int start = 0;
                    while (start < w.length()) {
                        int end = start + 1;
                        // ขยายจนกว่าจะล้น
                        while (end <= w.length() && fm.stringWidth(w.substring(start, end)) <= maxWidth) {
                            end++;
                        }
                        // ถอย 1 ตำแหน่งเพราะ end เกิน
                        end = Math.max(start + 1, end - 1);
                        lines.add(w.substring(start, end));
                        start = end;
                    }
                } else {
                    // เริ่มบรรทัดใหม่ด้วยคำนี้
                    cur.append(w);
                }
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }
}