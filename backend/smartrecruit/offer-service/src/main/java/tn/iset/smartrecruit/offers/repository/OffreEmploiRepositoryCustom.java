package tn.iset.smartrecruit.offers.repository;

import tn.iset.smartrecruit.offers.dto.OffreSkillsView;
import tn.iset.smartrecruit.offers.dto.SkillCount;
import tn.iset.smartrecruit.offers.dto.OffreFilter;
import tn.iset.smartrecruit.offers.model.OffreEmploi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OffreEmploiRepositoryCustom {
    Page<OffreEmploi> search(OffreFilter filter, Pageable pageable);
    List<SkillCount> aggregatePublishedSkills(String query, int limit);
    List<OffreSkillsView> publishedSkillOffers(int limit);
}