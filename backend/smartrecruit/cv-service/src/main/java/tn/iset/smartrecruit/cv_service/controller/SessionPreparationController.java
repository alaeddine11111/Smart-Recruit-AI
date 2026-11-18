package tn.iset.smartrecruit.cv_service.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.iset.smartrecruit.cv_service.dto.StartPreparationRequest;
import tn.iset.smartrecruit.cv_service.dto.SubmitPreparationAnswerRequest;
import tn.iset.smartrecruit.cv_service.model.SessionPreparation;
import tn.iset.smartrecruit.cv_service.service.SessionPreparationService;

import java.util.List;

@RestController
@RequestMapping("/api/preparations")
public class SessionPreparationController {
    private final SessionPreparationService service;

    public SessionPreparationController(SessionPreparationService service) {
        this.service = service;
    }

    @PostMapping
    public SessionPreparation start(Authentication auth,
                                    @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                    @Valid @RequestBody StartPreparationRequest request) {
        return service.start(auth.getName(), authorization, request);
    }

    @GetMapping
    public List<SessionPreparation> history(Authentication auth) {
        return service.history(auth.getName());
    }

    @GetMapping("/{id}")
    public SessionPreparation get(Authentication auth, @PathVariable String id) {
        return service.get(auth.getName(), id);
    }

    @PutMapping("/{id}/reponses")
    public SessionPreparation answer(Authentication auth, @PathVariable String id,
                                     @Valid @RequestBody SubmitPreparationAnswerRequest request) {
        return service.submitAnswer(auth.getName(), id, request);
    }

    @PostMapping("/{id}/terminer")
    public SessionPreparation finish(Authentication auth, @PathVariable String id) {
        return service.finish(auth.getName(), id);
    }
}
