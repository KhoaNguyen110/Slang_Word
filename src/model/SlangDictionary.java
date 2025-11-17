package src.model;

import java.io.IOException;
import java.util.*;

/**
 * SlangDictionary with deep-copy backup and restore + Definition inverted index.
 */
public class SlangDictionary {
    private static SlangDictionary instance;

    // wordKey -> SlangWord
    private Map<String, SlangWord> dictionary;

    // Inverted index for definitions: token -> set of word (slang)
    private Map<String, Set<String>> defIndex;

    private Map<String, SlangWord> originalSnapshot; // deep-copy snapshot
    private final Random random = new Random();

    private SlangDictionary() {
        dictionary = new HashMap<>();
        defIndex = new HashMap<>();
    }

    public static SlangDictionary getInstance() {
        if (instance == null) instance = new SlangDictionary();
        return instance;
    }

    // ---------------- Basic operations ----------------

    // Put slang; if existed, update index by removing old then adding new
    public void addSlang(SlangWord slang) {
        if (slang == null || slang.getWord() == null) return;
        String key = slang.getWord();
        SlangWord old = dictionary.get(key);
        if (old != null) {
            DefinitionIndex.removeFromIndex(defIndex, old);
        }
        dictionary.put(key, slang);
        DefinitionIndex.addToIndex(defIndex, slang);
    }

    public SlangWord findByWord(String word) {
        if (word == null) return null;
        String key = word.trim();
        // key được giữ nguyên (case-sensitive) theo dữ liệu file,
        // nếu muốn case-insensitive, có thể chuẩn hóa key ngay khi load vào.
        SlangWord direct = dictionary.get(key);
        if (direct != null) return direct;
        // fallback: tìm ignore-case
        for (Map.Entry<String, SlangWord> e : dictionary.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key)) return e.getValue();
        }
        return null;
    }

    public List<SlangWord> findByDefinition(String keyword) {
        List<SlangWord> result = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) return result;

        String lowered = DefinitionIndex.removeDiacritics(keyword).toLowerCase(Locale.ROOT).trim();

        // 1) Lấy tokens từ keyword -> ứng viên nhanh từ index
        List<String> tokens = DefinitionIndex.tokenize(keyword);
        Set<String> candidates;
        if (tokens.isEmpty()) {
            // Nếu không có token hợp lệ (vd keyword quá ngắn), fallback: duyệt nhanh nhưng vẫn có thể chậm
            candidates = dictionary.keySet();
        } else {
            candidates = DefinitionIndex.candidateByTokens(defIndex, tokens);
            // Nếu không có ứng viên nào, trả rỗng luôn
            if (candidates.isEmpty()) return result;
        }

        // 2) Lọc cuối bằng substring để đảm bảo đúng yêu cầu đề
        for (String w : candidates) {
            SlangWord sw = dictionary.get(w);
            if (DefinitionIndex.containsSubstring(sw, lowered)) {
                result.add(sw);
            }
        }
        return result;
    }

    public void clear() { dictionary.clear(); defIndex.clear(); }

    public Map<String, SlangWord> getAll() { return dictionary; }

    public Map<String, Set<String>> getDefIndex() { return defIndex; }

    public void setDefIndex(Map<String, Set<String>> idx) {
        this.defIndex = (idx == null) ? new HashMap<>() : idx;
    }

    // ---------------- Backup / reset ----------------

    /**
     * Create a deep-copy snapshot of current dictionary.
     */
    public void backupOriginal() {
        originalSnapshot = new HashMap<>();
        for (Map.Entry<String, SlangWord> e : dictionary.entrySet()) {
            SlangWord copy = deepCopySlang(e.getValue());
            originalSnapshot.put(copy.getWord(), copy);
        }
    }

    /**
     * Restore dictionary from the snapshot (if present) and rebuild index.
     */
    public void resetToOriginal() {
        if (originalSnapshot == null) return;
        dictionary.clear();
        for (Map.Entry<String, SlangWord> e : originalSnapshot.entrySet()) {
            SlangWord copy = deepCopySlang(e.getValue());
            dictionary.put(copy.getWord(), copy);
        }
        // rebuild index from snapshot
        defIndex = DefinitionIndex.build(dictionary);
    }

    // Edit: remove old key and insert newSlang under its own word (handles rename)
    public boolean editSlang(String oldWord, SlangWord newSlang) {
        if (oldWord == null || newSlang == null) return false;
        if (!dictionary.containsKey(oldWord)) return false;
        SlangWord old = dictionary.get(oldWord);
        dictionary.remove(oldWord);
        DefinitionIndex.removeFromIndex(defIndex, old);

        dictionary.put(newSlang.getWord(), newSlang);
        DefinitionIndex.addToIndex(defIndex, newSlang);
        return true;
    }

    public boolean deleteSlang(String word) {
        if (word == null) return false;
        SlangWord removed = dictionary.remove(word);
        if (removed != null) {
            DefinitionIndex.removeFromIndex(defIndex, removed);
            return true;
        }
        return false;
    }

    public SlangWord getRandomSlang() {
        if (dictionary.isEmpty()) return null;
        List<String> keys = new ArrayList<>(dictionary.keySet());
        return dictionary.get(keys.get(random.nextInt(keys.size())));
    }

    // Helper to deep-copy a SlangWord.
    private SlangWord deepCopySlang(SlangWord original) {
        if (original == null) return null;
        List<String> defs = original.getDefinitions();
        List<String> defsCopy = (defs == null) ? new ArrayList<>() : new ArrayList<>(defs);
        return new SlangWord(original.getWord(), defsCopy);
    }

    // Build index khi cần (không tự save)
    public void buildIndex() {
        defIndex = DefinitionIndex.build(dictionary);
    }

    // Try load index từ file, nếu không có thì build
    public void loadOrBuildIndex() throws IOException {
        Map<String, Set<String>> idx = DefinitionIndex.load();
        if (idx == null) {
            buildIndex();
            DefinitionIndex.save(defIndex);
        } else {
            setDefIndex(idx);
        }
    }
}