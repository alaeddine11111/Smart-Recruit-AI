package tn.iset.smartrecruit.offers.repository;

import tn.iset.smartrecruit.offers.model.Entreprise;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface EntrepriseRepository extends MongoRepository<Entreprise, String> {
    Optional<Entreprise> findByRecruteurId(String recruteurId);
}
