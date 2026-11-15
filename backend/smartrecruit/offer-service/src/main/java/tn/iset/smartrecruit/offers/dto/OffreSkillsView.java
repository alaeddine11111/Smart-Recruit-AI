package tn.iset.smartrecruit.offers.dto;

import tn.iset.smartrecruit.offers.model.ContractType;

import java.util.List;

public record OffreSkillsView(
        String id,
        String titre,
        String entrepriseNom,
        String localisation,
        ContractType typeContrat,
        List<String> competences) {}
