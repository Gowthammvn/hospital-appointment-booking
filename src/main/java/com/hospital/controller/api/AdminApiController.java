package com.hospital.controller.api;

import com.hospital.dto.AppointmentResponse;
import com.hospital.dto.DoctorForm;
import com.hospital.dto.DoctorResponse;
import com.hospital.service.AppointmentService;
import com.hospital.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Hospital administration (role ADMIN)")
@SecurityRequirement(name = "basicAuth")
public class AdminApiController {

    private final AppointmentService appointmentService;
    private final DoctorService doctorService;

    public AdminApiController(AppointmentService appointmentService, DoctorService doctorService) {
        this.appointmentService = appointmentService;
        this.doctorService = doctorService;
    }

    @GetMapping("/appointments")
    @Operation(summary = "All appointments")
    public List<AppointmentResponse> appointments() {
        return appointmentService.findAll().stream().map(AppointmentResponse::from).toList();
    }

    @GetMapping("/stats")
    @Operation(summary = "Appointment counts by status")
    public Map<String, Long> stats() {
        return appointmentService.statistics();
    }

    @GetMapping("/doctors")
    @Operation(summary = "All doctors, including unavailable ones")
    public List<DoctorResponse> doctors() {
        return doctorService.findAll().stream().map(DoctorResponse::from).toList();
    }

    @PostMapping("/doctors")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a doctor (creates their login account)")
    public DoctorResponse addDoctor(@Valid @RequestBody DoctorForm form) {
        return DoctorResponse.from(doctorService.create(form));
    }

    @PatchMapping("/doctors/{id}/availability")
    @Operation(summary = "Toggle whether a doctor accepts new appointments")
    public DoctorResponse toggleAvailability(@PathVariable Long id) {
        return DoctorResponse.from(doctorService.toggleAvailability(id));
    }
}
