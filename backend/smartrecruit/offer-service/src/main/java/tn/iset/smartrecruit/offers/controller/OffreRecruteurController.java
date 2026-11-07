package tn.iset.smartrecruit.offers.controller;

import tn.iset.smartrecruit.offers.dto.*;
import tn.iset.smartrecruit.offers.model.*;
import tn.iset.smartrecruit.offers.service.OffreEmploiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recruteur/offres")
@RequiredArgsConstructor
public class OffreRecruteurController {

    private final OffreEmploiService service;

    @GetMapping
    public PageResponse<OffreEmploi> mine(@RequestParam(required = false) OfferStatus statut,
                                          @RequestParam(required = false) String titre,
                                          @PageableDefault(size = 10, sort = "dateCreation", direction = Sort.Direction.DESC) Pageable p) {
        return service.searchMine(new OffreFilter(titre, null, null, null, statut, null), p);
    }

    @GetMapping("/{id}")
    public OffreEmploi get(@PathVariable String id) { return service.getMine(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OffreEmploi create(@Valid @RequestBody OffreRequest r) { return service.create(r); }

    @PutMapping("/{id}")
    public OffreEmploi update(@PathVariable String id, @Valid @RequestBody OffreRequest r) { return service.update(id, r); }

    @PatchMapping("/{id}/statut")
    public OffreEmploi status(@PathVariable String id, @RequestParam OfferStatus value) { return service.changeStatus(id, value); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) { service.delete(id); }
}
