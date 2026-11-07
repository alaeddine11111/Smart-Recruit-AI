package tn.iset.smartrecruit.offers.external;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Adzuna: GET https://api.adzuna.com/v1/api/jobs/{country}/search/{page}
 * Adzuna works per country; Tunisia was not in its country list when this was written
 * (verify on developer.adzuna.com). "fr" gives French-language jobs; treat it as a secondary source.
 */
@Component
@ConditionalOnProperty(name = "external-jobs.adzuna.enabled", havingValue = "true")
public class AdzunaProvider implements ExternalJobProvider {

    private final RestClient http = RestClient.create();
    private final String appId, appKey, country;

    public AdzunaProvider(@Value("${external-jobs.adzuna.app-id}") String appId,
                          @Value("${external-jobs.adzuna.app-key}") String appKey,
                          @Value("${external-jobs.adzuna.country:fr}") String country) {
        this.appId = appId; this.appKey = appKey; this.country = country;
    }

    @Override public String name() { return "Adzuna"; }

    @Override
    public List<ExternalOffre> search(String keywords, String location, int page) {
        UriComponentsBuilder b = UriComponentsBuilder
                .fromUriString("https://api.adzuna.com/v1/api/jobs/" + country + "/search/" + Math.max(page, 1))
                .queryParam("app_id", appId).queryParam("app_key", appKey).queryParam("results_per_page", 20);
        if (StringUtils.hasText(keywords)) b.queryParam("what", keywords);
        if (StringUtils.hasText(location)) b.queryParam("where", location);

        JsonNode res = http.get().uri(b.build().encode().toUri()).retrieve().body(JsonNode.class);
        List<ExternalOffre> out = new ArrayList<>();
        if (res == null || !res.has("results")) return out;
        for (JsonNode j : res.get("results")) {
            String salaire = j.hasNonNull("salary_min")
                    ? Math.round(j.get("salary_min").asDouble()) + " - " + Math.round(j.path("salary_max").asDouble()) : null;
            out.add(new ExternalOffre("adzuna-" + j.path("id").asText(), j.path("title").asText(),
                    j.path("company").path("display_name").asText(null),
                    j.path("location").path("display_name").asText(null),
                    j.path("description").asText(null), salaire,
                    j.path("contract_time").asText(null), j.path("created").asText(null),
                    j.path("redirect_url").asText(), name()));
        }
        return out;
    }
}
