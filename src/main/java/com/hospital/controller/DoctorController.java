package com.hospital.controller;

import com.hospital.entity.Appointment;
import com.hospital.entity.AppointmentStatus;
import com.hospital.exception.BookingException;
import com.hospital.service.AppointmentService;
import com.hospital.service.DoctorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/doctor")
public class DoctorController {

    private final AppointmentService appointmentService;
    private final DoctorService doctorService;

    public DoctorController(AppointmentService appointmentService, DoctorService doctorService) {
        this.appointmentService = appointmentService;
        this.doctorService = doctorService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        List<Appointment> appointments = appointmentService.findForDoctor(principal.getName());
        LocalDate today = LocalDate.now();
        model.addAttribute("doctor", doctorService.findByEmail(principal.getName()));
        model.addAttribute("appointments", appointments);
        model.addAttribute("todayCount", appointments.stream()
                .filter(a -> a.getAppointmentDate().equals(today)).count());
        model.addAttribute("pendingCount", appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.PENDING).count());
        model.addAttribute("confirmedCount", appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED).count());
        return "doctor/dashboard";
    }

    @PostMapping("/appointments/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam AppointmentStatus status,
                               @RequestParam(required = false) String notes,
                               Principal principal, RedirectAttributes redirect) {
        try {
            appointmentService.updateStatusByDoctor(id, principal.getName(), status, notes);
            redirect.addFlashAttribute("success", "Appointment #" + id + " marked " + status.name().toLowerCase() + ".");
        } catch (BookingException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/doctor/dashboard";
    }
}
