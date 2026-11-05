package tn.iset.smartrecruit.cv_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tn.iset.smartrecruit.cv_service.dto.OffreMatchingRequest;
import tn.iset.smartrecruit.cv_service.dto.ResultatMatching;
import tn.iset.smartrecruit.cv_service.model.Profil;
import tn.iset.smartrecruit.cv_service.service.MatchingService;
import tn.iset.smartrecruit.cv_service.service.ProfilService;

import java.util.List;

@RestController
@RequestMapping("/api/matching")
public class MatchingController {
    private final MatchingService matchingService;
    private final ProfilService profilService;

    public MatchingController(MatchingService matchingService, ProfilService profilService) {
        this.matchingService = matchingService;
        this.profilService = profilService;
    }

    @PostMapping
    public List<ResultatMatching> evaluer(Authentication auth, @RequestBody List<OffreMatchingRequest> offres) {
        if (auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_CANDIDAT"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Réservé aux candidats.");
        }
        if (offres == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La liste des offres est obligatoire.");
        }
        if (offres.stream().anyMatch(offre -> offre == null
                || offre.id() == null || offre.id().isBlank()
                || offre.titre() == null || offre.titre().isBlank()
                || offre.competencesRequises() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chaque offre doit avoir un id, un titre et une liste de compétences requises.");
        }

        Profil profil = profilService.get(auth.getName());
        if (!profil.valide()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Validez votre profil et vos compétences avant de demander des recommandations.");
        }

        return matchingService.evaluerToutes(profil, offres);
    }
}
