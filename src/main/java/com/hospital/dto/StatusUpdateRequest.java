package com.hospital.dto;

import com.hospital.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StatusUpdateRequest(
        @NotNull AppointmentStatus status,
        @Size(max = 1000) String notes
) {
}
