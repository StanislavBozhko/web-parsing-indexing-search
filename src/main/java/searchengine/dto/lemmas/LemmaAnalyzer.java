package searchengine.dto.lemmas;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import searchengine.model.PageDB;

import java.io.IOException;
import java.util.*;


public class LemmaAnalyzer {

    private final LuceneMorphology luceneMorphology;
    private static final String WORD_TYPE_REGEX = "\\W\\w&&[^а-яА-Я\\s]";
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "ЧАСТ"};

    public static LemmaAnalyzer getInstance() throws IOException {
        LuceneMorphology morphology = new RussianLuceneMorphology();
        return new LemmaAnalyzer(morphology);
    }

    private LemmaAnalyzer(LuceneMorphology luceneMorphology) {
        this.luceneMorphology = luceneMorphology;
    }

    private LemmaAnalyzer() {
        throw new RuntimeException("Disallow construct");
    }


    public Map<String, Integer> collectLemmasPage(PageDB pageDB) {
        String content = pageDB.getContent();
        StringBuilder stringBuilder = new StringBuilder(clearHTML(content, "title"));
        stringBuilder.append(clearHTML(content, "body"));
        return collectLemmas(stringBuilder.toString());
    }

    public Map<String, Integer> collectLemmas(String inputText) {

        String[] words = arrayContainsRussianWords(inputText);
        Map<String, Integer> lemmas = new HashMap<>();
        for (String word : words) {
            if (word.isBlank() || word.length() < 3) {
                continue;
            }

            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) {
                continue;
            }

            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }

            String normalWord = normalForms.get(0);
            if (lemmas.containsKey(normalWord)) {
                lemmas.put(normalWord, lemmas.get(normalWord) + 1);
            } else {
                lemmas.put(normalWord, 1);
            }
        }

        return lemmas;
    }

    /**
     * @param text текст из которого собираем все леммы
     * @return набор уникальных лемм найденных в тексте
     */
    public Set<String> getLemmaSet(String text, boolean onlyFirstNormalWord) {
        String[] textArray = arrayContainsRussianWords(text);
        Set<String> lemmaSet = new HashSet<>();
        HashMap<String, String> lemmaMap = new HashMap<>();
        for (String word : textArray) {
            if (word.length() > 2 && isCorrectWordForm(word)) {
                List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
                if (anyWordBaseBelongToParticle(wordBaseForms)) {
                    continue;
                }
                List<String> lemmaList = luceneMorphology.getNormalForms(word);
                if (lemmaList.size() == 0) {
                    continue;
                }
                if (onlyFirstNormalWord) {
                    lemmaSet.add(lemmaList.get(0));
                } else {
                    lemmaSet.addAll(lemmaList);
                }
            }
        }
        return lemmaSet;
    }

    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    private boolean hasParticleProperty(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }

    private String[] arrayContainsRussianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

    private boolean isCorrectWordForm(String word) {
        List<String> wordInfo = luceneMorphology.getMorphInfo(word);
        for (String morphInfo : wordInfo) {
            if (morphInfo.matches(WORD_TYPE_REGEX)) {
                return false;
            }
        }
        return true;
    }

    public String clearHTML(String content, String selector) {
        StringBuilder html = new StringBuilder();
        var doc = Jsoup.parse(content);
        var elements = doc.select(selector);
        for (Element el : elements) {
            html.append(el.html());
        }
        return Jsoup.parse(html.toString()).text();
    }

    public List<Integer> findPositionsLemmaInText(String content, String lemmaStr) {
        List<Integer> lemmaIndexList = new ArrayList<>();
        String[] words = content.toLowerCase(Locale.ROOT).split("\\p{Punct}|\\s");
        int index = 0;
        for (String word : words) {
            Set<String> lemmaStringSet = getLemmaSet(word, false);
            for (String lem : lemmaStringSet) {
                if (lem.equals(lemmaStr)) {
                    lemmaIndexList.add(index);
                }
            }
            index += word.length() + 1;
        }
        return lemmaIndexList;
    }
}
