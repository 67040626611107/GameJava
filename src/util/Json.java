package util;

import java.util.*;


public final class Json {

    private Json(){}

    @SuppressWarnings("unchecked")
    public static Map<String,Object> parseObject(String text) {
        Object v = new Parser(text).val();
        if (v instanceof Map) return (Map<String, Object>) v;
        return Collections.emptyMap();
    }

    public static String stringify(Object v) {
        StringBuilder sb = new StringBuilder(1024);
        write(v, sb);
        return sb.toString();
    }

    // -------- writer --------
    @SuppressWarnings("unchecked")
    private static void write(Object v, StringBuilder sb) {
        if (v == null) { sb.append("null"); return; }
        if (v instanceof String) { sb.append('"').append(escape((String)v)).append('"'); return; }
        if (v instanceof Number || v instanceof Boolean) { sb.append(v.toString()); return; }
        if (v instanceof Map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String,Object> e : ((Map<String,Object>)v).entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(escape(e.getKey())).append('"').append(':');
                write(e.getValue(), sb);
            }
            sb.append('}');
            return;
        }
        if (v instanceof Iterable) {
            sb.append('[');
            boolean first = true;
            for (Object o : (Iterable<?>) v) {
                if (!first) sb.append(',');
                first = false;
                write(o, sb);
            }
            sb.append(']');
            return;
        }
        // fallback as string
        sb.append('"').append(escape(String.valueOf(v))).append('"');
    }

    private static String escape(String s){
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n","\\n").replace("\r","\\r").replace("\t","\\t");
    }

    // -------- parser --------
    private static final class Parser {
        private final String s; private int i=0;
        Parser(String s){ this.s=s==null?"":s; }
        private void ws(){ while(i<s.length() && Character.isWhitespace(s.charAt(i))) i++; }
        private char ch(){ return s.charAt(i); }
        Object val(){
            ws(); if (i>=s.length()) return null;
            char c = ch();
            if (c=='{') return obj();
            if (c=='[') return arr();
            if (c=='"') return str();
            if (c=='t'||c=='f') return bool();
            if (c=='n') { i+=4; return null; }
            return num();
        }
        Map<String,Object> obj(){
            Map<String,Object> m=new LinkedHashMap<>();
            i++; ws();
            while (i<s.length() && ch()!='}'){
                String k = str();
                ws(); if (ch()==':') i++;
                Object v = val();
                m.put(k,v); ws();
                if (ch()==','){ i++; ws(); }
            }
            if (i<s.length() && ch()=='}') i++;
            return m;
        }
        java.util.List<Object> arr(){
            java.util.List<Object> a=new ArrayList<>();
            i++; ws();
            while (i<s.length() && ch()!=']'){
                a.add(val()); ws();
                if (ch()==','){ i++; ws(); }
            }
            if (i<s.length() && ch()==']') i++;
            return a;
        }
        String str(){
            StringBuilder b=new StringBuilder();
            if (ch()=='"') i++;
            while (i<s.length() && ch()!='"'){
                char c = s.charAt(i++);
                if (c=='\\' && i<s.length()){
                    char n = s.charAt(i++);
                    switch (n){
                        case '"': case '\\': case '/': b.append(n); break;
                        case 'b': b.append('\b'); break;
                        case 'f': b.append('\f'); break;
                        case 'n': b.append('\n'); break;
                        case 'r': b.append('\r'); break;
                        case 't': b.append('\t'); break;
                        default: b.append(n);
                    }
                } else b.append(c);
            }
            if (i<s.length() && ch()=='"') i++;
            return b.toString();
        }
        Boolean bool(){
            if (s.startsWith("true", i)){ i+=4; return Boolean.TRUE; }
            if (s.startsWith("false", i)){ i+=5; return Boolean.FALSE; }
            return Boolean.FALSE;
        }
        Number num(){
            int j=i;
            while (i<s.length() && "-+0123456789.eE".indexOf(s.charAt(i))>=0) i++;
            String seg = s.substring(j,i);
            try {
                if (seg.contains(".")||seg.contains("e")||seg.contains("E")) return Double.parseDouble(seg);
                return Long.parseLong(seg);
            } catch(Exception e){ return 0; }
        }
    }
}