package tn.iset.smartrecruit.cv_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.iset.smartrecruit.cv_service.model.Profil;

import java.util.Optional;

public interface ProfilRepository extends MongoRepository<Profil, String> {
    Optional<Profil> findByCandidatId(String candidatId);
}