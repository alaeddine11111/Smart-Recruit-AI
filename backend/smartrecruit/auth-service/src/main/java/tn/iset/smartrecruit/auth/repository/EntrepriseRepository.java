package tn.iset.smartrecruit.auth.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.iset.smartrecruit.auth.model.Entreprise;

import java.util.Optional;

public interface EntrepriseRepository extends MongoRepository<Entreprise, String> {
    Optional<Entreprise> findByNomIgnoreCase(String nom);
}
