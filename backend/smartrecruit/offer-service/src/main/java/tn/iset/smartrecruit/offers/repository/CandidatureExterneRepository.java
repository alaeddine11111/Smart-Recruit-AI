package tn.iset.smartrecruit.offers.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.iset.smartrecruit.offers.model.CandidatureExterne;

import java.util.List;

public interface CandidatureExterneRepository extends MongoRepository<CandidatureExterne, String> {
    List<CandidatureExterne> findByCandidatIdOrderByDateClicDesc(String candidatId);
}
