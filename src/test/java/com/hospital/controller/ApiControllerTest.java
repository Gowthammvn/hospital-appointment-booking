package com.hospital.controller;

import com.hospital.config.SecurityConfig;
import com.hospital.controller.api.PatientApiController;
import com.hospital.controller.api.PublicApiController;
import com.hospital.dto.SymptomAnalysis;
import com.hospital.entity.*;
import com.hospital.exception.BookingException;
import com.hospital.security.RoleBasedSuccessHandler;
import com.hospital.service.AppointmentService;
import com.hospital.service.DepartmentService;
import com.hospital.service.DoctorService;
import com.hospital.service.SymptomAnalyzerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({PublicApiController.class, PatientApiController.class})
@Import({SecurityConfig.class, RoleBasedSuccessHandler.class})
class ApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DepartmentService departmentService;
    @MockitoBean
    private DoctorService doctorService;
    @MockitoBean
    private AppointmentService appointmentService;
    @MockitoBean
    private SymptomAnalyzerService symptomAnalyzer;

    @Test
    void slotsArePublicAndFormattedAsHoursMinutes() throws Exception {
        LocalDate date = LocalDate.of(2026, 12, 1);
        when(appointmentService.getAvailableSlots(1L, date))
                .thenReturn(List.of(LocalTime.of(9, 0), LocalTime.of(14, 30)));

        mvc.perform(get("/api/doctors/1/slots").param("date", "2026-12-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("09:00"))
                .andExpect(jsonPath("$[1]").value("14:30"));
    }

    @Test
    void analyzeSymptomsReturnsSuggestedDepartment() throws Exception {
        when(symptomAnalyzer.analyze("chest pain"))
                .thenReturn(new SymptomAnalysis("Cardiology", 0.71, Map.of("Cardiology", 0.71)));

        mvc.perform(post("/api/symptoms/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symptoms\":\"chest pain\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department").value("Cardiology"))
                .andExpect(jsonPath("$.confidence").value(0.71));
    }

    @Test
    void patientEndpointsRequireLogin() throws Exception {
        mvc.perform(get("/api/patient/appointments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "ananya@hospital.com", roles = "DOCTOR")
    void patientEndpointsRejectOtherRoles() throws Exception {
        mvc.perform(get("/api/patient/appointments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "patient@hospital.com", roles = "PATIENT")
    void patientBooksAppointment() throws Exception {
        when(symptomAnalyzer.analyze("chest pain"))
                .thenReturn(new SymptomAnalysis("Cardiology", 0.71, Map.of()));
        when(appointmentService.book(eq("patient@hospital.com"), eq(1L), eq(LocalDate.of(2026, 12, 1)),
                eq(LocalTime.of(10, 30)), eq("chest pain"), eq("Cardiology")))
                .thenReturn(sampleAppointment());

        mvc.perform(post("/api/patient/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"doctorId":1,"date":"2026-12-01","time":"10:30","symptoms":"chest pain"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.time").value("10:30"))
                .andExpect(jsonPath("$.suggestedDepartment").value("Cardiology"));
    }

    @Test
    @WithMockUser(username = "patient@hospital.com", roles = "PATIENT")
    void bookingConflictReturns409() throws Exception {
        when(appointmentService.book(any(), any(), any(), any(), any(), any()))
                .thenThrow(new BookingException("This slot is already booked"));

        mvc.perform(post("/api/patient/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"doctorId":1,"date":"2026-12-01","time":"10:30"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("This slot is already booked"));
    }

    @Test
    @WithMockUser(username = "patient@hospital.com", roles = "PATIENT")
    void invalidBookingRequestReturns400() throws Exception {
        mvc.perform(post("/api/patient/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-12-01\",\"time\":\"10:30\"}"))
                .andExpect(status().isBadRequest());
    }

    private static Appointment sampleAppointment() {
        User patient = new User("Ravi", "patient@hospital.com", "x", "1", Role.PATIENT);
        patient.setId(10L);
        Department dept = new Department("Cardiology", "");
        dept.setId(1L);
        Doctor doctor = new Doctor(new User("Dr. Ananya", "ananya@hospital.com", "x", "2", Role.DOCTOR),
                dept, "Cardiologist", "MD", 10, BigDecimal.TEN);
        doctor.setId(1L);

        Appointment a = new Appointment();
        a.setId(100L);
        a.setPatient(patient);
        a.setDoctor(doctor);
        a.setAppointmentDate(LocalDate.of(2026, 12, 1));
        a.setAppointmentTime(LocalTime.of(10, 30));
        a.setSymptoms("chest pain");
        a.setSuggestedDepartment("Cardiology");
        a.setStatus(AppointmentStatus.PENDING);
        return a;
    }
}
