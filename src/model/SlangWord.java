package src.model;

import java.util.List;

public class SlangWord {
    private String word;
    private List<String> definitions;

    public SlangWord(String word, List<String> definitions) {
        this.word = word;
        this.definitions = definitions;
    }

    public String getWord() { return word; }
    public List<String> getDefinitions() { return definitions; }
    public void setDefinitions(List<String> definitions) { this.definitions = definitions; }

    @Override
    public String toString() {
        return word + " = " + String.join(" | ", definitions);
    }
}

