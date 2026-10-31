package tn.iset.smartrecruit.cv_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tn.iset.smartrecruit.cv_service.dto.AnalyseInfo;
import tn.iset.smartrecruit.cv_service.dto.CvInfo;
import tn.iset.smartrecruit.cv_service.service.CvService;

import java.io.IOException;

@RestController
@RequestMapping("/api/cv")
public class CvController {
    private final CvService service;

    public CvController(CvService service) { this.service = service; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CvInfo deposer(Authentication auth, @RequestParam("file") MultipartFile file) throws IOException {
        return service.deposer(auth.getName(), file);
    }

    @GetMapping
    public CvInfo courant(Authentication auth) {
        return service.courant(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun CV"));
    }

    @GetMapping("/analyse")
    public AnalyseInfo analyse(Authentication auth) {
        return service.analyse(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune analyse"));
    }
}