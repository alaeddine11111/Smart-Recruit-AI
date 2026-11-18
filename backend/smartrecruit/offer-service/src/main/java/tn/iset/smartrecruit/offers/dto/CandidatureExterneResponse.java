package tn.iset.smartrecruit.offers.dto;

import tn.iset.smartrecruit.offers.model.CandidatureExterne;
import tn.iset.smartrecruit.offers.model.ExternalApplicationStatus;

import java.time.Instant;

public record CandidatureExterneResponse(
        String id,
        String offreId,
        String titre,
        String entreprise,
        String urlExterne,
        String source,
        Instant dateClic,
        ExternalApplicationStatus statut
) {
    public static CandidatureExterneResponse from(CandidatureExterne candidature) {
        return new CandidatureExterneResponse(
                candidature.getId(),
                candidature.getOffreId(),
                candidature.getTitre(),
                candidature.getEntreprise(),
                candidature.getUrlExterne(),
                candidature.getSource(),
                candidature.getDateClic(),
                candidature.getStatut()
        );
    }
}
