package tn.iset.smartrecruit.offers.controller;

import tn.iset.smartrecruit.offers.external.ExternalOffre;
import tn.iset.smartrecruit.offers.external.ExternalOffreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/offres-externes")
@RequiredArgsConstructor
public class OffreExterneController {

    private final ExternalOffreService service;

    @GetMapping
    public List<ExternalOffre> search(@RequestParam(defaultValue = "") String q,
                                      @RequestParam(defaultValue = "") String location,
                                      @RequestParam(defaultValue = "1") int page) {
        return service.search(q.trim(), location.trim(), page);
    }
}
