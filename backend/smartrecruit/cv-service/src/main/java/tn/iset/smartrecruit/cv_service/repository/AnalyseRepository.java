package tn.iset.smartrecruit.cv_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.iset.smartrecruit.cv_service.model.AnalyseCv;

import java.util.Optional;

public interface AnalyseRepository extends MongoRepository<AnalyseCv, String> {
    Optional<AnalyseCv> findFirstByCandidatIdOrderByDateDesc(String candidatId);
}