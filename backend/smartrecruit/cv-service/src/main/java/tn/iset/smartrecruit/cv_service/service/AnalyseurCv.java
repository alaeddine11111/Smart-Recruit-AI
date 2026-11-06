package tn.iset.smartrecruit.cv_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tn.iset.smartrecruit.cv_service.dto.OcrResponse;
import tn.iset.smartrecruit.cv_service.model.AnalyseCv;
import tn.iset.smartrecruit.cv_service.model.Profil;
import tn.iset.smartrecruit.cv_service.repository.AnalyseRepository;
import tn.iset.smartrecruit.cv_service.repository.ProfilRepository;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;


@Component
public class AnalyseurCv {
    private final RestClient rest;
    private final AnalyseRepository analyses;
    private final ProfilRepository profils;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AnalyseurCv.class);

    public AnalyseurCv(@Value("${app.ocr-url}") String ocrUrl,
                       AnalyseRepository analyses, ProfilRepository profils) {
        var factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofMinutes(5));
        this.rest = RestClient.builder().baseUrl(ocrUrl).requestFactory(factory).build();
        this.analyses = analyses;
        this.profils = profils;
    }

    @Async
    public void lancer(AnalyseCv analyse, Path pdf) {
        try {
            var body = new LinkedMultiValueMap<String, Object>();
            body.add("file", new FileSystemResource(pdf));
            OcrResponse res = rest.post().uri("/analyse")
                    .contentType(MediaType.MULTIPART_FORM_DATA).body(body)
                    .retrieve().body(OcrResponse.class);
            var d = res.donnees();

            // un nouveau CV remplace le brouillon, mais on garde les sections ajoutées à la main
            Profil ancien = profils.findByCandidatId(analyse.candidatId()).orElse(null);
            profils.save(new Profil(
                    ancien == null ? null : ancien.id(), analyse.candidatId(),
                    nz(d.titre()), nz(d.resume()), nz(d.telephone()), nz(d.localisation()),
                    nz(d.experiences()), nz(d.formations()),
                    ancien == null ? List.of() : ancien.competences(), nz(d.competences()),
                    ancien == null ? List.of() : nz(ancien.competencesValidees()), nz(d.langues()),
                    ancien == null ? List.of() : ancien.sections(),
                    false, Instant.now()));

            analyses.save(new AnalyseCv(analyse.id(), analyse.candidatId(), analyse.cvId(),
                    AnalyseCv.Statut.TERMINEE, res.texteOCR(), res.ocrUtilise(), null, Instant.now()));
        } catch (Exception e) {
                log.error("Analyse CV échouée", e);
                String msg = (e instanceof ResourceAccessException)
                        ? "ocr-service injoignable (est-il lancé sur le port 8083 ?)"
                        : "Échec de l'analyse : " + e.getMessage();
                analyses.save(new AnalyseCv(analyse.id(), analyse.candidatId(), analyse.cvId(),
                        AnalyseCv.Statut.ECHEC, null, false, msg, Instant.now()));
                }
    }

    private static String nz(String s) { return s == null ? "" : s; }
    private static <T> List<T> nz(List<T> l) { return l == null ? List.of() : l; }
}