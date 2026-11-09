package tn.iset.smartrecruit.offers.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.iset.smartrecruit.offers.dto.EntrepriseRequest;
import tn.iset.smartrecruit.offers.model.Entreprise;
import tn.iset.smartrecruit.offers.service.EntrepriseService;

@RestController
@RequiredArgsConstructor
public class EntrepriseController {

    private final EntrepriseService service;

    /** RECRUTEUR/ADMIN: my own company */
    @GetMapping("/api/recruteur/entreprise")
    public Entreprise mine() {
        return service.getMine();
    }

    /** RECRUTEUR/ADMIN: create or update my company */
    @PutMapping("/api/recruteur/entreprise")
    public Entreprise save(@Valid @RequestBody EntrepriseRequest r) {
        return service.upsertMine(r);
    }

    /** Any authenticated user: company card on the offer detail page */
    @GetMapping("/api/entreprises/{id}")
    public Entreprise byId(@PathVariable String id) {
        return service.getById(id);
    }
}