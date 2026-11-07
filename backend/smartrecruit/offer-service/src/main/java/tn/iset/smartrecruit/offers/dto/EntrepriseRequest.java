package tn.iset.smartrecruit.offers.dto;
import jakarta.validation.constraints.*;

public record EntrepriseRequest(
        @NotBlank @Size(max = 120) String nom,
        @Size(max = 3000) String description,
        @Size(max = 100) String secteur,
        @Size(max = 200) String siteWeb,
        @Size(max = 100) String ville,
        @Size(max = 250) String adresse,
        @Size(max = 30) String telephone,
        @Email String emailContact,
        @Size(max = 300) String logoUrl) {}
