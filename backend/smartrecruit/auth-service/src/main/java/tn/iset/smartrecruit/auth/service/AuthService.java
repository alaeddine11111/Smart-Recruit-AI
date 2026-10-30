package tn.iset.smartrecruit.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tn.iset.smartrecruit.auth.dto.*;
import tn.iset.smartrecruit.auth.model.Candidat;
import tn.iset.smartrecruit.auth.model.Entreprise;
import tn.iset.smartrecruit.auth.model.Recruteur;
import tn.iset.smartrecruit.auth.model.Utilisateur;
import tn.iset.smartrecruit.auth.repository.EntrepriseRepository;
import tn.iset.smartrecruit.auth.repository.UtilisateurRepository;
import tn.iset.smartrecruit.auth.security.JwtService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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

    public AuthResponse registerRecruteur(RegisterRecruteurRequest req) {
        String email = normalize(req.email());
        ensureEmailFree(email);

        String nomEntreprise = req.entrepriseNom().trim();
        Entreprise entreprise = entrepriseRepository.findByNomIgnoreCase(nomEntreprise)
                .orElseGet(() -> {
                    Entreprise e = new Entreprise();
                    e.setNom(nomEntreprise);
                    e.setDescription(req.entrepriseDescription());
                    e.setSiteWeb(req.entrepriseSiteWeb());
                    return entrepriseRepository.save(e);
                });

        Recruteur r = new Recruteur();
        r.setNom(req.nom().trim());
        r.setEmail(email);
        r.setMotDePasseHash(passwordEncoder.encode(req.motDePasse()));
        r.setPoste(req.poste());
        r.setEntrepriseId(entreprise.getId());
        return toResponse(utilisateurRepository.save(r));
    }

    public AuthResponse login(LoginRequest req) {
        Utilisateur u = utilisateurRepository.findByEmail(normalize(req.email()))
                .filter(x -> passwordEncoder.matches(req.motDePasse(), x.getMotDePasseHash()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect"));
        return toResponse(u);
    }

    public UserResponse me(String email) {
        return utilisateurRepository.findByEmail(email)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    public List<UserResponse> listUsers() {
        return utilisateurRepository.findAll().stream().map(UserResponse::from).toList();
    }

    private void ensureEmailFree(String email) {
        if (utilisateurRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet email est déjà utilisé");
        }
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private AuthResponse toResponse(Utilisateur u) {
        return new AuthResponse(jwtService.generate(u), "Bearer", jwtService.getExpirationMs(), UserResponse.from(u));
    }
}
