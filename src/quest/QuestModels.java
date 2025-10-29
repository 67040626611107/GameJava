package quest;

import java.util.*;

public class QuestModels {
    public static class GoalDef {
        public String id;
        public String text;
        public String type;    
        public String filter;   // e.g., "any", "rarity:golden", "name:Carp"
        public int target;
        public boolean optional;

        // per-goal rewards
        public int rewardMoney;      
        public String rewardText;   
    }

    public static class QuestDef {
        public String title;
        public List<GoalDef> goals = new ArrayList<>();
    }

    public static class QuestProgress {
        public final Map<String, Integer> counters = new HashMap<>();
        public final Set<String> completed = new HashSet<>();

        public int get(String goalId) { return counters.getOrDefault(goalId, 0); }

        public void add(String goalId, int delta, int target) {
            int v = counters.getOrDefault(goalId, 0) + delta;
            if (v < 0) v = 0;
            counters.put(goalId, v);
            if (target > 0 && v >= target) completed.add(goalId);
        }

        public boolean isCompleted(String goalId) { return completed.contains(goalId); }
    }
}