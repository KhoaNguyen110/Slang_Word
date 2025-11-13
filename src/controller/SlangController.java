package src.controller;

import src.model.SlangDictionary;
import src.model.SlangWord;
import src.model.SlangDAO;
import java.io.IOException;
import java.util.Map;
import java.util.List;

public class SlangController {
    private SlangDictionary dict;

    public SlangController() {
        dict = SlangDictionary.getInstance(); // Singleton pattern
        try {
            SlangDAO.load(dict);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SlangWord searchByWord(String word) {
        return dict.findByWord(word);
    }

    public List<SlangWord> searchByDefinition(String keyword) {
        return dict.findByDefinition(keyword);
    }

    public void addSlang(String word, String definition) {
        dict.addSlang(new SlangWord(word, java.util.List.of(definition)));
    }

    public Map<String, SlangWord> getAllSlang() {
        return dict.getAll();
    }
}
