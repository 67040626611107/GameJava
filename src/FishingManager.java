import java.util.Random;
import java.util.function.Consumer;

/**
 * FishingManager - ควบคุม logic การตกปลา (แก้ให้ตัวแปรที่ใช้ใน thread เป็น final)
 *
 * NOTE: โค้ดนี้ออกแบบให้ทำงานกับ FishInfo (หรือโครงข้อมูลปลาอื่น ๆ ใน repo)
 */
public class FishingManager {
    private final WorldConfigLoader.World2Config.FishingSettings settings;
    private final Random rnd = new Random();

    public FishingManager(WorldConfigLoader.World2Config.FishingSettings settings) {
        this.settings = settings;
    }

    public static class FishingResult {
        public final boolean caught;
        public final FishInfo fish;
        public FishingResult(boolean caught, FishInfo fish) {
            this.caught = caught;
            this.fish = fish;
        }
    }

    /**
     * เริ่ม session การตกปลา (ทำงานใน background thread)
     *
     * @param fish ปลาที่ตก
     * @param stats ตัวละคร
     * @param onProgressUpdate รับ percent (0..1)
     * @param onBite Runnable เรียกเมื่อปลา "กัด"
     * @param onEnd รับ FishingResult
     */
    public void startFishingSession(FishInfo fish,
                                    CharacterStats stats,
                                    Consumer<Double> onProgressUpdate,
                                    Runnable onBite,
                                    Consumer<FishingResult> onEnd) {
        // compute values once and keep them effectively-final for use inside thread
        final double baseProgress = settings != null ? settings.baseProgressSpeed : 0.6;
        final double difficultyModifier = settings != null && settings.progressSpeedModifierByDifficulty != null
                ? settings.progressSpeedModifierByDifficulty.getOrDefault(fish.difficulty, 1.0)
                : 1.0;
        // luck reduces difficulty: higher luck => faster progress
        final double luckModifier = 1.0 + (stats.getLuck() * 0.02); // tunable: 2% per luck point
        final double effectiveSpeed = baseProgress * difficultyModifier * luckModifier;

        final double struggleStrength = fish.struggleStrength;
        final double biteMultiplier = settings != null && settings.biteSpeedMultiplier != null
                ? settings.biteSpeedMultiplier.getOrDefault(fish.difficulty, 1.0)
                : 1.0;
        // compute bite time and make it final (fixes "must be final or effectively final")
        final double biteTime = Math.max(0.5, (fish.biteTimeSeconds / biteMultiplier) - stats.getLuck() * 0.05);

        new Thread(() -> {
            try {
                // รอให้กัด
                Thread.sleep((long)(biteTime * 1000));
                if (onBite != null) onBite.run();

                double progress = 0.0;
                while (progress < 1.0) {
                    double delta = effectiveSpeed * (0.05 + rnd.nextDouble() * 0.15);
                    progress += delta;
                    if (rnd.nextDouble() < 0.35) {
                        double kick = struggleStrength * (0.03 + rnd.nextDouble() * 0.12);
                        progress -= kick;
                        if (progress < 0) progress = 0;
                    }
                    double clamped = Math.max(0.0, Math.min(1.0, progress));
                    if (onProgressUpdate != null) onProgressUpdate.accept(clamped);
                    Thread.sleep(150);
                }
                if (onEnd != null) onEnd.accept(new FishingResult(true, fish));
            } catch (InterruptedException e) {
                if (onEnd != null) onEnd.accept(new FishingResult(false, fish));
            } catch (Exception ex) {
                if (onEnd != null) onEnd.accept(new FishingResult(false, fish));
            }
        }, "FishingSession-" + fish.id).start();
    }

    public double getBarScaleMultiplier() {
        return settings != null ? settings.barScaleMultiplier : 1.0;
    }
}