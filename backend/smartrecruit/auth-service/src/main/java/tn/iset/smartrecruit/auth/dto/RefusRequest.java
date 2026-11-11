package tn.iset.smartrecruit.auth.dto;

import jakarta.validation.constraints.Size;

public record RefusRequest(@Size(max = 500) String motif) {}