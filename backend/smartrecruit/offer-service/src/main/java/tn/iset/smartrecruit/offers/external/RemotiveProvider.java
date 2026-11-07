package tn.iset.smartrecruit.offers.external;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Remotive: free public API, no key. Remote jobs (tech focus), usable from anywhere including Tunisia.
 * Rules from Remotive: very few fetches per day, max 2 requests/minute, link back to the source (done via "source" + url).
 * We download the software-dev board once and cache it for 6 hours.
 */
@Component
@ConditionalOnProperty(name = "external-jobs.remotive.enabled", havingValue = "true")
public class RemotiveProvider extends SnapshotProvider {

    private final RestClient http = RestClient.create();

    public RemotiveProvider() { super(6 * 60 * 60 * 1000L); }

    @Override public String name() { return "Remotive"; }

    @Override
    protected List<Entry> fetch() {
        JsonNode res = http.get()
                .uri("https://remotive.com/api/remote-jobs?category=software-dev")
                .retrieve().body(JsonNode.class);

        List<Entry> out = new ArrayList<>();
        if (res == null || !res.has("jobs")) return out;

        for (JsonNode j : res.get("jobs")) {
            String title = j.path("title").asText("");
            String company = j.path("company_name").asText("");
            String loc = j.path("candidate_required_location").asText("");
            String desc = ExternalText.clean(j.path("description").asText(""), 1000);
            String tags = ExternalText.join(j.path("tags"));
            String url = j.path("url").asText("");
            if (url.isEmpty()) continue;

            ExternalOffre o = new ExternalOffre(
                    "remotive-" + j.path("id").asText(),
                    title, company, loc.isEmpty() ? "Remote" : "Remote - " + loc,
                    ExternalText.clean(desc, 300),
                    ExternalText.blankToNull(j.path("salary").asText("")),
                    ExternalText.blankToNull(j.path("job_type").asText("")),
                    ExternalText.blankToNull(j.path("publication_date").asText("")),
                    url, name());

            out.add(new Entry(o,
                    (title + " " + company + " " + tags + " " + desc).toLowerCase(),
                    ("remote " + loc).toLowerCase()));
        }
        return out;
    }
}
