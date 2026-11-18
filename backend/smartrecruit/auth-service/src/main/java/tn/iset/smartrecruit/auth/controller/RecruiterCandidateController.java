package tn.iset.smartrecruit.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import tn.iset.smartrecruit.auth.dto.RecruiterCandidateResponse;
import tn.iset.smartrecruit.auth.service.RecruiterCandidateService;

@RestController
@RequestMapping("/api/recruteur/candidatures")
@RequiredArgsConstructor
public class RecruiterCandidateController {
    private final RecruiterCandidateService service;

    @GetMapping("/{applicationId}/candidate")
    public RecruiterCandidateResponse candidate(
            @PathVariable String applicationId, HttpServletRequest request) {
        return service.forApplication(applicationId, request.getHeader(HttpHeaders.AUTHORIZATION));
    }
}
