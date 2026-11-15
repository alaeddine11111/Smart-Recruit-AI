package tn.iset.smartrecruit.offers.external;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Translates external offers to English by calling POST /translate on the OCR service (Gemini).
 * Results are cached per offer id, so each offer is translated only once.
 * If the OCR service is down or slow, the original (untranslated) offers are returned.
 */
@Component
@Slf4j
public class TranslationClient {

    private static final int BATCH_SIZE = 8;
    private static final int MAX_CACHE = 5000;

    private record Translated(String titre, String description, String lang) {}

    private final boolean enabled;
    private final RestClient http;
    private final Map<String, Translated> cache = new ConcurrentHashMap<>();

    public TranslationClient(@Value("${external-jobs.translation.enabled:false}") boolean enabled,
                             @Value("${external-jobs.translation.url:http://localhost:8083}") String url) {
        this.enabled = enabled;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(90_000);
        this.http = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }

        public List<ExternalOffre> toEnglish(List<ExternalOffre> offers) {
        log.info("Translation called: enabled={}, offers={}", enabled, offers.size());
        if (!enabled || offers.isEmpty()) return offers;

        List<ExternalOffre> missing = offers.stream().filter(o -> !cache.containsKey(o.id())).toList();
        log.info("Translation: {} offers to translate", missing.size());
        partition(missing, BATCH_SIZE).parallelStream().forEach(this::translateBatch);

        return offers.stream().map(o -> {
            Translated t = cache.get(o.id());
            return t == null ? o : o.withTranslation(t.titre(), t.description(), t.lang());
        }).toList();
    }

    private void translateBatch(List<ExternalOffre> batch) {
        try {
            List<Map<String, String>> items = batch.stream()
                    .map(o -> Map.of("id", o.id(), "titre", nz(o.titre()), "description", nz(o.description())))
                    .toList();

            JsonNode res = http.post().uri("/translate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("target", "en", "items", items))
                    .retrieve().body(JsonNode.class);

            if (res == null || !res.has("items")) return;
            log.info("Translation batch ok: {} items translated", res.get("items").size());
            if (cache.size() > MAX_CACHE) cache.clear();

            for (JsonNode n : res.get("items")) {
                String id = n.path("id").asText("");
                if (id.isEmpty()) continue;
                cache.put(id, new Translated(n.path("titre").asText(""),
                        n.path("description").asText(""), n.path("sourceLanguage").asText("")));
            }
        } catch (Exception e) {
            log.warn("Translation batch failed, keeping original text: {}", e.getMessage());
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) out.add(list.subList(i, Math.min(i + size, list.size())));
        return out;
    }
}
