package tn.iset.smartrecruit.cv_service.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import tn.iset.smartrecruit.cv_service.repository.CvRepository;
import tn.iset.smartrecruit.cv_service.repository.ProfilRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class RecruiterCandidateDataServiceTest {
    @Test
    void reportsOfferServiceNotFoundAsGatewayFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RecruiterCandidateDataService service = new RecruiterCandidateDataService(
                builder, mock(ProfilRepository.class), mock(CvRepository.class), "http://offer-service");
        server.expect(requestTo("http://offer-service/api/recruteur/candidatures/application-1/candidate"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.profileForApplication("application-1", "Bearer recruiter-token"));

        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
        server.verify();
    }
}
