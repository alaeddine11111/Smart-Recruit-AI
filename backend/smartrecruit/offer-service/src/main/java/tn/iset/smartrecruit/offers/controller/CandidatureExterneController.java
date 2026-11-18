package tn.iset.smartrecruit.offers.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tn.iset.smartrecruit.offers.dto.CandidatureExterneRequest;
import tn.iset.smartrecruit.offers.dto.CandidatureExterneResponse;
import tn.iset.smartrecruit.offers.service.CandidatureExterneService;

import java.util.List;

@RestController
@RequestMapping("/api/candidatures/externes")
@RequiredArgsConstructor
public class CandidatureExterneController {
    private final CandidatureExterneService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CandidatureExterneResponse recordRedirect(@Valid @RequestBody CandidatureExterneRequest request) {
        return service.recordRedirect(request);
    }

    @GetMapping
    public List<CandidatureExterneResponse> mesCandidatures() {
        return service.mesCandidatures();
    }
}
