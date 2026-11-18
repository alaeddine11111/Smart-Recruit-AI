package tn.iset.smartrecruit.cv_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.iset.smartrecruit.cv_service.dto.CvInfo;
import tn.iset.smartrecruit.cv_service.model.Profil;
import tn.iset.smartrecruit.cv_service.service.RecruiterCandidateDataService;

@RestController
@RequestMapping("/api/recruteur/candidatures/{applicationId}")
public class RecruiterCandidateDataController {
    private final RecruiterCandidateDataService service;

    public RecruiterCandidateDataController(RecruiterCandidateDataService service) {
        this.service = service;
    }

    @GetMapping("/profil")
    public ResponseEntity<Profil> profile(
            @PathVariable String applicationId, HttpServletRequest request) {
        return service.profileForApplication(applicationId, request.getHeader(HttpHeaders.AUTHORIZATION))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/cv")
    public ResponseEntity<CvInfo> cv(
            @PathVariable String applicationId, HttpServletRequest request) {
        return service.cvForApplication(applicationId, request.getHeader(HttpHeaders.AUTHORIZATION))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
