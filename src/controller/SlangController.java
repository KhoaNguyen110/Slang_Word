package src.controller;

import src.model.SlangDictionary;
import src.model.SlangWord;
import src.model.SlangDAO;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller (MVC) for Slang Dictionary.
 *
 * - Wraps SlangDictionary singleton and SlangDAO persistence.
 * - Provides add/edit/delete/reset/random operations for the View layer.
 *
 * Usage notes:
 * - After loading data, call backupOriginal() once if you want to be able to reset to the loaded snapshot.
 * - addSlang(...) has two overloads:
 *     * addSlang(word, definitionsRaw) -> attempts to add; if exists returns AddResult.EXISTS.
 *     * addSlang(word, definitionsRaw, option) -> performs overwrite/duplicate/add according to option.
 *
 * - Methods try to persist changes via SlangDAO.save(...) where appropriate; IOExceptions are caught and printed.
 */
public class SlangController {
    private final SlangDictionary dict;

    public enum AddOption {
        OVERWRITE,
        DUPLICATE,
        CANCEL
    }

    public enum AddResult {
        ADDED,
        OVERWRITTEN,
        DUPLICATED,
        EXISTS,     // exists but caller didn't choose option
        FAILED
    }

    public SlangController() {
        dict = SlangDictionary.getInstance(); // Singleton pattern
        try {
            SlangDAO.load(dict);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Search / Read ---
    public SlangWord searchByWord(String word) {
        return dict.findByWord(word);
    }

    public List<SlangWord> searchByDefinition(String keyword) {
        return dict.findByDefinition(keyword);
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
     * Simple add. If the word already exists, returns AddResult.EXISTS and does nothing.
     * definitionsRaw can contain multiple defs separated by '|' or newline.
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
            SlangWord sw = new SlangWord(key, defs);
            dict.addSlang(sw);
            persist();
            return AddResult.ADDED;
        }
    }

    /**
     * Add with chosen option when there is an existing word.
     * - OVERWRITE: replace definitions
     * - DUPLICATE: append definitions to existing definitions
     * - CANCEL: do nothing
     */
    public AddResult addSlang(String word, String definitionsRaw, AddOption option) {
        if (word == null || word.trim().isEmpty() || definitionsRaw == null || definitionsRaw.trim().isEmpty()) {
            return AddResult.FAILED;
        }
        String key = word.trim();
        SlangWord existing = dict.findByWord(key);
        List<String> defs = parseDefinitions(definitionsRaw);

        if (existing == null) {
            SlangWord sw = new SlangWord(key, defs);
            dict.addSlang(sw);
            persist();
            return AddResult.ADDED;
        } else {
            if (option == AddOption.OVERWRITE) {
                existing.setDefinitions(defs);
                dict.addSlang(existing); // put back (handles same key)
                persist();
                return AddResult.OVERWRITTEN;
            } else if (option == AddOption.DUPLICATE) {
                // Append new defs (avoid duplicates in list)
                List<String> list = existing.getDefinitions();
                for (String d : defs) {
                    if (!list.contains(d)) list.add(d);
                }
                dict.addSlang(existing);
                persist();
                return AddResult.DUPLICATED;
            } else {
                return AddResult.FAILED;
            }
        }
    }

    // --- Edit ---
    /**
     * Edit an existing slang entry.
     * If oldWord doesn't exist, returns false.
     * If newWord differs from oldWord, this will remove the old entry and insert the updated one using newWord as key.
     */
    public boolean editSlang(String oldWord, String newWord, String definitionsRaw) {
        if (oldWord == null || newWord == null || definitionsRaw == null) return false;
        SlangWord existing = dict.findByWord(oldWord);
        if (existing == null) return false;

        List<String> newDefs = parseDefinitions(definitionsRaw);
        // Create updated SlangWord (or modify existing)
        existing.setWord(newWord.trim());
        existing.setDefinitions(newDefs);
        // Use dictionary edit method to handle key change
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
            // Best-effort persistence — print stack trace but don't crash the UI
            e.printStackTrace();
        }
    }
}