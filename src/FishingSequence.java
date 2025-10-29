public class FishingSequence {
    public FishingPhase phase;
    public boolean reelingFinished = false;
    public boolean success = false;
    public int castMaxTime;          
    public int castTimeRemaining;   
    public final Fish caughtFish;

    private static final int TICK_MS = 50;

    public FishingSequence(Fish fish, int castTimeMs) {
        this.caughtFish = fish;
        this.castMaxTime = Math.max(200, castTimeMs);
        this.castTimeRemaining = this.castMaxTime;
        this.phase = FishingPhase.CASTING;
    }

    public FishingSequence(Fish fish) {
        this(fish, 2000);
    }

    public void update() {
        if (phase == FishingPhase.CASTING) {
            castTimeRemaining -= TICK_MS;
            if (castTimeRemaining <= 0) {
                phase = FishingPhase.REELING;
            }
        } else if (phase == FishingPhase.REELING) {
        }
    }
}