package src.controller;

import src.model.SlangDictionary;
import src.model.SlangWord;
import src.model.SlangDAO;
import src.model.SearchHistoryEntry;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SlangController (MVC)
 * - Wrapper quanh SlangDictionary + SlangDAO
 * - Quản lý lịch sử tìm kiếm: record, get, delete by index, clear all
 *
 * NOTE: ensure we never attempt to modify immutable lists returned by SlangWord.getDefinitions().
 * All modifications create a new mutable ArrayList and then call setDefinitions(...) on the SlangWord.
 */
public class SlangController {
    private final SlangDictionary dict;
    private final List<SearchHistoryEntry> history = new ArrayList<>();

    public enum AddOption {
        OVERWRITE,
        DUPLICATE,
        CANCEL
    }

    public enum AddResult {
        ADDED,
        OVERWRITTEN,
        DUPLICATED,
        EXISTS,
        FAILED
    }

    public SlangController() {
        dict = SlangDictionary.getInstance(); // Singleton pattern
        try {
            SlangDAO.load(dict);
            dict.backupOriginal();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        try {
            SlangDAO.save(dict);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Random ---
    public SlangWord getRandomSlang() {
        return dict.getRandomSlang();
    }

    // --- Add ---
    /**
     * Add new slang. If exists, return EXISTS (do not modify existing).
     * definitionsRaw can contain '|' or newline separators.
     */
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
            // Ensure we pass a mutable list to SlangWord (avoid immutable lists)
            SlangWord sw = new SlangWord(key, new ArrayList<>(defs));
            dict.addSlang(sw);
            persist();
            return AddResult.ADDED;
        }
    }

    /**
     * Add with chosen option when there is an existing word.
     * - OVERWRITE: replace definitions
     * - DUPLICATE: append definitions to existing definitions (avoiding duplicates)
     */
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
                // Replace with a new mutable list
                existing.setDefinitions(new ArrayList<>(defs));
                dict.addSlang(existing);
                persist();
                return AddResult.OVERWRITTEN;
            } else if (option == AddOption.DUPLICATE) {
                // Create a new mutable list copying current definitions, then append new defs avoiding repetitions
                List<String> merged = new ArrayList<>();
                List<String> current = existing.getDefinitions();
                if (current != null) merged.addAll(current);
                for (String d : defs) {
                    if (!merged.contains(d)) merged.add(d);
                }
                existing.setDefinitions(merged);
                dict.addSlang(existing);
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
        // set new word and new mutable definitions list
        existing.setWord(newWord.trim());
        existing.setDefinitions(new ArrayList<>(newDefs));
        boolean ok = dict.editSlang(oldWord, existing);
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
    // Record a history entry; newest entries are at index 0
    private void recordHistory(String query, String type, List<String> resultWords) {
        if (query == null) query = "";
        SearchHistoryEntry entry = new SearchHistoryEntry(query, type == null ? "" : type, resultWords == null ? Collections.emptyList() : new ArrayList<>(resultWords));
        history.add(0, entry);
    }

    // Return unmodifiable list (newest-first)
    public List<SearchHistoryEntry> getSearchHistory() {
        return Collections.unmodifiableList(history);
    }

    // Delete an entry by its index in the history list (index 0 is newest)
    public boolean deleteHistoryEntry(int index) {
        if (index < 0 || index >= history.size()) return false;
        history.remove(index);
        return true;
    }

    // Clear all history
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}