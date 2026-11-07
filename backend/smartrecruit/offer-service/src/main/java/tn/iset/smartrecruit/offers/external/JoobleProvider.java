package tn.iset.smartrecruit.offers.external;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/** Jooble: POST https://jooble.org/api/{key}  body {keywords, location, page}. Request a free key on jooble.org. */
@Component
@ConditionalOnProperty(name = "external-jobs.jooble.enabled", havingValue = "true")
public class JoobleProvider implements ExternalJobProvider {

    private final RestClient http;

    public JoobleProvider(@Value("${external-jobs.jooble.api-key}") String apiKey) {
        this.http = RestClient.create("https://jooble.org/api/" + apiKey);
    }

    @Override public String name() { return "Jooble"; }

    @Override
    public List<ExternalOffre> search(String keywords, String location, int page) {
        Map<String, String> body = new HashMap<>();
        body.put("keywords", keywords == null ? "" : keywords);
        body.put("location", location == null ? "" : location);
        body.put("page", String.valueOf(Math.max(page, 1)));

        JsonNode res = http.post().contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
        List<ExternalOffre> out = new ArrayList<>();
        if (res == null || !res.has("jobs")) return out;
        for (JsonNode j : res.get("jobs")) {
            out.add(new ExternalOffre("jooble-" + j.path("id").asText(), j.path("title").asText(),
                    j.path("company").asText(null), j.path("location").asText(null),
                    j.path("snippet").asText(null), j.path("salary").asText(null),
                    j.path("type").asText(null), j.path("updated").asText(null),
                    j.path("link").asText(), name()));
        }
        return out;
    }
}
