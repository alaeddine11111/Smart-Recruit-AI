package tn.iset.smartrecruit.auth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tn.iset.smartrecruit.auth.model.Administrateur;
import tn.iset.smartrecruit.auth.repository.UtilisateurRepository;

/** Creates the first admin on startup (admins are never created through public registration). */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        String email = adminEmail.trim().toLowerCase();
        if (utilisateurRepository.existsByEmail(email)) {
            return;
        }
        Administrateur admin = new Administrateur();
        admin.setNom("Administrateur");
        admin.setEmail(email);
        admin.setMotDePasseHash(passwordEncoder.encode(adminPassword));
        utilisateurRepository.save(admin);
        log.info("Admin account created: {}", email);
    }
}
