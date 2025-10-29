package map;

import util.Json;

import java.util.*;


public final class MapIO {
    private MapIO(){}

    public static String toJson(MapData data){
        Map<String,Object> root = new LinkedHashMap<>();
        root.put("width", data.width);
        root.put("height", data.height);
        root.put("tileSize", data.tileSize);
        root.put("waterTopY", data.waterTopY);

        List<Object> arr = new ArrayList<>();
        for (MapData.MapObject o : data.objects) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("src", o.src);
            m.put("x", o.x);
            m.put("y", o.y);
            m.put("collide", o.collide);
            m.put("footH", o.footH);
            m.put("layer", o.layer);
            arr.add(m);
        }
        root.put("objects", arr);
        return Json.stringify(root);
    }

    @SuppressWarnings("unchecked")
    public static MapData fromJson(String text){
        MapData out = new MapData();
        Map<String,Object> root = Json.parseObject(text);

        out.width = asInt(root.get("width"), 1400);
        out.height = asInt(root.get("height"), 800);
        out.tileSize = asInt(root.get("tileSize"), 64);
        out.waterTopY = asInt(root.get("waterTopY"), out.height - 220);

        Object objs = root.get("objects");
        if (objs instanceof List) {
            for (Object it : (List<?>) objs) {
                if (!(it instanceof Map)) continue;
                Map<String,Object> m = (Map<String,Object>) it;
                MapData.MapObject o = new MapData.MapObject();
                o.src = asStr(m.get("src"));
                o.x = asInt(m.get("x"), 0);
                o.y = asInt(m.get("y"), 0);
                o.collide = asBool(m.get("collide"), true);
                o.footH = asInt(m.get("footH"), 16);
                o.layer = asInt(m.get("layer"), 0);
                if (o.src != null && !o.src.isEmpty()) out.objects.add(o);
            }
        }
        return out;
    }

    private static int asInt(Object o, int d){
        if (o instanceof Number) return ((Number)o).intValue();
        try { return o==null ? d : Integer.parseInt(String.valueOf(o)); } catch(Exception e){ return d; }
    }
    private static String asStr(Object o){ return o==null?null:String.valueOf(o); }
    private static boolean asBool(Object o, boolean d){ return o instanceof Boolean ? (Boolean)o : d; }
}