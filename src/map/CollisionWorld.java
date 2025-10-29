package map;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class CollisionWorld {
    private final List<Rectangle> solids = new ArrayList<>();

    public void add(Rectangle r) {
        if (r != null) solids.add(new Rectangle(r));
    }

    public void addAll(List<Rectangle> rects) {
        if (rects == null) return;
        for (Rectangle r : rects) add(r);
    }

    public boolean blocks(Rectangle r) {
        for (Rectangle s : solids) {
            if (s.intersects(r)) return true;
        }
        return false;
    }

    public List<Rectangle> getSolids() {
        return solids;
    }
}