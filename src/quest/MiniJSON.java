package quest;

import java.util.*;

public class MiniJSON {
    private final String s;
    private int i = 0;

    public MiniJSON(String s) { this.s = s; }

    public static Object parse(String s) {
        return new MiniJSON(s).value();
    }

    private void skip() {
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') i++;
            else break;
        }
    }

    private Object value() {
        skip();
        if (i >= s.length()) return null;
        char c = s.charAt(i);
        if (c == '{') return obj();
        if (c == '[') return arr();
        if (c == '"' || c == '\'') return str();
        if (c == 't' || c == 'f') return bool();
        if (c == 'n') { i += 4; return null; }
        return num();
    }

    private Map<String, Object> obj() {
        Map<String, Object> m = new LinkedHashMap<>();
        i++; skip();
        if (i < s.length() && s.charAt(i) == '}') { i++; return m; }
        while (i < s.length()) {
            String k = (String) str();
            skip();
            if (i < s.length() && s.charAt(i) == ':') i++;
            Object v = value();
            m.put(k, v);
            skip();
            if (i < s.length() && s.charAt(i) == ',') { i++; continue; }
            if (i < s.length() && s.charAt(i) == '}') { i++; break; }
        }
        return m;
    }

    private List<Object> arr() {
        List<Object> a = new ArrayList<>();
        i++; skip();
        if (i < s.length() && s.charAt(i) == ']') { i++; return a; }
        while (i < s.length()) {
            Object v = value();
            a.add(v);
            skip();
            if (i < s.length() && s.charAt(i) == ',') { i++; continue; }
            if (i < s.length() && s.charAt(i) == ']') { i++; break; }
        }
        return a;
    }

    private Object str() {
        skip();
        char q = s.charAt(i++);
        StringBuilder sb = new StringBuilder();
        while (i < s.length()) {
            char c = s.charAt(i++);
            if (c == q) break;
            if (c == '\\') {
                if (i >= s.length()) break;
                char e = s.charAt(i++);
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (i + 3 < s.length()) {
                            String hex = s.substring(i, i + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        }
                        break;
                    default: sb.append(e);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Boolean bool() {
        if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
        if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
        return Boolean.FALSE;
    }

    private Number num() {
        int j = i;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isDigit(c) || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') i++;
            else break;
        }
        String sub = s.substring(j, i);
        if (sub.contains(".") || sub.contains("e") || sub.contains("E")) {
            try { return Double.parseDouble(sub); } catch (Exception e) { return 0; }
        } else {
            try { return Long.parseLong(sub); } catch (Exception e) { return 0; }
        }
    }

    @SuppressWarnings("unchecked")
    public static String getString(Map<String, Object> m, String k, String def) {
        Object v = m.get(k);
        return v instanceof String ? (String) v : def;
    }

    public static int getInt(Map<String, Object> m, String k, int def) {
        Object v = m.get(k);
        if (v instanceof Number) return ((Number) v).intValue();
        return def;
    }

    public static boolean getBool(Map<String, Object> m, String k, boolean def) {
        Object v = m.get(k);
        if (v instanceof Boolean) return (Boolean) v;
        return def;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getArray(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v instanceof List ? (List<Object>) v : Collections.emptyList();
    }
}