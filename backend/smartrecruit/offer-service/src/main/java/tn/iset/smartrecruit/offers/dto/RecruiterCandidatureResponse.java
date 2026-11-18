package tn.iset.smartrecruit.offers.dto;

import tn.iset.smartrecruit.offers.model.ApplicationStatus;
import tn.iset.smartrecruit.offers.model.Candidature;
import tn.iset.smartrecruit.offers.model.OffreEmploi;

import java.time.Instant;

public record RecruiterCandidatureResponse(
        String id,
        String candidateEmail,
        String offreId,
        Instant dateCandidature,
        ApplicationStatus statut,
        OffreEmploi offre
) {
    public static RecruiterCandidatureResponse from(Candidature candidature, OffreEmploi offre) {
        return new RecruiterCandidatureResponse(
                candidature.getId(),
                candidature.getCandidatId(),
                candidature.getOffreId(),
                candidature.getDateCandidature(),
                candidature.getStatut(),
                offre
        );
    }
}
