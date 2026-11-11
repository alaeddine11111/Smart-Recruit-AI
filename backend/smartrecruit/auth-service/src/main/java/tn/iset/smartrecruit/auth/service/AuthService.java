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
        return toResponse(utilisateurRepository.save(c));
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

    public UserResponse me(String email) {
        return utilisateurRepository.findByEmail(email)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
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

        d.setStatut(StatutDemande.ACCEPTEE);
        d.setDateTraitement(LocalDateTime.now());
        return DemandeRecruteurResponse.from(demandeRepository.save(d));
    }

    public DemandeRecruteurResponse refuserDemande(String id, String motif) {
        DemandeRecruteur d = findEnAttente(id);
        d.setStatut(StatutDemande.REFUSEE);
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