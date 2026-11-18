package tn.iset.smartrecruit.offers.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import tn.iset.smartrecruit.offers.exception.ConflictException;
import tn.iset.smartrecruit.offers.exception.ResourceNotFoundException;
import tn.iset.smartrecruit.offers.model.ApplicationStatus;
import tn.iset.smartrecruit.offers.model.Candidature;
import tn.iset.smartrecruit.offers.model.OffreEmploi;
import tn.iset.smartrecruit.offers.model.OfferStatus;
import tn.iset.smartrecruit.offers.repository.CandidatureRepository;
import tn.iset.smartrecruit.offers.repository.OffreEmploiRepository;
import tn.iset.smartrecruit.offers.security.AuthFacade;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidatureServiceTest {
    @Mock CandidatureRepository candidatureRepository;
    @Mock OffreEmploiRepository offreRepository;
    @Mock AuthFacade auth;

    CandidatureService service;

    @BeforeEach
    void setUp() {
        service = new CandidatureService(candidatureRepository, offreRepository, auth);
        when(auth.currentUserId()).thenReturn("candidate@example.com");
    }

    @Test
    void appliesToPublishedOfferWithPendingStatus() {
        OffreEmploi offre = OffreEmploi.builder().id("offer-1").statut(OfferStatus.PUBLISHED).titre("Java").build();
        when(offreRepository.findByIdAndStatut("offer-1", OfferStatus.PUBLISHED)).thenReturn(Optional.of(offre));
        when(candidatureRepository.findByCandidatIdAndOffreId("candidate@example.com", "offer-1")).thenReturn(Optional.empty());
        when(candidatureRepository.save(any(Candidature.class))).thenAnswer(invocation -> {
            Candidature c = invocation.getArgument(0);
            c.setId("application-1");
            return c;
        });

        var result = service.apply("offer-1");

        assertEquals(ApplicationStatus.PENDING, result.statut());
        assertEquals("offer-1", result.offreId());
    }

    @Test
    void rejectsDuplicateApplication() {
        OffreEmploi offre = OffreEmploi.builder().id("offer-1").statut(OfferStatus.PUBLISHED).build();
        Candidature existing = Candidature.builder().id("existing").build();
        when(offreRepository.findByIdAndStatut("offer-1", OfferStatus.PUBLISHED)).thenReturn(Optional.of(offre));
        when(candidatureRepository.findByCandidatIdAndOffreId("candidate@example.com", "offer-1"))
                .thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class, () -> service.apply("offer-1"));
        verify(candidatureRepository, never()).save(any());
    }

    @Test
    void rejectsUnpublishedOrMissingOffer() {
        when(offreRepository.findByIdAndStatut("draft-1", OfferStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.apply("draft-1"));
    }

    @Test
    void returnsOnlyAuthenticatedCandidateApplications() {
        Candidature mine = Candidature.builder()
                .id("mine").candidatId("candidate@example.com").offreId("offer-1")
                .statut(ApplicationStatus.PENDING).build();
        when(candidatureRepository.findByCandidatIdOrderByDateCandidatureDesc("candidate@example.com"))
                .thenReturn(List.of(mine));
        when(offreRepository.findById("offer-1")).thenReturn(Optional.of(
                OffreEmploi.builder().id("offer-1").titre("Java").build()));

        var result = service.mesCandidatures();

        assertEquals(1, result.size());
        assertEquals("mine", result.get(0).id());
        verify(candidatureRepository).findByCandidatIdOrderByDateCandidatureDesc("candidate@example.com");
    }

    @Test
    void preventsViewingAnotherCandidateApplication() {
        Candidature other = Candidature.builder()
                .id("other").candidatId("other@example.com").offreId("offer-1").build();
        when(candidatureRepository.findById("other")).thenReturn(Optional.of(other));

        assertThrows(AccessDeniedException.class, () -> service.getOwned("other"));
    }

    @Test
    void recruiterListsOnlyApplicationsForOwnedOffers() {
        when(auth.currentUserId()).thenReturn("recruiter@example.com");
        when(offreRepository.findByRecruteurId("recruiter@example.com")).thenReturn(List.of(
                OffreEmploi.builder().id("owned-offer").titre("Owned role").recruteurId("recruiter@example.com").build()));
        Candidature owned = Candidature.builder().id("owned-app").offreId("owned-offer").build();
        Candidature foreign = Candidature.builder().id("foreign-app").offreId("foreign-offer").build();
        when(candidatureRepository.findByOffreIdIn(List.of("owned-offer"))).thenReturn(List.of(owned, foreign));

        var result = service.recruiterApplications(null, null, null);

        assertEquals(1, result.size());
        assertEquals("owned-app", result.get(0).id());
        assertEquals("Owned role", result.get(0).offre().getTitre());
    }

    @Test
    void recruiterStatsCountApplicationsByOwnedOfferAndStatus() {
        when(auth.currentUserId()).thenReturn("recruiter@example.com");
        OffreEmploi offer = OffreEmploi.builder()
                .id("owned-offer").titre("Owned role").recruteurId("recruiter@example.com").build();
        when(offreRepository.findByRecruteurId("recruiter@example.com")).thenReturn(List.of(offer));
        when(candidatureRepository.findByOffreIdIn(List.of("owned-offer"))).thenReturn(List.of(
                Candidature.builder().id("one").offreId("owned-offer").statut(ApplicationStatus.PENDING).build(),
                Candidature.builder().id("two").offreId("owned-offer").statut(ApplicationStatus.ACCEPTED).build()));

        var result = service.recruiterStats();

        assertEquals(2, result.total());
        assertEquals(1, result.pending());
        assertEquals(1, result.accepted());
        assertEquals(0, result.rejected());
        assertEquals(1, result.offers().size());
        assertEquals(2, result.offers().get(0).total());
    }

    @Test
    void recruiterCanAcceptApplicationForOwnedOffer() {
        when(auth.currentUserId()).thenReturn("recruiter@example.com");
        Candidature application = Candidature.builder().id("app").offreId("owned-offer")
                .statut(ApplicationStatus.PENDING).build();
        OffreEmploi offer = OffreEmploi.builder().id("owned-offer").recruteurId("recruiter@example.com").build();
        when(candidatureRepository.findById("app")).thenReturn(Optional.of(application));
        when(offreRepository.findById("owned-offer")).thenReturn(Optional.of(offer));
        when(candidatureRepository.save(application)).thenReturn(application);

        var result = service.updateRecruiterStatus("app", ApplicationStatus.ACCEPTED);

        assertEquals(ApplicationStatus.ACCEPTED, result.statut());
        verify(candidatureRepository).save(application);
    }

    @Test
    void recruiterCanRejectApplicationForOwnedOffer() {
        when(auth.currentUserId()).thenReturn("recruiter@example.com");
        Candidature application = Candidature.builder().id("app").offreId("owned-offer")
                .statut(ApplicationStatus.PENDING).build();
        OffreEmploi offer = OffreEmploi.builder().id("owned-offer").recruteurId("recruiter@example.com").build();
        when(candidatureRepository.findById("app")).thenReturn(Optional.of(application));
        when(offreRepository.findById("owned-offer")).thenReturn(Optional.of(offer));
        when(candidatureRepository.save(application)).thenReturn(application);

        var result = service.updateRecruiterStatus("app", ApplicationStatus.REJECTED);

        assertEquals(ApplicationStatus.REJECTED, result.statut());
        verify(candidatureRepository).save(application);
    }

    @Test
    void recruiterCannotUpdateApplicationForAnotherRecruitersOffer() {
        when(auth.currentUserId()).thenReturn("recruiter@example.com");
        Candidature application = Candidature.builder().id("app").offreId("foreign-offer")
                .statut(ApplicationStatus.PENDING).build();
        OffreEmploi offer = OffreEmploi.builder().id("foreign-offer").recruteurId("someone-else@example.com").build();
        when(candidatureRepository.findById("app")).thenReturn(Optional.of(application));
        when(offreRepository.findById("foreign-offer")).thenReturn(Optional.of(offer));

        assertThrows(AccessDeniedException.class,
                () -> service.updateRecruiterStatus("app", ApplicationStatus.ACCEPTED));
        verify(candidatureRepository, never()).save(any());
    }
}
