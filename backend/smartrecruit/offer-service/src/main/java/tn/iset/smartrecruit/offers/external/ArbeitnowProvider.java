package tn.iset.smartrecruit.offers.external;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Arbeitnow: free public API, no key. Jobs in Europe (mostly Germany) plus remote roles.
 * The API has no keyword search, so we download the first pages once, cache them for 1 hour
 * and filter locally by keyword / location (use "remote" as location to see remote jobs).
 */
@Component
@ConditionalOnProperty(name = "external-jobs.arbeitnow.enabled", havingValue = "true")
public class ArbeitnowProvider extends SnapshotProvider {

    private static final int PAGES = 3;
    private final RestClient http = RestClient.create();

    public ArbeitnowProvider() { super(60 * 60 * 1000L); }

    @Override public String name() { return "Arbeitnow"; }

    @Override
    protected List<Entry> fetch() {
        List<Entry> out = new ArrayList<>();
        for (int p = 1; p <= PAGES; p++) {
            JsonNode res = http.get()
                    .uri("https://www.arbeitnow.com/api/job-board-api?page=" + p)
                    .retrieve().body(JsonNode.class);
            if (res == null || !res.has("data") || res.get("data").isEmpty()) break;

            for (JsonNode j : res.get("data")) {
                String title = j.path("title").asText("");
                String company = j.path("company_name").asText("");
                String loc = j.path("location").asText("");
                boolean remote = j.path("remote").asBoolean(false);
                String desc = ExternalText.clean(j.path("description").asText(""), 1000);
                String tags = ExternalText.join(j.path("tags"));
                String types = ExternalText.join(j.path("job_types"));
                String url = j.path("url").asText("");
                if (url.isEmpty()) continue;

                long ts = j.path("created_at").asLong(0);
                String date = ts > 0 ? Instant.ofEpochSecond(ts).toString() : null;

                ExternalOffre o = new ExternalOffre(
                        "arbeitnow-" + j.path("slug").asText(),
                        title, company, remote ? (loc.isEmpty() ? "Remote" : loc + " (remote)") : loc,
                        ExternalText.clean(desc, 300), null,
                        ExternalText.blankToNull(types), date, url, name());

                out.add(new Entry(o,
                        (title + " " + company + " " + tags + " " + desc).toLowerCase(),
                        (loc + (remote ? " remote" : "")).toLowerCase()));
            }
        }
        return out;
    }
}
