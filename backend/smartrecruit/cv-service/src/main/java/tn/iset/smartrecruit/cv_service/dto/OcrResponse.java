package tn.iset.smartrecruit.cv_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tn.iset.smartrecruit.cv_service.model.Profil;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OcrResponse(String texteOCR, boolean ocrUtilise, int pages, Donnees donnees) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Donnees(String titre, String resume, String telephone, String localisation,
            List<Profil.Competence> competences, List<Profil.Experience> experiences,
            List<Profil.Formation> formations, List<Profil.Langue> langues) {}
}