package tn.iset.smartrecruit.cv_service.dto;

import java.util.List;

public record ResultatMatching(
        OffreMatchingRequest offre,
        int score,
        List<String> competencesCorrespondantes,
        List<String> competencesManquantes,
        String explication) {
}
