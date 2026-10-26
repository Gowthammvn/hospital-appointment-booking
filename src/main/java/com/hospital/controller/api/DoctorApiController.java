package com.hospital.controller.api;

import com.hospital.dto.AppointmentResponse;
import com.hospital.dto.StatusUpdateRequest;
import com.hospital.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/doctor/appointments")
@Tag(name = "Doctor", description = "Manage appointments assigned to you (role DOCTOR)")
@SecurityRequirement(name = "basicAuth")
public class DoctorApiController {

    private final AppointmentService appointmentService;

    public DoctorApiController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    @Operation(summary = "Appointments assigned to me")
    public List<AppointmentResponse> myAppointments(Principal principal) {
        return appointmentService.findForDoctor(principal.getName()).stream().map(AppointmentResponse::from).toList();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Confirm, complete or cancel an appointment")
    public AppointmentResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request,
                                            Principal principal) {
        return AppointmentResponse.from(appointmentService.updateStatusByDoctor(
                id, principal.getName(), request.status(), request.notes()));
    }
}
