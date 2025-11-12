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
        return dictionary.get(word);
    }

    public List<SlangWord> findByDefinition(String keyword) {
        List<SlangWord> result = new ArrayList<>();
        for (SlangWord sw : dictionary.values()) {
            for (String def : sw.getDefinitions()) {
                if (def.toLowerCase().contains(keyword.toLowerCase())) {
                    result.add(sw);
                    break;
                }
            }
        }
        return result;
    }

    public void clear() { dictionary.clear(); }
    public Map<String, SlangWord> getAll() { return dictionary; }
}
