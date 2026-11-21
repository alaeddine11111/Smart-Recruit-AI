package tn.iset.smartrecruit.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tn.iset.smartrecruit.auth.dto.*;
import tn.iset.smartrecruit.auth.model.*;
import tn.iset.smartrecruit.auth.repository.DemandeRecruteurRepository;
import tn.iset.smartrecruit.auth.repository.EntrepriseRepository;
import tn.iset.smartrecruit.auth.repository.UtilisateurRepository;
import tn.iset.smartrecruit.auth.security.JwtService;
import tn.iset.smartrecruit.auth.security.KeycloakTokenVerifier;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final DemandeRecruteurRepository demandeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final KeycloakAdminService keycloakAdminService;
    private final KeycloakTokenVerifier keycloakTokenVerifier;

    // ---------------------------------------------------------------- registration

    public AuthResponse registerCandidat(RegisterCandidatRequest req) {
        String email = normalize(req.email());
        ensureEmailFree(email);

        Candidat c = new Candidat();
        c.setNom(req.nom().trim());
        c.setEmail(email);
        c.setMotDePasseHash(passwordEncoder.encode(req.motDePasse()));
        c.setTelephone(req.telephone());
        c.setLocalisation(req.localisation());
        Candidat saved = utilisateurRepository.save(c);
        keycloakAdminService.createUser(email, saved.getNom(), req.motDePasse(), Role.CANDIDAT.name(), true);
        return toResponse(saved);
    }

    /** A recruiter does not get an account right away: we store a request for the admin. */
    public MessageResponse demanderCompteRecruteur(RegisterRecruteurRequest req) {
        String email = normalize(req.email());
        ensureEmailFree(email);

        DemandeRecruteur d = new DemandeRecruteur();
        d.setNom(req.nom().trim());
        d.setEmail(email);
        d.setMotDePasseHash(passwordEncoder.encode(req.motDePasse()));
        d.setPoste(req.poste());
        d.setEntrepriseNom(req.entrepriseNom().trim());
        d.setEntrepriseDescription(req.entrepriseDescription());
        d.setEntrepriseSiteWeb(req.entrepriseSiteWeb());
        demandeRepository.save(d);
        // Keycloak user created disabled and without role: the admin decision enables it or deletes it
        keycloakAdminService.createUser(email, d.getNom(), req.motDePasse(), null, false);

        return new MessageResponse(
                "Votre demande a bien été envoyée. Un administrateur va l'examiner, "
                        + "vous pourrez vous connecter dès qu'elle sera acceptée.");
    }

    // ---------------------------------------------------------------- login

    public AuthResponse login(LoginRequest req) {
        String email = normalize(req.email());

        var user = utilisateurRepository.findByEmail(email);
        if (user.isPresent()) {
            if (passwordEncoder.matches(req.motDePasse(), user.get().getMotDePasseHash())) {
                keycloakAdminService.syncOnLogin(user.get(), req.motDePasse());
                return toResponse(user.get());
            }
            throw badCredentials();
        }

        // No account: maybe a recruiter whose request is still pending or was refused.
        // We only reveal the status if the password matches the request.
        demandeRepository.findFirstByEmailOrderByDateDemandeDesc(email)
                .filter(d -> passwordEncoder.matches(req.motDePasse(), d.getMotDePasseHash()))
                .ifPresent(d -> {
                    if (d.getStatut() == StatutDemande.EN_ATTENTE) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "Votre demande de compte recruteur est en cours d'examen.");
                    }
                    if (d.getStatut() == StatutDemande.REFUSEE) {
                        String motif = d.getMotifRefus() == null || d.getMotifRefus().isBlank()
                                ? "" : " Motif : " + d.getMotifRefus();
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "Votre demande de compte recruteur a été refusée." + motif);
                    }
                });

        throw badCredentials();
    }

    /**
     * Mobile login through Keycloak: the app sends the access token it got from Keycloak, we verify it
     * and return the normal SmartRecruit JWT, so the other services do not change.
     */
    public AuthResponse loginWithKeycloak(String accessToken) {
        if (!keycloakAdminService.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Keycloak n'est pas activé sur ce serveur");
        }
        Jwt jwt;
        try {
            jwt = keycloakTokenVerifier.verify(accessToken);
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Jeton Keycloak invalide ou expiré");
        }
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Le jeton Keycloak ne contient pas d'email");
        }
        Utilisateur user = utilisateurRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Aucun compte SmartRecruit ne correspond à cet utilisateur Keycloak"));
        return toResponse(user);
    }

    public UserResponse me(String email) {
        return utilisateurRepository.findByEmail(email)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    // ---------------------------------------------------------------- profile

    public ProfileResponse getProfile(String email) {
        return toProfile(findUser(email));
    }

    public ProfileResponse updateProfile(String email, UpdateProfileRequest req) {
        Utilisateur u = findUser(email);
        u.setNom(req.nom().trim());
        if (u instanceof Candidat c) {
            c.setTelephone(req.telephone());
            c.setLocalisation(req.localisation());
        } else if (u instanceof Recruteur r) {
            r.setPoste(req.poste());
        }
        return toProfile(utilisateurRepository.save(u));
    }

    public void changePassword(String email, ChangePasswordRequest req) {
        Utilisateur u = findUser(email);
        if (!passwordEncoder.matches(req.ancienMotDePasse(), u.getMotDePasseHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mot de passe actuel incorrect");
        }
        u.setMotDePasseHash(passwordEncoder.encode(req.nouveauMotDePasse()));
        utilisateurRepository.save(u);
    }

    private Utilisateur findUser(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    private ProfileResponse toProfile(Utilisateur u) {
        String telephone = null, localisation = null, poste = null;
        String entrepriseNom = null, entrepriseDescription = null, entrepriseSiteWeb = null;

        if (u instanceof Candidat c) {
            telephone = c.getTelephone();
            localisation = c.getLocalisation();
        } else if (u instanceof Recruteur r) {
            poste = r.getPoste();
            Entreprise e = r.getEntrepriseId() == null
                    ? null
                    : entrepriseRepository.findById(r.getEntrepriseId()).orElse(null);
            if (e != null) {
                entrepriseNom = e.getNom();
                entrepriseDescription = e.getDescription();
                entrepriseSiteWeb = e.getSiteWeb();
            }
        }
        return new ProfileResponse(u.getId(), u.getNom(), u.getEmail(), u.getRole(), u.getDateCreation(),
                telephone, localisation, poste, entrepriseNom, entrepriseDescription, entrepriseSiteWeb);
    }

    // ---------------------------------------------------------------- admin

    public List<UserResponse> listUsers() {
        return utilisateurRepository.findAll().stream().map(UserResponse::from).toList();
    }

    public List<DemandeRecruteurResponse> listDemandes(StatutDemande statut) {
        List<DemandeRecruteur> list = statut == null
                ? demandeRepository.findAllByOrderByDateDemandeDesc()
                : demandeRepository.findByStatutOrderByDateDemandeDesc(statut);
        return list.stream().map(DemandeRecruteurResponse::from).toList();
    }

    /** Accepting creates the Recruteur account (role RECRUTEUR) and its company. */
    public DemandeRecruteurResponse accepterDemande(String id) {
        DemandeRecruteur d = findEnAttente(id);

        if (utilisateurRepository.existsByEmail(d.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet email est déjà utilisé par un compte");
        }

        Entreprise entreprise = entrepriseRepository.findByNomIgnoreCase(d.getEntrepriseNom())
                .orElseGet(() -> {
                    Entreprise e = new Entreprise();
                    e.setNom(d.getEntrepriseNom());
                    e.setDescription(d.getEntrepriseDescription());
                    e.setSiteWeb(d.getEntrepriseSiteWeb());
                    return entrepriseRepository.save(e);
                });

        Recruteur r = new Recruteur();
        r.setNom(d.getNom());
        r.setEmail(d.getEmail());
        r.setMotDePasseHash(d.getMotDePasseHash());   // already hashed at request time
        r.setPoste(d.getPoste());
        r.setEntrepriseId(entreprise.getId());
        utilisateurRepository.save(r);
        keycloakAdminService.enableAndAssignRole(d.getEmail(), Role.RECRUTEUR.name());

        d.setStatut(StatutDemande.ACCEPTEE);
        d.setDateTraitement(LocalDateTime.now());
        return DemandeRecruteurResponse.from(demandeRepository.save(d));
    }

    public DemandeRecruteurResponse refuserDemande(String id, String motif) {
        DemandeRecruteur d = findEnAttente(id);
        d.setStatut(StatutDemande.REFUSEE);
        keycloakAdminService.deleteUser(d.getEmail());
        d.setMotifRefus(motif == null || motif.isBlank() ? null : motif.trim());
        d.setDateTraitement(LocalDateTime.now());
        return DemandeRecruteurResponse.from(demandeRepository.save(d));
    }

    // ---------------------------------------------------------------- helpers

    private DemandeRecruteur findEnAttente(String id) {
        DemandeRecruteur d = demandeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable"));
        if (d.getStatut() != StatutDemande.EN_ATTENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette demande a déjà été traitée");
        }
        return d;
    }

    private void ensureEmailFree(String email) {
        if (utilisateurRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet email est déjà utilisé");
        }
        if (demandeRepository.existsByEmailAndStatut(email, StatutDemande.EN_ATTENTE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Une demande est déjà en attente pour cet email");
        }
    }

    private ResponseStatusException badCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect");
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private AuthResponse toResponse(Utilisateur u) {
        return new AuthResponse(jwtService.generate(u), "Bearer", jwtService.getExpirationMs(), UserResponse.from(u));
    }
}