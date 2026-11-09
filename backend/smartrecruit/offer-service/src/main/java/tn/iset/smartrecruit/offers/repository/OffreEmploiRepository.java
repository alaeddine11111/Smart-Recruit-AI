package tn.iset.smartrecruit.offers.repository;

import tn.iset.smartrecruit.offers.model.OffreEmploi;
import tn.iset.smartrecruit.offers.model.OfferStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface OffreEmploiRepository extends MongoRepository<OffreEmploi, String>, OffreEmploiRepositoryCustom {
    Optional<OffreEmploi> findByIdAndStatut(String id, OfferStatus statut);
    List<OffreEmploi> findByEntrepriseId(String entrepriseId);
}
