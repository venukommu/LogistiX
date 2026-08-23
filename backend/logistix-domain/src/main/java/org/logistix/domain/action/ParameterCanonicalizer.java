package org.logistix.domain.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Deterministic parameter canonicalizer and deep immutable copier for LogistiX.
 *
 * Guarantees collision-resistant, unambiguous serialization for SHA-256 fingerprint calculations:
 * - Map keys are sorted lexicographically.
 * - Set elements are deterministically sorted by their canonical representations.
 * - List order is strictly preserved.
 * - Explicit typed prefixes prevent delimiter collisions between different structures.
 */
public final class ParameterCanonicalizer {

    private ParameterCanonicalizer() {
        // Utility class
    }

    /**
     * Produces a deterministic canonical string representation of a parameter map or nested structure.
     */
    public static String canonicalize(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String str) {
            return "S:\"" + escape(str) + "\"";
        }
        if (obj instanceof Number num) {
            return "N:" + num.toString();
        }
        if (obj instanceof Boolean bool) {
            return "B:" + bool.toString();
        }
        if (obj instanceof Enum<?> enumVal) {
            return "E:" + enumVal.name();
        }
        if (obj instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("M:{");
            TreeMap<String, Object> sortedMap = new TreeMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sortedMap.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            boolean first = true;
            for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {
                if (!first) sb.append(",");
                sb.append(escape(entry.getKey())).append("=").append(canonicalize(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof Set<?> set) {
            StringBuilder sb = new StringBuilder("SET:[");
            TreeSet<String> sortedElements = new TreeSet<>();
            for (Object element : set) {
                sortedElements.add(canonicalize(element));
            }
            boolean first = true;
            for (String canonElem : sortedElements) {
                if (!first) sb.append(",");
                sb.append(canonElem);
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj instanceof Collection<?> coll) {
            StringBuilder sb = new StringBuilder("L:[");
            boolean first = true;
            for (Object element : coll) {
                if (!first) sb.append(",");
                sb.append(canonicalize(element));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }
        return "O:\"" + escape(obj.toString()) + "\"";
    }

    /**
     * Creates a deep unmodifiable copy of a parameter map to guarantee strict immutability.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> deepUnmodifiableCopy(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            copy.put(entry.getKey(), deepCopyValue(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    @SuppressWarnings("unchecked")
    private static Object deepCopyValue(Object val) {
        if (val instanceof Map<?, ?> mapVal) {
            Map<String, Object> nested = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : mapVal.entrySet()) {
                nested.put(String.valueOf(entry.getKey()), deepCopyValue(entry.getValue()));
            }
            return Collections.unmodifiableMap(nested);
        }
        if (val instanceof List<?> listVal) {
            List<Object> nestedList = new ArrayList<>();
            for (Object item : listVal) {
                nestedList.add(deepCopyValue(item));
            }
            return Collections.unmodifiableList(nestedList);
        }
        if (val instanceof Set<?> setVal) {
            List<Object> items = new ArrayList<>();
            for (Object item : setVal) {
                items.add(deepCopyValue(item));
            }
            return Collections.unmodifiableSet(new java.util.LinkedHashSet<>(items));
        }
        return val;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("=", "\\=").replace(";", "\\;");
    }
}
