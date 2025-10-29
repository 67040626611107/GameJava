public class ReelMinigame {
    private static final double VMAX = 14.0;

    private double progress = 20.0;     // 0..100
    private int dir = -1;               // -1 release, +1 press
    private double vel = 0.0;           // player window velocity
    private double accel = 1.0;         // acceleration
    private double res = 0.9;           // resilience (higher -> easier)
    private double progressEfficiency = 2.0;
    private double controlWidth = 0.5;  // fraction (0.2..0.9)
    private double movementFactor = 1.0;
    private boolean perfect = true;

    private double playerBarX = 1.0;    // [0..1]
    private double fishX = 0.5;         // [0..1]
    private double fishTargetX = 0.5;
    private double fishCooldown = 0.0;  // sec

    private boolean finished = false;
    private boolean success = false;

    public void press()  { dir = +1; }
    public void release(){ dir = -1; }

    public void setResilience(double resilience) { this.res = Math.max(0.2, resilience); }
    public void setProgressEfficiency(double efficiency) { this.progressEfficiency = Math.max(0.1, efficiency); }
    public void setControlWidth(double widthFraction) { this.controlWidth = clamp(0.2, 0.9, widthFraction); }
    public void setMovementFactor(double factor) { this.movementFactor = Math.max(0.1, factor); }
    public void setInitialProgress(double p) { this.progress = clamp(-1, 100, p); }

    public void update(double dtSeconds, double barPixelWidth) {
        if (finished) return;

        vel = clamp(-VMAX, VMAX, vel + dir * accel * dtSeconds * 60.0);
        double half = controlWidth / 2.0;
        playerBarX = clamp(half, 1.0 - half, playerBarX + vel * 0.001 * dtSeconds * 60.0);

        fishCooldown -= dtSeconds;
        if (fishCooldown <= 0.0) {
            double delta = randRange(-0.32, 0.32);
            fishTargetX = clamp(0.03, 0.97, fishX + delta * clamp(0.8, 1.2, res));
            double base = randRange(1.3, 3.5);
            fishCooldown = base * clamp(0.1, 1.5, res) / movementFactor;
        }
        double t = clamp(0, 1, 0.08 * dtSeconds * 60.0 / clamp(0.8, 1.4, res));
        fishX = lerp(fishX, fishTargetX, t);

        double fishWidthNorm = 8.0 / Math.max(1.0, barPixelWidth);
        boolean overlap = overlaps(playerBarX, controlWidth, fishX, fishWidthNorm);
        if (overlap) {
            progress = clamp(-1, 100, progress + 0.2 * progressEfficiency * dtSeconds * 60.0);
        } else {
            progress = clamp(-1, 100, progress - (0.2 * dtSeconds * 60.0 + 0.0017 * res));
            perfect = false;
        }

        if (progress >= 100.0) { finished = true; success = true; }
        if (progress <= -1.0)  { finished = true; success = false; }
    }

    public double getProgress() { return progress; }
    public double getPlayerBarCenter() { return playerBarX; }
    public double getControlWidth() { return controlWidth; }
    public double getFishCenter() { return fishX; }
    public boolean isFinished() { return finished; }
    public boolean isSuccess() { return success; }
    public boolean isPerfect() { return perfect; }

    private static double clamp(double min, double max, double v) { return Math.max(min, Math.min(max, v)); }
    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    private static double randRange(double a, double b) { return a + Math.random() * (b - a); }
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