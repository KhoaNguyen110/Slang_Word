package src.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * SearchHistoryEntry (simplified)
 * - Lưu lịch sử chỉ gồm: query, type ("WORD" hoặc "DEFINITION"), resultWords
 * - Không lưu timestamp theo yêu cầu.
 */
public class SearchHistoryEntry {
    private final String query;
    private final String type;
    private final List<String> resultWords;

    public SearchHistoryEntry(String query, String type, List<String> resultWords) {
        this.query = query == null ? "" : query;
        this.type = type == null ? "" : type;
        this.resultWords = resultWords == null ? List.of() : List.copyOf(resultWords);
    }

    public String getQuery() {
        return query;
    }

    public String getType() {
        return type;
    }

    public List<String> getResultWords() {
        return resultWords;
    }

    @Override
    public String toString() {
        String results = (resultWords == null || resultWords.isEmpty())
                ? "(no results)"
                : resultWords.stream().collect(Collectors.joining(", "));
        return String.format("%s: \"%s\" -> %s", type, query, results);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SearchHistoryEntry)) return false;
        SearchHistoryEntry that = (SearchHistoryEntry) o;
        return Objects.equals(query, that.query) &&
                Objects.equals(type, that.type) &&
                Objects.equals(resultWords, that.resultWords);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, type, resultWords);
    }
}