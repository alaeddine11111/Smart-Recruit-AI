package tn.iset.smartrecruit.offers.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;

    public JwtAuthFilter(JwtService jwt) { this.jwt = jwt; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims c = jwt.parse(header.substring(7));
                List<GrantedAuthority> roles = new ArrayList<>();
                Object role = c.get("role");
                if (role != null) roles.add(new SimpleGrantedAuthority("ROLE_" + role));
                // le subject du token devient auth.getName() dans les contrôleurs
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(c.getSubject(), null, roles));
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext(); // token invalide : la requête sera refusée (401)
            }
        }
        chain.doFilter(req, res);
    }
}