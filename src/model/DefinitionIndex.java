package src.model;

import java.io.*;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * DefinitionIndex
 * - Xây inverted index cho definition: token (lowercased, no diacritics) -> set slang words
 * - Lưu/Load index ra file để lần chạy sau không cần build lại.
 *
 * Lưu ý:
 * - Index tăng tốc tìm kiếm; kết quả cuối cùng vẫn lọc bằng substring để đảm bảo "definition có chứa keyword".
 */
public final class DefinitionIndex {

    public static final String INDEX_FILE = "data/def_index.ser";

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    private DefinitionIndex() {}

    // Build index từ toàn bộ dictionary
    public static Map<String, Set<String>> build(Map<String, SlangWord> dict) {
        Map<String, Set<String>> index = new HashMap<>();
        for (SlangWord sw : dict.values()) {
            addToIndex(index, sw);
        }
        return index;
    }

    // Thêm một slang vào index
    public static void addToIndex(Map<String, Set<String>> index, SlangWord sw) {
        if (sw == null || sw.getWord() == null) return;
        String wordKey = sw.getWord();
        List<String> defs = sw.getDefinitions();
        if (defs == null) return;

        for (String def : defs) {
            if (def == null) continue;
            for (String tok : tokenize(def)) {
                index.computeIfAbsent(tok, k -> new HashSet<>()).add(wordKey);
            }
        }
    }

    // Gỡ slang khỏi index (dùng khi edit/delete)
    public static void removeFromIndex(Map<String, Set<String>> index, SlangWord sw) {
        if (sw == null || sw.getWord() == null) return;
        String wordKey = sw.getWord();
        List<String> defs = sw.getDefinitions();
        if (defs == null) return;

        for (String def : defs) {
            if (def == null) continue;
            for (String tok : tokenize(def)) {
                Set<String> bucket = index.get(tok);
                if (bucket != null) {
                    bucket.remove(wordKey);
                    if (bucket.isEmpty()) index.remove(tok);
                }
            }
        }
    }

    // Khi đổi định nghĩa/đổi từ: gỡ bản cũ, thêm bản mới
    public static void updateOnEdit(Map<String, Set<String>> index, SlangWord oldSw, SlangWord newSw) {
        if (oldSw != null) removeFromIndex(index, oldSw);
        if (newSw != null) addToIndex(index, newSw);
    }

    // Lưu index ra file
    public static void save(Map<String, Set<String>> index) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(INDEX_FILE)))) {
            oos.writeObject(convertToSerializable(index));
        }
    }

    // Load index từ file, trả về null nếu không có
    @SuppressWarnings("unchecked")
    public static Map<String, Set<String>> load() throws IOException {
        File f = new File(INDEX_FILE);
        if (!f.exists()) return null;
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)))) {
            Map<String, List<String>> ser = (Map<String, List<String>>) ois.readObject();
            Map<String, Set<String>> idx = new HashMap<>();
            for (Map.Entry<String, List<String>> e : ser.entrySet()) {
                idx.put(e.getKey(), new HashSet<>(e.getValue()));
            }
            return idx;
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to deserialize index", e);
        }
    }

    // Tìm các ứng viên từ index bằng cách giao các bucket của từng token
    public static Set<String> candidateByTokens(Map<String, Set<String>> index, List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) return Collections.emptySet();
        List<Set<String>> buckets = new ArrayList<>();
        for (String t : tokens) {
            Set<String> b = index.get(t);
            if (b == null || b.isEmpty()) return Collections.emptySet();
            buckets.add(b);
        }
        // Giao dần dần để giảm tập
        Set<String> result = new HashSet<>(buckets.get(0));
        for (int i = 1; i < buckets.size(); i++) {
            result.retainAll(buckets.get(i));
            if (result.isEmpty()) break;
        }
        return result;
    }

    // Tokenize + normalize: lowercase, bỏ dấu, tách theo non-alnum
    public static List<String> tokenize(String text) {
        if (text == null) return Collections.emptyList();
        String norm = removeDiacritics(text).toLowerCase(Locale.ROOT);
        String[] parts = NON_ALNUM.split(norm);
        List<String> tokens = Arrays.stream(parts)
                .map(String::trim)
                .filter(s -> s.length() >= 2) // bỏ token 1 ký tự để index gọn hơn
                .collect(Collectors.toList());
        return tokens;
    }

    public static String removeDiacritics(String s) {
        String tmp = Normalizer.normalize(s, Normalizer.Form.NFD);
        return tmp.replaceAll("\\p{M}+", "");
    }

    // Kiểm tra substring thực tế (đảm bảo đúng yêu cầu đề)
    public static boolean containsSubstring(SlangWord sw, String keywordLower) {
        if (sw == null || keywordLower == null || keywordLower.isEmpty()) return false;
        if (sw.getDefinitions() == null) return false;
        for (String d : sw.getDefinitions()) {
            if (d != null) {
                String norm = removeDiacritics(d).toLowerCase(Locale.ROOT);
                if (norm.contains(keywordLower)) return true;
            }
        }
        return false;
    }

    // Lưu Set<String> dưới dạng List<String> để đảm bảo serializable đơn giản, giảm rủi ro
    private static Map<String, List<String>> convertToSerializable(Map<String, Set<String>> index) {
        Map<String, List<String>> ser = new HashMap<>();
        for (Map.Entry<String, Set<String>> e : index.entrySet()) {
            ser.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        return ser;
    }
}