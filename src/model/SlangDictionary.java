package src.model;

import java.util.*;

public class SlangDictionary {
    private static SlangDictionary instance;
    private Map<String, SlangWord> dictionary;
    private Map<String, SlangWord> originalSnapshot; // backup để reset
    private final Random random = new Random();

    private SlangDictionary() {
        dictionary = new HashMap<>();
    }

    public static SlangDictionary getInstance() {
        if (instance == null) instance = new SlangDictionary();
        return instance;
    }

    // Thêm / ghi đè (put)
    public void addSlang(SlangWord slang) {
        dictionary.put(slang.getWord(), slang);
    }

    // Tìm theo word
    public SlangWord findByWord(String word) {
        if (word == null) return null;
        String upper = word.toUpperCase().trim();
        if (dictionary.containsKey(upper)) return dictionary.get(upper);
        return dictionary.get(word);
    }

    // Tìm theo definition
    public List<SlangWord> findByDefinition(String keyword) {
        List<SlangWord> result = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) return result;

        String lowerKey = keyword.toLowerCase().trim();
        for (SlangWord slang : dictionary.values()) {
            for (String def : slang.getDefinitions()) {
                // Tách nhiều nghĩa, ví dụ "Cool | excellent"
                String[] parts = def.split("\\|");
                for (String p : parts) {
                    if (p.trim().toLowerCase().contains(lowerKey) || p.trim().contains(keyword.trim())) {
                        result.add(slang);
                        break;
                    }
                }
            }
        }
        return result;
    }

    public void clear() { dictionary.clear(); }

    public Map<String, SlangWord> getAll() { return dictionary; }

    // --- New methods ---

    // Lưu snapshot ban đầu (gọi 1 lần sau khi load dữ liệu từ file)
    public void backupOriginal() {
        originalSnapshot = new HashMap<>();
        for (Map.Entry<String, SlangWord> e : dictionary.entrySet()) {
            // Nếu cần deep copy SlangWord thì làm ở đây. Giả sử SlangWord copy constructor không có,
            // ta giữ reference (nếu bạn muốn copy thực sự: tạo SlangWord mới với list copy).
            originalSnapshot.put(e.getKey(), e.getValue());
        }
    }

    // Khôi phục từ snapshot (nếu đã backup)
    public void resetToOriginal() {
        if (originalSnapshot == null) return;
        dictionary.clear();
        for (Map.Entry<String, SlangWord> e : originalSnapshot.entrySet()) {
            dictionary.put(e.getKey(), e.getValue());
        }
    }

    // Edit: thay entry cũ bằng entry mới (có thể đổi key)
    public boolean editSlang(String oldWord, SlangWord newSlang) {
        if (oldWord == null || newSlang == null) return false;
        if (!dictionary.containsKey(oldWord)) return false;
        // Nếu đổi key, remove old, put new
        dictionary.remove(oldWord);
        dictionary.put(newSlang.getWord(), newSlang);
        return true;
    }

    // Delete
    public boolean deleteSlang(String word) {
        if (word == null) return false;
        return dictionary.remove(word) != null;
    }

    // Get random slang
    public SlangWord getRandomSlang() {
        if (dictionary.isEmpty()) return null;
        List<SlangWord> list = new ArrayList<>(dictionary.values());
        return list.get(random.nextInt(list.size()));
    }
}