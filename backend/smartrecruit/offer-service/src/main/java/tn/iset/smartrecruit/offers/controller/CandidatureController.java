package tn.iset.smartrecruit.offers.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tn.iset.smartrecruit.offers.dto.CandidatureResponse;
import tn.iset.smartrecruit.offers.service.CandidatureService;

import java.util.List;

@RestController
@RequestMapping("/api/candidatures")
@RequiredArgsConstructor
public class CandidatureController {
    private final CandidatureService service;

    @PostMapping("/{offreId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CandidatureResponse apply(@PathVariable String offreId) {
        return service.apply(offreId);
    }

    @GetMapping("/mes")
    public List<CandidatureResponse> mesCandidatures() {
        return service.mesCandidatures();
    }

    @GetMapping("/{id}")
    public CandidatureResponse get(@PathVariable String id) {
        return service.getOwned(id);
    }
}
