package com.hospital.controller.api;

import com.hospital.dto.DoctorResponse;
import com.hospital.dto.SymptomAnalysis;
import com.hospital.entity.Department;
import com.hospital.service.AppointmentService;
import com.hospital.service.DepartmentService;
import com.hospital.service.DoctorService;
import com.hospital.service.SymptomAnalyzerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Public", description = "Departments, doctors, available slots and symptom analysis (no login needed)")
public class PublicApiController {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final DepartmentService departmentService;
    private final DoctorService doctorService;
    private final AppointmentService appointmentService;
    private final SymptomAnalyzerService symptomAnalyzer;

    public PublicApiController(DepartmentService departmentService, DoctorService doctorService,
                               AppointmentService appointmentService, SymptomAnalyzerService symptomAnalyzer) {
        this.departmentService = departmentService;
        this.doctorService = doctorService;
        this.appointmentService = appointmentService;
        this.symptomAnalyzer = symptomAnalyzer;
    }

    @GetMapping("/departments")
    @Operation(summary = "List all departments")
    public List<Department> departments() {
        return departmentService.findAll();
    }

    @GetMapping("/doctors")
    @Operation(summary = "List bookable doctors, optionally filtered by department")
    public List<DoctorResponse> doctors(@RequestParam(required = false) Long departmentId) {
        return doctorService.findBookable(departmentId).stream().map(DoctorResponse::from).toList();
    }

    @GetMapping("/doctors/{id}")
    @Operation(summary = "Get a doctor's profile")
    public DoctorResponse doctor(@PathVariable Long id) {
        return DoctorResponse.from(doctorService.findById(id));
    }

    @GetMapping("/doctors/{id}/slots")
    @Operation(summary = "Free time slots (HH:mm) for a doctor on a date")
    public List<String> slots(@PathVariable Long id,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return appointmentService.getAvailableSlots(id, date).stream().map(t -> t.format(HH_MM)).toList();
    }

    @GetMapping("/slots")
    @Operation(summary = "All daily consultation slots")
    public List<String> dailySlots() {
        return AppointmentService.DAILY_SLOTS.stream().map((LocalTime t) -> t.format(HH_MM)).toList();
    }

    @PostMapping("/symptoms/analyze")
    @Operation(summary = "Suggest a department from free-text symptoms (Apache OpenNLP)")
    public SymptomAnalysis analyze(@Valid @RequestBody SymptomRequest request) {
        return symptomAnalyzer.analyze(request.symptoms());
    }

    public record SymptomRequest(@NotBlank String symptoms) {
    }
}
