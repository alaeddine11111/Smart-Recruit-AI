package tn.iset.smartrecruit.offers.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.iset.smartrecruit.offers.dto.ApplicationStatusUpdateRequest;
import tn.iset.smartrecruit.offers.dto.CandidateAccessResponse;
import tn.iset.smartrecruit.offers.dto.RecruiterApplicationStats;
import tn.iset.smartrecruit.offers.dto.RecruiterCandidatureResponse;
import tn.iset.smartrecruit.offers.model.ApplicationStatus;
import tn.iset.smartrecruit.offers.service.CandidatureService;

import java.util.List;

@RestController
@RequestMapping("/api/recruteur/candidatures")
@RequiredArgsConstructor
public class CandidatureRecruteurController {
    private final CandidatureService service;

    @GetMapping("/stats")
    public RecruiterApplicationStats stats() {
        return service.recruiterStats();
    }

    @GetMapping
    public List<RecruiterCandidatureResponse> list(
            @RequestParam(required = false) String offreId,
            @RequestParam(required = false) ApplicationStatus statut,
            @RequestParam(required = false) String candidate) {
        return service.recruiterApplications(offreId, statut, candidate);
    }

    @GetMapping("/{id}")
    public RecruiterCandidatureResponse detail(@PathVariable String id) {
        return service.recruiterApplication(id);
    }

    @PatchMapping("/{id}/statut")
    public RecruiterCandidatureResponse status(
            @PathVariable String id, @Valid @RequestBody ApplicationStatusUpdateRequest request) {
        return service.updateRecruiterStatus(id, request.statut());
    }

    @GetMapping("/{id}/candidate")
    public CandidateAccessResponse candidate(@PathVariable String id) {
        return service.candidateForRecruiter(id);
    }
}
