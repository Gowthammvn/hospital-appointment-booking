package com.hospital.dto;

import com.hospital.entity.Doctor;

import java.math.BigDecimal;

public record DoctorResponse(
        Long id,
        String name,
        Long departmentId,
        String department,
        String specialization,
        String qualification,
        int experienceYears,
        BigDecimal consultationFee
) {
    public static DoctorResponse from(Doctor d) {
        return new DoctorResponse(
                d.getId(),
                d.getName(),
                d.getDepartment().getId(),
                d.getDepartment().getName(),
                d.getSpecialization(),
                d.getQualification(),
                d.getExperienceYears(),
                d.getConsultationFee());
    }
}
