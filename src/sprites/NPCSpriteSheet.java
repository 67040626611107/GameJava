package sprites;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;


public class NPCSpriteSheet {

    public enum Action {
        IDLE_DOWN, IDLE_LEFT, IDLE_RIGHT, IDLE_UP,
        WALK_DOWN, WALK_LEFT, WALK_RIGHT, WALK_UP,
        FISH_CAST, FISH_REEL
    }

    public static class Frame {
        public final int row, col;
        public Frame(int r, int c){ row=r; col=c; }
    }

    public static class Animation {
        public final List<Frame> frames = new ArrayList<>();
        public final int msPerFrame;
        public Animation(int msPerFrame){ this.msPerFrame = msPerFrame; }
        public Frame get(long t){
            if (frames.isEmpty()) return new Frame(0,0);
            int idx = (int)((t / msPerFrame) % frames.size());
            return frames.get(idx);
        }
    }

    private final BufferedImage sheet;
    private final int cell;     // 64
    private final int cols;
    private final int rows;
    private final Map<Action, Animation> anims = new EnumMap<>(Action.class);
    private final String fileName;

    public NPCSpriteSheet(String path, int cell, String mappingJsonPathIfAny) throws Exception {
        File imgFile = resolve(path);
        if (imgFile == null) throw new IllegalArgumentException("Sprite not found: " + path);
        this.sheet = ImageIO.read(imgFile);
        this.cell = cell <= 0 ? 64 : cell;
        this.cols = Math.max(1, sheet.getWidth() / this.cell);
        this.rows = Math.max(1, sheet.getHeight() / this.cell);
        this.fileName = imgFile.getName();

        Map<String, Object> mapping = loadMapping(mappingJsonPathIfAny);
        if (!applyMapping(mapping)) {
            applyDefault();
        }
    }

