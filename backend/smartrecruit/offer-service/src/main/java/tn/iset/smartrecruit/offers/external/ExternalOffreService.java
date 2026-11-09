package tn.iset.smartrecruit.offers.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Aggregates all enabled providers. A failing provider never breaks the page. */
@Service
@Slf4j
public class ExternalOffreService {

    private final List<ExternalJobProvider> providers;

    public ExternalOffreService(List<ExternalJobProvider> providers) { this.providers = providers; }

    /** Cached to protect free-tier API quotas (needs @EnableCaching, see ExternalJobsConfig). */
    @Cacheable(value = "external-offers", key = "#keywords + '|' + #location + '|' + #page")
    public List<ExternalOffre> search(String keywords, String location, int page) {
        List<ExternalOffre> all = new ArrayList<>();
        for (ExternalJobProvider p : providers) {
            try {
                p.search(keywords, location, page).stream()
                        .filter(o -> o.urlExterne() != null && o.urlExterne().startsWith("http")) // http(s) links only
                        .forEach(all::add);
            } catch (Exception e) {
                log.warn("External provider {} failed: {}", p.name(), e.getMessage());
            }
        }
        return all;
    }
}
