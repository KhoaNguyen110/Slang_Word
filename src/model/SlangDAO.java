package src.model;

import java.io.*;
import java.util.*;

public class SlangDAO {
    private static final String FILE_PATH = "data/slang.txt";

    public static void load(SlangDictionary dict) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.contains("`")) continue;
                String[] parts = line.split("`");
                if (parts.length < 2) continue;
                String word = parts[0].trim();
                List<String> defs = Arrays.asList(parts[1].split("\\|"));
                dict.addSlang(new SlangWord(word, defs));
            }
        }
    }

    public static void save(SlangDictionary dict) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (SlangWord sw : dict.getAll().values()) {
                bw.write(sw.getWord() + "`" + String.join("|", sw.getDefinitions()));
                bw.newLine();
            }
        }
    }
}
