package tn.iset.smartrecruit.offers.controller;

import tn.iset.smartrecruit.offers.dto.*;
import tn.iset.smartrecruit.offers.model.*;
import tn.iset.smartrecruit.offers.service.OffreEmploiService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Candidate browsing – published offers only.
 *  GET /api/offres?titre=java&localisation=tunis&competences=java,spring&typeContrat=CDI */
@RestController
@RequestMapping("/api/offres")
@RequiredArgsConstructor
public class OffrePublicController {

    private final OffreEmploiService service;

    @GetMapping
    public PageResponse<OffreEmploi> search(@RequestParam(required = false) String titre,
                                            @RequestParam(required = false) String localisation,
                                            @RequestParam(required = false) List<String> competences,
                                            @RequestParam(required = false) ContractType typeContrat,
                                            @PageableDefault(size = 10, sort = "datePublication", direction = Sort.Direction.DESC) Pageable p) {
        return service.searchPublished(new OffreFilter(titre, localisation, competences, typeContrat, null, null), p);
    }

    @GetMapping("/{id}")
    public OffreEmploi get(@PathVariable String id) { return service.getPublished(id); }
}
