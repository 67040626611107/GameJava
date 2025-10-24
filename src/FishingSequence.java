public class FishingSequence {
    Fish caughtFish;
    FishingPhase phase;
    
    // ใช้เป็นตัวนับ "รอปลากินเหยื่อ"
    long castTimeRemaining = 3000;
    long castMaxTime = 3000;
    boolean castFinished = false;
    float castProgress = 0;
    float perfectZone = 0.7f; // ไม่ใช้แล้ว
    
    // SNAG ไม่ใช้แล้ว แต่เก็บฟิลด์ไว้ให้คอมไพล์ผ่าน
    long snagTimeRemaining = 0;
    long snagMaxTime = 0;
    boolean snagFinished = true;
    boolean shaking = false;
    boolean snagSuccess = true;
    int shakeCounter = 0;
    
    // REELING จะขับเคลื่อนโดย ReelMinigame ภายนอก
    float tension = 0.5f;
    float reelingValue = 0;
    float reelingMaxValue = 100;
    boolean reelingFinished = false;
    boolean success = false;
    
    FishingSequence(Fish fish) {
        this.caughtFish = fish;
        this.phase = FishingPhase.CASTING;
        // สุ่มเวลารอ 2-5 วินาที แล้วค่อยเข้า REELING อัตโนมัติ
        this.castMaxTime = (long) (2000 + Math.random() * 3000);
        this.castTimeRemaining = castMaxTime;
        this.castFinished = false;
    }

    void update() {
        if (phase == FishingPhase.CASTING) {
            castTimeRemaining -= 50;
            if (castTimeRemaining < 0) castTimeRemaining = 0;
            castProgress = 1 - (float) castTimeRemaining / Math.max(1, castMaxTime);
            if (castTimeRemaining <= 0) {
                // ปลากัดแล้ว เข้าสู่ REELING ทันที
                phase = FishingPhase.REELING;
                castFinished = true;
                // ค่าใน REELING จะถูกขับโดย ReelMinigame จาก GamePanel
                reelingFinished = false;
                success = false;
            }
        } else if (phase == FishingPhase.SNAG) {
            // ไม่ใช้ SNAG แล้ว ข้ามเฟสนี้ (ถือว่าสำเร็จเสมอ)
            snagFinished = true;
            phase = FishingPhase.REELING;
        } else if (phase == FishingPhase.REELING) {
            // ปล่อยให้ GamePanel + ReelMinigame จัดการความคืบหน้า
            // ไม่แก้ reelingValue/tension ที่นี่
        }
    }

    // ไม่ใช้แล้ว (คงไว้เพื่อความเข้ากันได้)
    void castPress() { /* no-op */ }

    // ไม่ใช้แล้ว (คงไว้เพื่อความเข้ากันได้)
    void snagPress() { /* no-op */ }

    // เดิมใช้เมาส์ควบคุม tension — ไม่ใช้ใน flow ใหม่
    void updateTension(float normalizedX) { this.tension = normalizedX; }
}