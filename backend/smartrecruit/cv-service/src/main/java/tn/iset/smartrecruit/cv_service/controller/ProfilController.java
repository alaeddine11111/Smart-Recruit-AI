package tn.iset.smartrecruit.cv_service.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.iset.smartrecruit.cv_service.model.Profil;
import tn.iset.smartrecruit.cv_service.service.ProfilService;

@RestController
@RequestMapping("/api/profil")
public class ProfilController {
    private final ProfilService service;

    public ProfilController(ProfilService service) { this.service = service; }

    @GetMapping
    public Profil get(Authentication auth) { return service.get(auth.getName()); }

    @PutMapping
    public Profil save(Authentication auth, @RequestBody Profil body) {
        return service.enregistrer(auth.getName(), body, false); // toute modification annule la validation
    }

    @PostMapping("/valider")
    public Profil valider(Authentication auth) { return service.valider(auth.getName()); }
}