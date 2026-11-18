package tn.iset.smartrecruit.offers.dto;

import tn.iset.smartrecruit.offers.model.ApplicationStatus;
import tn.iset.smartrecruit.offers.model.Candidature;
import tn.iset.smartrecruit.offers.model.OffreEmploi;

import java.time.Instant;

public record CandidatureResponse(
        String id,
        String offreId,
        Instant dateCandidature,
        ApplicationStatus statut,
        OffreEmploi offre
) {
    public static CandidatureResponse from(Candidature candidature, OffreEmploi offre) {
        return new CandidatureResponse(
                candidature.getId(),
                candidature.getOffreId(),
                candidature.getDateCandidature(),
                candidature.getStatut(),
                offre
        );
    }
}
