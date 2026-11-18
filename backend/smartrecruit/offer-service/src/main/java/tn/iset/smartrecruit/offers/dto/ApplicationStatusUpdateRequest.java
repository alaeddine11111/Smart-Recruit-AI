package tn.iset.smartrecruit.offers.dto;

import jakarta.validation.constraints.NotNull;
import tn.iset.smartrecruit.offers.model.ApplicationStatus;

public record ApplicationStatusUpdateRequest(@NotNull ApplicationStatus statut) {
}
