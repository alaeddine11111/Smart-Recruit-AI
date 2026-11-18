package tn.iset.smartrecruit.offers.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.iset.smartrecruit.offers.model.Candidature;

import java.util.List;
import java.util.Optional;

public interface CandidatureRepository extends MongoRepository<Candidature, String> {
    Optional<Candidature> findByCandidatIdAndOffreId(String candidatId, String offreId);
    List<Candidature> findByCandidatIdOrderByDateCandidatureDesc(String candidatId);
    List<Candidature> findByOffreIdIn(List<String> offreIds);
}
