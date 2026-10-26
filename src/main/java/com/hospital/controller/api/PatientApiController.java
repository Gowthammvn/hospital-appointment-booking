package com.hospital.controller.api;

import com.hospital.dto.AppointmentRequest;
import com.hospital.dto.AppointmentResponse;
import com.hospital.service.AppointmentService;
import com.hospital.service.SymptomAnalyzerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/patient/appointments")
@Tag(name = "Patient", description = "Book, view and cancel your appointments (role PATIENT)")
@SecurityRequirement(name = "basicAuth")
public class PatientApiController {

    private final AppointmentService appointmentService;
    private final SymptomAnalyzerService symptomAnalyzer;

    public PatientApiController(AppointmentService appointmentService, SymptomAnalyzerService symptomAnalyzer) {
        this.appointmentService = appointmentService;
        this.symptomAnalyzer = symptomAnalyzer;
    }

    @GetMapping
    @Operation(summary = "My appointments")
    public List<AppointmentResponse> myAppointments(Principal principal) {
        return appointmentService.findForPatient(principal.getName()).stream().map(AppointmentResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Book an appointment (symptoms are analyzed to record a suggested department)")
    public AppointmentResponse book(@Valid @RequestBody AppointmentRequest request, Principal principal) {
        String suggested = request.symptoms() == null || request.symptoms().isBlank()
                ? null : symptomAnalyzer.analyze(request.symptoms()).department();
        return AppointmentResponse.from(appointmentService.book(principal.getName(), request.doctorId(),
                request.date(), request.time(), request.symptoms(), suggested));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel one of my appointments")
    public AppointmentResponse cancel(@PathVariable Long id, Principal principal) {
        return AppointmentResponse.from(appointmentService.cancelByPatient(id, principal.getName()));
    }
}
