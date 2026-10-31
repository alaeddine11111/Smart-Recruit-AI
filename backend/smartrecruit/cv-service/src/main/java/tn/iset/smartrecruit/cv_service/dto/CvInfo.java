package tn.iset.smartrecruit.cv_service.dto;

import java.time.Instant;

public record CvInfo(String nomFichier, long taille, Instant dateDepot) {}