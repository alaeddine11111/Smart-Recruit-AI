package tn.iset.smartrecruit.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.iset.smartrecruit.auth.dto.DemandeRecruteurResponse;
import tn.iset.smartrecruit.auth.dto.RefusRequest;
import tn.iset.smartrecruit.auth.dto.UserResponse;
import tn.iset.smartrecruit.auth.model.StatutDemande;
import tn.iset.smartrecruit.auth.service.AuthService;

import java.util.List;

/** Admin-only (see SecurityConfig: /api/admin/** requires ROLE_ADMIN). */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;

    @GetMapping("/utilisateurs")
    public List<UserResponse> utilisateurs() {
        return authService.listUsers();
    }

    /** Optional filter: ?statut=EN_ATTENTE | ACCEPTEE | REFUSEE */
    @GetMapping("/demandes-recruteur")
    public List<DemandeRecruteurResponse> demandes(@RequestParam(required = false) StatutDemande statut) {
        return authService.listDemandes(statut);
    }

    @PostMapping("/demandes-recruteur/{id}/accepter")
    public DemandeRecruteurResponse accepter(@PathVariable String id) {
        return authService.accepterDemande(id);
    }

    @PostMapping("/demandes-recruteur/{id}/refuser")
    public DemandeRecruteurResponse refuser(@PathVariable String id,
                                            @Valid @RequestBody(required = false) RefusRequest req) {
        return authService.refuserDemande(id, req == null ? null : req.motif());
    }
}