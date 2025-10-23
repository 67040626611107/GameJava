public class Fish {
    String name;
    int price;

    Fish(String name, int price) {
        this.name = name;
        this.price = price;
    }

    static Fish getRandomFish() {
        Fish[] fishes = {
                new Fish("ปลาทอง", 100),
                new Fish("ปลาหมึก", 150),
                new Fish("ปลาเบ็ด", 200),
                new Fish("ปลาสลิด", 80),
                new Fish("ปลาช่อน", 180),
                new Fish("ปลาบึก", 250)
        };
        return fishes[(int) (Math.random() * fishes.length)];
    }
}