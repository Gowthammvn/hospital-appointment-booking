package com.hospital.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Request to book an appointment")
public record AppointmentRequest(
        @NotNull @Schema(example = "1") Long doctorId,
        @NotNull @FutureOrPresent @Schema(example = "2026-12-01") LocalDate date,
        @NotNull @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "10:30") LocalTime time,
        @Size(max = 1000) @Schema(example = "Chest pain and shortness of breath") String symptoms
) {
}
