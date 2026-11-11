package tn.iset.smartrecruit.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.iset.smartrecruit.auth.dto.*;
import tn.iset.smartrecruit.auth.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/candidat")
    public ResponseEntity<AuthResponse> registerCandidat(@Valid @RequestBody RegisterCandidatRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerCandidat(req));
    }

    /** Creates a request for the admin, not an account. */
    @PostMapping("/register/recruteur")
    public ResponseEntity<MessageResponse> registerRecruteur(@Valid @RequestBody RegisterRecruteurRequest req) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(authService.demanderCompteRecruteur(req));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return authService.me(authentication.getName());
    }
}