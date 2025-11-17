package src.controller;

import src.model.SlangDictionary;
import src.model.SlangWord;
import src.model.SlangDAO;
import src.model.SearchHistoryEntry;
import src.model.DefinitionIndex;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SlangController (MVC)
 * - Wrapper quanh SlangDictionary + SlangDAO
 * - Quản lý lịch sử tìm kiếm
 * - Quản lý persist: dictionary + inverted index
 */
public class SlangController {
    private static final SlangController INSTANCE = new SlangController(true);
    private final List<SearchHistoryEntry> history = new ArrayList<>();
    private final SlangDictionary dict;

    public enum AddOption { OVERWRITE, DUPLICATE, CANCEL }
    public enum AddResult { ADDED, OVERWRITTEN, DUPLICATED, EXISTS, FAILED }

    public SlangController(boolean loadData) {
        dict = SlangDictionary.getInstance(); // Singleton pattern
        if (loadData) {
            try {
                SlangDAO.load(dict);              // load từ file data/slang.txt
                dict.loadOrBuildIndex();          // load index nếu có, nếu không build + save
                dict.backupOriginal();            // snapshot gốc sau khi load
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static SlangController getInstance() {
        return INSTANCE;
    }

    // --- Search / Read (ghi lịch sử) ---
    public SlangWord searchByWord(String word) {
        SlangWord res = dict.findByWord(word);
        List<String> found = res == null ? Collections.emptyList() : Collections.singletonList(res.getWord());
        recordHistory(word, "WORD", found);
        return res;
    }

    public List<SlangWord> searchByDefinition(String keyword) {
        List<SlangWord> res = dict.findByDefinition(keyword);
        List<String> found = res == null ? Collections.emptyList()
                : res.stream().map(SlangWord::getWord).collect(Collectors.toList());
        recordHistory(keyword, "DEFINITION", found);
        return res;
    }

    public Map<String, SlangWord> getAllSlang() {
        return dict.getAll();
    }

    // --- Backup / Reset ---
    public void backupOriginal() {
        dict.backupOriginal();
    }

    public void resetToOriginal() {
        dict.resetToOriginal();
        persist(); // save dict + index sau khi reset
    }

    // --- Random ---
    public SlangWord getRandomSlang() {
        return dict.getRandomSlang();
    }

    // --- Add ---
    public AddResult addSlang(String word, String definitionsRaw) {
        if (word == null || word.trim().isEmpty() || definitionsRaw == null || definitionsRaw.trim().isEmpty()) {
            return AddResult.FAILED;
        }
        String key = word.trim();
        SlangWord existing = dict.findByWord(key);
        List<String> defs = parseDefinitions(definitionsRaw);
        if (existing != null) {
            return AddResult.EXISTS;
        } else {
            SlangWord sw = new SlangWord(key, new ArrayList<>(defs));
            dict.addSlang(sw);
            persist();
            return AddResult.ADDED;
        }
    }

    public AddResult addSlang(String word, String definitionsRaw, AddOption option) {
        if (word == null || word.trim().isEmpty() || definitionsRaw == null || definitionsRaw.trim().isEmpty()) {
            return AddResult.FAILED;
        }
        String key = word.trim();
        SlangWord existing = dict.findByWord(key);
        List<String> defs = parseDefinitions(definitionsRaw);

        if (existing == null) {
            SlangWord sw = new SlangWord(key, new ArrayList<>(defs));
            dict.addSlang(sw);
            persist();
            return AddResult.ADDED;
        } else {
            if (option == AddOption.OVERWRITE) {
                existing.setDefinitions(new ArrayList<>(defs));
                dict.addSlang(existing); // addSlang sẽ tự update index (remove old + add new)
                persist();
                return AddResult.OVERWRITTEN;
            } else if (option == AddOption.DUPLICATE) {
                List<String> merged = new ArrayList<>();
                List<String> current = existing.getDefinitions();
                if (current != null) merged.addAll(current);
                for (String d : defs) {
                    if (!merged.contains(d)) merged.add(d);
                }
                existing.setDefinitions(merged);
                dict.addSlang(existing); // update index
                persist();
                return AddResult.DUPLICATED;
            } else {
                return AddResult.FAILED;
            }
        }
    }

    // --- Edit ---
    public boolean editSlang(String oldWord, String newWord, String definitionsRaw) {
        if (oldWord == null || newWord == null || definitionsRaw == null) return false;
        SlangWord existing = dict.findByWord(oldWord);
        if (existing == null) return false;

        List<String> newDefs = parseDefinitions(definitionsRaw);
        SlangWord edited = new SlangWord(newWord.trim(), new ArrayList<>(newDefs));
        boolean ok = dict.editSlang(oldWord, edited);
        if (ok) persist();
        return ok;
    }

    // --- Delete ---
    public boolean deleteSlang(String word) {
        if (word == null) return false;
        boolean ok = dict.deleteSlang(word);
        if (ok) persist();
        return ok;
    }

    // --- History management ---
    private void recordHistory(String query, String type, List<String> resultWords) {
        if (query == null) query = "";
        SearchHistoryEntry entry = new SearchHistoryEntry(query, type == null ? "" : type, resultWords == null ? Collections.emptyList() : new ArrayList<>(resultWords));
        history.add(0, entry);
    }

    public List<SearchHistoryEntry> getSearchHistory() {
        return Collections.unmodifiableList(history);
    }

    public boolean deleteHistoryEntry(int index) {
        if (index < 0 || index >= history.size()) return false;
        history.remove(index);
        return true;
    }

    public void clearSearchHistory() {
        history.clear();
    }

    // --- Helpers ---
    private List<String> parseDefinitions(String raw) {
        if (raw == null) return Collections.emptyList();
        String[] parts = raw.split("\\r?\\n|\\|");
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private void persist() {
        try {
            SlangDAO.save(dict);
            DefinitionIndex.save(dict.getDefIndex()); // LƯU index để lần sau không cần build lại
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}