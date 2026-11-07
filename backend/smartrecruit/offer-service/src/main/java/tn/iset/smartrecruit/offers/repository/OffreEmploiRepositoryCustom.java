package tn.iset.smartrecruit.offers.repository;

import tn.iset.smartrecruit.offers.dto.OffreFilter;
import tn.iset.smartrecruit.offers.model.OffreEmploi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OffreEmploiRepositoryCustom {
    Page<OffreEmploi> search(OffreFilter filter, Pageable pageable);
}