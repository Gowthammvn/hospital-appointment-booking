package com.hospital.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hospital.entity.Appointment;
import com.hospital.entity.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentResponse(
        Long id,
        Long patientId,
        String patientName,
        Long doctorId,
        String doctorName,
        String department,
        LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime time,
        String symptoms,
        String suggestedDepartment,
        AppointmentStatus status,
        String doctorNotes
) {
    public static AppointmentResponse from(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getPatient().getId(),
                a.getPatient().getFullName(),
                a.getDoctor().getId(),
                a.getDoctor().getName(),
                a.getDoctor().getDepartment().getName(),
                a.getAppointmentDate(),
                a.getAppointmentTime(),
                a.getSymptoms(),
                a.getSuggestedDepartment(),
                a.getStatus(),
                a.getDoctorNotes());
    }
}