    private File resolve(String p){
        File f = new File(p);
        if (f.exists()) return f;
        f = new File("src/" + p);
        if (f.exists()) return f;
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadMapping(String jsonPath){
        try {
            if (jsonPath == null) return Collections.emptyMap();
            File f = resolve(jsonPath);
            if (f == null || !f.exists()) return Collections.emptyMap();
            String txt = Files.readString(Paths.get(f.toURI()));
            return SimpleJson.parseObject(txt);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private boolean applyMapping(Map<String, Object> mapping){
        if (mapping == null || mapping.isEmpty()) return false;
        Object arr = mapping.get("entries");
        if (!(arr instanceof List)) return false;

        List<?> entries = (List<?>) arr;
        for (Object o : entries) {
            if (!(o instanceof Map)) continue;
            Map<String, Object> e = (Map<String, Object>) o;
            String file = asStr(e.get("file"));
            if (file == null) continue;
            if (!file.equalsIgnoreCase(fileName)) continue;

            Map<String, Object> seqs = asObj(e.get("sequences"));
            if (seqs == null) continue;

            for (Action a : Action.values()) {
                Map<String, Object> def = asObj(seqs.get(a.name()));
                if (def == null) continue;
                Integer row = asInt(def.get("row"));
                List<Integer> colsList = asIntList(def.get("cols"));
                Integer ms = asInt(def.get("ms"));
                if (row == null || colsList == null || colsList.isEmpty()) continue;
                Animation ani = new Animation(ms != null ? ms : 100);
                for (Integer c : colsList) {
                    if (row >= 0 && row < rows && c >= 0 && c < this.cols){
                        ani.frames.add(new Frame(row, c));
                    }
                }
                if (!ani.frames.isEmpty()){
                    anims.put(a, ani);
                }
            }
            return !anims.isEmpty();
        }
        return false;
    }

    private void applyDefault(){
        int[] rowOf = new int[]{0,1,2,3};
        int maxCols = Math.min(cols, 8);
        int idleCol = 0;

        putSimple(Action.IDLE_DOWN, rowOf[0], new int[]{idleCol}, 400);
        putSimple(Action.IDLE_LEFT, rowOf[1], new int[]{idleCol}, 400);
        putSimple(Action.IDLE_RIGHT,rowOf[2], new int[]{idleCol}, 400);
        putSimple(Action.IDLE_UP,  rowOf[3], new int[]{idleCol}, 400);

        putLinear(Action.WALK_DOWN, rowOf[0], 0, maxCols-1, 100);
        putLinear(Action.WALK_LEFT, rowOf[1], 0, maxCols-1, 100);
        putLinear(Action.WALK_RIGHT,rowOf[2], 0, maxCols-1, 100);
        putLinear(Action.WALK_UP,  rowOf[3], 0, maxCols-1, 100);
    }

    private void putSimple(Action a, int row, int[] colIdx, int ms){
        Animation ani = new Animation(ms);
        for (int c : colIdx){
            if (row >= 0 && row < rows && c >= 0 && c < this.cols) {
                ani.frames.add(new Frame(row, c));
            }
        }
        if (!ani.frames.isEmpty()) anims.put(a, ani);
    }

    private void putLinear(Action a, int row, int from, int to, int ms){
        Animation ani = new Animation(ms);
        int lo = Math.max(0, Math.min(from, to));
        int hi = Math.min(cols-1, Math.max(from, to));
        for (int c = lo; c <= hi; c++){
            if (row >= 0 && row < rows) ani.frames.add(new Frame(row, c));
        }
        if (!ani.frames.isEmpty()) anims.put(a, ani);
    }

    public BufferedImage get(Action a, long t) {
        Animation ani = anims.get(a);
        if (ani == null) return crop(0,0);
        Frame f = ani.get(t);
        return crop(f.row, f.col);
    }

    public BufferedImage crop(int row, int col){
        int sx = col * cell;
        int sy = row * cell;
        int w = Math.min(cell, sheet.getWidth() - sx);
        int h = Math.min(cell, sheet.getHeight() - sy);
        return sheet.getSubimage(sx, sy, w, h);
    }

    // ---------- JSON helpers ----------
    private static String asStr(Object o){ return (o instanceof String) ? (String)o : null; }
    private static Integer asInt(Object o){
        if (o instanceof Number) return ((Number)o).intValue();
        try { return o == null ? null : Integer.parseInt(String.valueOf(o)); } catch(Exception e){ return null; }
    }
    @SuppressWarnings("unchecked")
    private static Map<String,Object> asObj(Object o){ return (o instanceof Map) ? (Map<String,Object>) o : null; }
    @SuppressWarnings("unchecked")
    private static List<Integer> asIntList(Object o){
        if (!(o instanceof List)) return null;
        List<?> raw = (List<?>)o;
        List<Integer> out = new ArrayList<>();
        for (Object x : raw){
            Integer v = asInt(x);
            if (v != null) out.add(v);
        }
        return out;
    }


    static class SimpleJson {
        private final String s; private int i=0;
        private SimpleJson(String s){ this.s=s; }
        @SuppressWarnings("unchecked")
        static Map<String,Object> parseObject(String txt) {
            Object v = new SimpleJson(txt).val();
            if (v instanceof Map) return (Map<String, Object>) v;
            return Collections.emptyMap();
        }
        private void ws(){ while(i<s.length() && Character.isWhitespace(s.charAt(i))) i++; }
        private char ch(){ return s.charAt(i); }
        private Object val(){
            ws(); if (i>=s.length()) return null;
            char c = ch();
            if (c=='{') return obj();
            if (c=='[') return arr();
            if (c=='"') return str();
            if (c=='t'||c=='f') return bool();
            if (c=='n') { i+=4; return null; }
            return num();
        }
        private Map<String,Object> obj(){
            Map<String,Object> m=new LinkedHashMap<>();
            i++; ws();
            while (i<s.length() && ch()!='}'){
                String k = str();
                ws(); if (ch()==':') i++;
                Object v = val();
                m.put(k,v);
                ws();
                if (ch()==','){ i++; ws(); }
            }
            if (i<s.length() && ch()=='}') i++;
            return m;
        }
        private List<Object> arr(){
            List<Object> a=new ArrayList<>();
            i++; ws();
            while (i<s.length() && ch()!=']'){
                a.add(val()); ws();
                if (ch()==','){ i++; ws(); }
            }
            if (i<s.length() && ch()==']') i++;
            return a;
        }
        private String str(){
            StringBuilder b=new StringBuilder();
            if (ch()=='"') i++;
            while (i<s.length() && ch()!='"'){
                char c = s.charAt(i++);
                if (c=='\\' && i<s.length()){
                    char n = s.charAt(i++);
                    if (n=='"'||n=='\\'||n=='/') b.append(n);
                    else if (n=='b') b.append('\b');
                    else if (n=='f') b.append('\f');
                    else if (n=='n') b.append('\n');
                    else if (n=='r') b.append('\r');
                    else if (n=='t') b.append('\t');
                    else b.append(n);
                } else b.append(c);
            }
            if (i<s.length() && ch()=='"') i++;
            return b.toString();
        }
        private Boolean bool(){
            if (s.startsWith("true", i)){ i+=4; return Boolean.TRUE; }
            if (s.startsWith("false", i)){ i+=5; return Boolean.FALSE; }
            return null;
        }
        private Number num(){
            int j=i;
            while (i<s.length() && "-+0123456789.eE".indexOf(s.charAt(i))>=0) i++;
            String seg = s.substring(j,i);
            try {
                if (seg.contains(".")||seg.contains("e")||seg.contains("E")) return Double.parseDouble(seg);
                return Long.parseLong(seg);
            } catch(Exception e){
                return 0;
            }
        }
    }
}