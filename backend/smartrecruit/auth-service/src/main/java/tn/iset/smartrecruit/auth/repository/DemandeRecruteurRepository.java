package tn.iset.smartrecruit.auth.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.iset.smartrecruit.auth.model.DemandeRecruteur;
import tn.iset.smartrecruit.auth.model.StatutDemande;

import java.util.List;
import java.util.Optional;

public interface DemandeRecruteurRepository extends MongoRepository<DemandeRecruteur, String> {
    boolean existsByEmailAndStatut(String email, StatutDemande statut);
    Optional<DemandeRecruteur> findFirstByEmailOrderByDateDemandeDesc(String email);
    List<DemandeRecruteur> findByStatutOrderByDateDemandeDesc(StatutDemande statut);
    List<DemandeRecruteur> findAllByOrderByDateDemandeDesc();
}