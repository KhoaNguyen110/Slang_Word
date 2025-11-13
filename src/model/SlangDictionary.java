package src.model;

import java.util.*;

public class SlangDictionary {
    private static SlangDictionary instance;
    private Map<String, SlangWord> dictionary;

    private SlangDictionary() {
        dictionary = new HashMap<>();
    }

    public static SlangDictionary getInstance() {
        if (instance == null) instance = new SlangDictionary();
        return instance;
    }

    public void addSlang(SlangWord slang) {
        dictionary.put(slang.getWord(), slang);
    }

    public SlangWord findByWord(String word) {
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
}
