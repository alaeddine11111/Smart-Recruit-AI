package tn.iset.smartrecruit.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import tn.iset.smartrecruit.auth.repository.UtilisateurRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class RecruiterCandidateServiceTest {
    @Test
    void reportsOfferServiceNotFoundAsGatewayFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RecruiterCandidateService service = new RecruiterCandidateService(
                builder, mock(UtilisateurRepository.class), "http://offer-service");
        server.expect(requestTo("http://offer-service/api/recruteur/candidatures/application-1/candidate"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.forApplication("application-1", "Bearer recruiter-token"));

        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
        server.verify();
    }
}
