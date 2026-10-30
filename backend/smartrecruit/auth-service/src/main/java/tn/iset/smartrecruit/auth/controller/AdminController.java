package tn.iset.smartrecruit.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.iset.smartrecruit.auth.dto.UserResponse;
import tn.iset.smartrecruit.auth.service.AuthService;

import java.util.List;

/** Admin-only (see SecurityConfig). Starting point for gererUtilisateurs(). */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;

    @GetMapping("/utilisateurs")
    public List<UserResponse> utilisateurs() {
        return authService.listUsers();
    }
}
