package tn.iset.smartrecruit.offers.external;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * Base class for free APIs that cannot search server-side or have strict rate limits.
 * It downloads the board once, keeps it in memory for a while, and filters locally.
 */
@Slf4j
public abstract class SnapshotProvider implements ExternalJobProvider {

    protected record Entry(ExternalOffre offer, String haystack, String location) {}

    private static final int PAGE_SIZE = 20;
    private static final long RETRY_AFTER_ERROR_MS = 10 * 60 * 1000L;

    private final long ttlMs;
    private List<Entry> snapshot = List.of();
    private long nextRefresh = 0;

    protected SnapshotProvider(long ttlMs) { this.ttlMs = ttlMs; }

    /** Downloads the whole board. All strings in Entry.haystack/location must be lowercase. */
    protected abstract List<Entry> fetch() throws Exception;

    @Override
    public List<ExternalOffre> search(String keywords, String location, int page) {
        String[] tokens = keywords == null || keywords.isBlank()
                ? new String[0] : keywords.toLowerCase().trim().split("\\s+");
        String loc = location == null ? "" : location.toLowerCase().trim();

        List<ExternalOffre> matches = snapshot().stream()
                .filter(e -> Arrays.stream(tokens).allMatch(t -> e.haystack().contains(t)))
                .filter(e -> loc.isEmpty() || e.location().contains(loc))
                .map(Entry::offer)
                .toList();

        int from = (Math.max(page, 1) - 1) * PAGE_SIZE;
        if (from >= matches.size()) return List.of();
        return matches.subList(from, Math.min(from + PAGE_SIZE, matches.size()));
    }

    private synchronized List<Entry> snapshot() {
        long now = System.currentTimeMillis();
        if (now >= nextRefresh) {
            try {
                snapshot = fetch();
                nextRefresh = now + ttlMs;
                log.info("{}: loaded {} offers", name(), snapshot.size());
            } catch (Exception ex) {
                nextRefresh = now + RETRY_AFTER_ERROR_MS;   // do not hammer a failing/limiting API
                log.warn("{}: refresh failed ({}), keeping previous data", name(), ex.getMessage());
            }
        }
        return snapshot;
    }
}
