public class FishingSequence {
    Fish caughtFish;
    FishingPhase phase;
    
    long castTimeRemaining = 3000;
    long castMaxTime = 3000;
    boolean castFinished = false;
    float castProgress = 0;
    float perfectZone = 0.7f;
    
    long snagTimeRemaining = 2000;
    long snagMaxTime = 2000;
    boolean snagFinished = false;
    boolean shaking = false;
    boolean snagSuccess = false;
    int shakeCounter = 0;
    
    float tension = 0.5f;
    float reelingValue = 0;
    float reelingMaxValue = 100;
    boolean reelingFinished = false;
    boolean success = false;
    
    FishingSequence(Fish fish) {
        this.caughtFish = fish;
        this.phase = FishingPhase.CASTING;
    }

    void update() {
        if (phase == FishingPhase.CASTING) {
            castTimeRemaining -= 50;
            castProgress = 1 - (float) castTimeRemaining / castMaxTime;
            if (castTimeRemaining <= 0) {
                castFinished = true;
            }
        } else if (phase == FishingPhase.SNAG) {
            snagTimeRemaining -= 50;
            shakeCounter++;
            if (shakeCounter % 15 == 0) {
                shaking = !shaking;
            }
            if (snagTimeRemaining <= 0) {
                snagFinished = true;
                if (!snagSuccess) {
                    success = false;
                    reelingFinished = true;
                }
            }
        } else if (phase == FishingPhase.REELING) {
            tension += (Math.random() - 0.5) * 0.05f;
            tension = Math.max(0, Math.min(1, tension));
            
            float controlCenter = 0.5f;
            float controlRange = 0.15f;
            
            if (tension < controlCenter - controlRange || tension > controlCenter + controlRange) {
                reelingValue -= 2;
            } else {
                reelingValue += 1;
            }
            
            if (reelingValue >= reelingMaxValue) {
                success = true;
                reelingFinished = true;
            } else if (reelingValue <= 0) {
                success = false;
                reelingFinished = true;
            }
        }
    }

    void castPress() {
        float tolerance = 0.1f;
        if (Math.abs(castProgress - perfectZone) < tolerance) {
            success = true;
        }
        castFinished = true;
    }

    void snagPress() {
        if (shaking) {
            snagSuccess = true;
        }
        snagFinished = true;
    }

    void updateTension(float normalizedX) {
        tension = normalizedX;
    }
}