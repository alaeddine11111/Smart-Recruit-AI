package tn.iset.smartrecruit.cv_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.iset.smartrecruit.cv_service.model.CvDocument;

import java.util.Optional;

public interface CvRepository extends MongoRepository<CvDocument, String> {
    Optional<CvDocument> findByCandidatId(String candidatId);
}