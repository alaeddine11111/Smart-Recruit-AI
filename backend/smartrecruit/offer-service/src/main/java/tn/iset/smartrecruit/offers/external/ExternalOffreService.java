package tn.iset.smartrecruit.offers.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregates all enabled providers, then translates the offers to English.
 * Remotive and Arbeitnow cache their own data (see SnapshotProvider) and translations are cached per
 * offer (see TranslationClient), so this method is not cached as a whole: a failed translation
 * must not be remembered.
 */
@Service
@Slf4j
public class ExternalOffreService {

    private final List<ExternalJobProvider> providers;
    private final TranslationClient translation;

    public ExternalOffreService(List<ExternalJobProvider> providers, TranslationClient translation) {
        this.providers = providers;
        this.translation = translation;
    }

    public List<ExternalOffre> search(String keywords, String location, int page) {
        List<ExternalOffre> all = new ArrayList<>();
        for (ExternalJobProvider p : providers) {
            try {
                p.search(keywords, location, page).stream()
                        .filter(o -> o.urlExterne() != null && o.urlExterne().startsWith("http"))
                        .forEach(all::add);
            } catch (Exception e) {
                log.warn("External provider {} failed: {}", p.name(), e.getMessage());
            }
        }
        return translation.toEnglish(all);
    }
}
