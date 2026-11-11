package tn.iset.smartrecruit.auth.dto;

import tn.iset.smartrecruit.auth.model.DemandeRecruteur;
import tn.iset.smartrecruit.auth.model.StatutDemande;

import java.time.LocalDateTime;

public record DemandeRecruteurResponse(
        String id,
        String nom,
        String email,
        String poste,
        String entrepriseNom,
        String entrepriseDescription,
        String entrepriseSiteWeb,
        StatutDemande statut,
        String motifRefus,
        LocalDateTime dateDemande,
        LocalDateTime dateTraitement
) {
    public static DemandeRecruteurResponse from(DemandeRecruteur d) {
        return new DemandeRecruteurResponse(
                d.getId(), d.getNom(), d.getEmail(), d.getPoste(),
                d.getEntrepriseNom(), d.getEntrepriseDescription(), d.getEntrepriseSiteWeb(),
                d.getStatut(), d.getMotifRefus(), d.getDateDemande(), d.getDateTraitement());
    }
}