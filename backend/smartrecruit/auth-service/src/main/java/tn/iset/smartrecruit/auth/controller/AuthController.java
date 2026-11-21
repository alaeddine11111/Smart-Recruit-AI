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

    /** Mobile login: exchanges a Keycloak access token for the normal SmartRecruit JWT. */
    @PostMapping("/keycloak")
    public AuthResponse keycloakLogin(@Valid @RequestBody KeycloakLoginRequest req) {
        return authService.loginWithKeycloak(req.accessToken());
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return authService.me(authentication.getName());
    }

    @GetMapping("/profil")
    public ProfileResponse profil(Authentication authentication) {
        return authService.getProfile(authentication.getName());
    }

    @PutMapping("/profil")
    public ProfileResponse updateProfil(Authentication authentication,
                                        @Valid @RequestBody UpdateProfileRequest req) {
        return authService.updateProfile(authentication.getName(), req);
    }

    @PutMapping("/mot-de-passe")
    public MessageResponse changePassword(Authentication authentication,
                                          @Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(authentication.getName(), req);
        return new MessageResponse("Mot de passe modifié.");
    }
}