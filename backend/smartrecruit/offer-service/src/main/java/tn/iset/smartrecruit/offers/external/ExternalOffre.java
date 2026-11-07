package tn.iset.smartrecruit.offers.external;

public record ExternalOffre(String id, String titre, String entreprise, String localisation,
                            String description, String salaire, String typeContrat,
                            String dateMiseAJour, String urlExterne, String source) {}

