package tn.iset.smartrecruit.cv_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.iset.smartrecruit.cv_service.model.SessionPreparation;

import java.util.List;
import java.util.Optional;

public interface SessionPreparationRepository extends MongoRepository<SessionPreparation, String> {
    List<SessionPreparation> findByCandidatIdOrderByDateCreationDesc(String candidatId);
    Optional<SessionPreparation> findByIdAndCandidatId(String id, String candidatId);
}
