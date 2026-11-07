package tn.iset.smartrecruit.offers.dto;
import tn.iset.smartrecruit.offers.model.ContractType;
import tn.iset.smartrecruit.offers.model.OfferStatus;

import java.util.List;

/** All fields optional. statut / recruteurId are set by the service, never by public callers. */
public record OffreFilter(String titre, String localisation, List<String> competences,
                          ContractType typeContrat, OfferStatus statut, String recruteurId) {}

