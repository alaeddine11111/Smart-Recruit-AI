package tn.iset.smartrecruit.offers.controller;

import tn.iset.smartrecruit.offers.dto.*;
import tn.iset.smartrecruit.offers.model.*;
import tn.iset.smartrecruit.offers.service.OffreEmploiService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

/** Candidate browsing – published offers only.
 *  GET /api/offres?titre=java&localisation=tunis&competences=java,spring&typeContrat=CDI */
@RestController
@RequestMapping("/api/offres")
@RequiredArgsConstructor
@Validated
public class OffrePublicController {

    private static final int MAX_SKILL_INDEX_OFFERS = 1000;

    private final OffreEmploiService service;

    @GetMapping
    public PageResponse<OffreEmploi> search(@RequestParam(required = false) String titre,
                                            @RequestParam(required = false) String localisation,
                                            @RequestParam(required = false) List<String> competences,
                                            @RequestParam(required = false) ContractType typeContrat,
                                            @PageableDefault(size = 10, sort = "datePublication", direction = Sort.Direction.DESC) Pageable p) {
        return service.searchPublished(new OffreFilter(titre, localisation, competences, typeContrat, null, null), p);
    }

    @GetMapping("/competences")
    public List<String> competences(@RequestParam(required = false) String q,
                                    @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
        return service.publishedSkillStats(q, limit).stream()
                .map(SkillCount::competence)
                .toList();
    }

    @GetMapping("/competences/stats")
    public List<SkillCount> competencesStats(@RequestParam(required = false) String q,
                                             @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
        return service.publishedSkillStats(q, limit);
    }

    @GetMapping("/index-competences")
    public List<OffreSkillsView> indexCompetences() {
        return service.publishedSkillOffers(MAX_SKILL_INDEX_OFFERS);
    }

    @GetMapping("/{id}")
    public OffreEmploi get(@PathVariable String id) { return service.getPublished(id); }
}
