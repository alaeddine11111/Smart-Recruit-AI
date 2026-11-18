package tn.iset.smartrecruit.offers.dto;

public record RecruiterApplicationStats(
        long total,
        long pending,
        long accepted,
        long rejected,
        java.util.List<OfferApplicationStats> offers
) {
    public record OfferApplicationStats(
            String offreId,
            String titre,
            long total,
            long pending,
            long accepted,
            long rejected
    ) {
    }
}
