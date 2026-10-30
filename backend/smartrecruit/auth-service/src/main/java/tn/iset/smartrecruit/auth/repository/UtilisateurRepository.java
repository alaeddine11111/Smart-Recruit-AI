package tn.iset.smartrecruit.auth.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.iset.smartrecruit.auth.model.Utilisateur;

import java.util.Optional;

public interface UtilisateurRepository extends MongoRepository<Utilisateur, String> {
    Optional<Utilisateur> findByEmail(String email);
    boolean existsByEmail(String email);
}
