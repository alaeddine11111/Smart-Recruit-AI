package tn.iset.smartrecruit.cv_service.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tn.iset.smartrecruit.cv_service.security.JwtService;
import tn.iset.smartrecruit.cv_service.security.SecurityConfig;
import tn.iset.smartrecruit.cv_service.service.SessionPreparationService;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessionPreparationController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:4200")
class SessionPreparationSecurityTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private JwtService jwt;

    @MockBean
    private SessionPreparationService service;

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/preparations"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsAuthenticatedNonCandidate() throws Exception {
        when(jwt.parse("recruiter-token")).thenReturn(claims("recruiter-1", "RECRUTEUR"));

        mvc.perform(get("/api/preparations").header("Authorization", "Bearer recruiter-token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void allowsCandidateAndScopesHistoryToJwtSubject() throws Exception {
        when(jwt.parse("candidate-token")).thenReturn(claims("candidate-1", "CANDIDAT"));
        when(service.history("candidate-1")).thenReturn(List.of());

        mvc.perform(get("/api/preparations").header("Authorization", "Bearer candidate-token"))
                .andExpect(status().isOk());

        verify(service).history("candidate-1");
    }

    private static Claims claims(String subject, String role) {
        return Jwts.claims().subject(subject).add("role", role).build();
    }
}
