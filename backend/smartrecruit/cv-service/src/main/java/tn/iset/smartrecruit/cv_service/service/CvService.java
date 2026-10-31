package tn.iset.smartrecruit.cv_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tn.iset.smartrecruit.cv_service.dto.AnalyseInfo;
import tn.iset.smartrecruit.cv_service.dto.CvInfo;
import tn.iset.smartrecruit.cv_service.model.AnalyseCv;
import tn.iset.smartrecruit.cv_service.model.CvDocument;
import tn.iset.smartrecruit.cv_service.repository.AnalyseRepository;
import tn.iset.smartrecruit.cv_service.repository.CvRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Optional;

@Service
public class CvService {
    private static final long MAX = 5L * 1024 * 1024;
    private final CvRepository cvs;
    private final AnalyseRepository analyses;
    private final AnalyseurCv analyseur;
    private final Path uploads;

    public CvService(CvRepository cvs, AnalyseRepository analyses, AnalyseurCv analyseur,
                     @Value("${app.uploads-dir}") String dir) {
        this.cvs = cvs;
        this.analyses = analyses;
        this.analyseur = analyseur;
        this.uploads = Path.of(dir);
    }

    public CvInfo deposer(String candidatId, MultipartFile f) throws IOException {
        if (f.isEmpty() || f.getSize() > MAX)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier est vide ou dépasse 5 Mo.");
        byte[] head = f.getInputStream().readNBytes(4);
        if (!new String(head, StandardCharsets.ISO_8859_1).equals("%PDF"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le CV doit être un fichier PDF.");

        // le nom de dossier vient du token : on retire tout caractère dangereux (ex: @ . /)
        Path dir = uploads.resolve(candidatId.replaceAll("[^a-zA-Z0-9_-]", "_"));
        Files.createDirectories(dir);
        Path target = dir.resolve("cv.pdf"); // un seul CV par candidat
        Files.copy(f.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String id = cvs.findByCandidatId(candidatId).map(CvDocument::id).orElse(null);
        CvDocument cv = cvs.save(new CvDocument(id, candidatId, f.getOriginalFilename(),
                f.getSize(), target.toString(), Instant.now()));

        AnalyseCv analyse = analyses.save(new AnalyseCv(null, candidatId, cv.id(),
                AnalyseCv.Statut.EN_COURS, null, false, null, Instant.now()));
        analyseur.lancer(analyse, target);
        return new CvInfo(cv.nomFichier(), cv.taille(), cv.dateDepot());
    }

    public Optional<CvInfo> courant(String candidatId) {
        return cvs.findByCandidatId(candidatId)
                .map(c -> new CvInfo(c.nomFichier(), c.taille(), c.dateDepot()));
    }

    public Optional<AnalyseInfo> analyse(String candidatId) {
    return analyses.findFirstByCandidatIdOrderByDateDesc(candidatId)
            .map(a -> new AnalyseInfo(a.statut(), a.messageErreur(), a.texteOCR(), a.ocrUtilise()));
}
}