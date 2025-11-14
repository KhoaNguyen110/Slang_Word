package src.model;

import java.util.*;

/**
 * SlangDictionary with deep-copy backup and restore.
 */
public class SlangDictionary {
    private static SlangDictionary instance;
    private Map<String, SlangWord> dictionary;
    private Map<String, SlangWord> originalSnapshot; // deep-copy snapshot
    private final Random random = new Random();

    private SlangDictionary() {
        dictionary = new HashMap<>();
    }

    public static SlangDictionary getInstance() {
        if (instance == null) instance = new SlangDictionary();
        return instance;
    }

    // Basic operations
    public void addSlang(SlangWord slang) {
        dictionary.put(slang.getWord(), slang);
    }

    public SlangWord findByWord(String word) {
        if (word == null) return null;
        String upper = word.toUpperCase().trim();
        if (dictionary.containsKey(upper)) return dictionary.get(upper);
        return dictionary.get(word);
    }

    public List<SlangWord> findByDefinition(String keyword) {
        List<SlangWord> result = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) return result;

        String lowerKey = keyword.toLowerCase().trim();
        for (SlangWord slang : dictionary.values()) {
            for (String def : slang.getDefinitions()) {
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

    // --- New: deep-copy backup / reset ---

    /**
     * Create a deep-copy snapshot of current dictionary.
     * Each SlangWord is duplicated (new instance) with a new List of definitions,
     * so later mutations on current dictionary won't affect the snapshot.
     *
     * Call this once after initial load from file.
     */
    public void backupOriginal() {
        originalSnapshot = new HashMap<>();
        for (Map.Entry<String, SlangWord> e : dictionary.entrySet()) {
            SlangWord copy = deepCopySlang(e.getValue());
            originalSnapshot.put(copy.getWord(), copy);
        }
    }

    /**
     * Restore dictionary from the snapshot (if present).
     * Restores new copies of each saved SlangWord into current dictionary.
     */
    public void resetToOriginal() {
        if (originalSnapshot == null) return;
        dictionary.clear();
        for (Map.Entry<String, SlangWord> e : originalSnapshot.entrySet()) {
            SlangWord copy = deepCopySlang(e.getValue());
            dictionary.put(copy.getWord(), copy);
        }
    }

    // Edit: remove old key and insert newSlang under its own word (handles rename)
    public boolean editSlang(String oldWord, SlangWord newSlang) {
        if (oldWord == null || newSlang == null) return false;
        if (!dictionary.containsKey(oldWord)) return false;
        dictionary.remove(oldWord);
        dictionary.put(newSlang.getWord(), newSlang);
        return true;
    }

    public boolean deleteSlang(String word) {
        if (word == null) return false;
        return dictionary.remove(word) != null;
    }

    public SlangWord getRandomSlang() {
        if (dictionary.isEmpty()) return null;
        return dictionary.get(new ArrayList<>(dictionary.keySet()).get(random.nextInt(dictionary.size())));
    }

    // Helper to deep-copy a SlangWord. Assumes SlangWord has constructor SlangWord(String, List<String>)
    private SlangWord deepCopySlang(SlangWord original) {
        if (original == null) return null;
        List<String> defs = original.getDefinitions();
        List<String> defsCopy = defs == null ? new ArrayList<>() : new ArrayList<>(defs);
        // create a new SlangWord instance using the copied list
        return new SlangWord(original.getWord(), defsCopy);
    }
}