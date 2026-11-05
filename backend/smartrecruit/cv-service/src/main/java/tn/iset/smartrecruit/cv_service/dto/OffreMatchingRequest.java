package tn.iset.smartrecruit.cv_service.dto;

import java.util.List;

public record OffreMatchingRequest(
        String id,
        String titre,
        String source,
        String url,
        List<String> competencesRequises) {
}
