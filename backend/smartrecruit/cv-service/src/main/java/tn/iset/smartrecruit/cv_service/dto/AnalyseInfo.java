package tn.iset.smartrecruit.cv_service.dto;

import tn.iset.smartrecruit.cv_service.model.AnalyseCv;

public record AnalyseInfo(AnalyseCv.Statut statut, String messageErreur,
                          String texteOCR, boolean ocrUtilise) {}