package com.hospital.controller;

import com.hospital.dto.BookingForm;
import com.hospital.dto.SymptomAnalysis;
import com.hospital.exception.BookingException;
import com.hospital.service.AppointmentService;
import com.hospital.service.DepartmentService;
import com.hospital.service.DoctorService;
import com.hospital.service.SymptomAnalyzerService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/patient")
public class PatientController {

    private final AppointmentService appointmentService;
    private final DoctorService doctorService;
    private final DepartmentService departmentService;
    private final SymptomAnalyzerService symptomAnalyzer;

    public PatientController(AppointmentService appointmentService, DoctorService doctorService,
                             DepartmentService departmentService, SymptomAnalyzerService symptomAnalyzer) {
        this.appointmentService = appointmentService;
        this.doctorService = doctorService;
        this.departmentService = departmentService;
        this.symptomAnalyzer = symptomAnalyzer;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        model.addAttribute("appointments", appointmentService.findForPatient(principal.getName()));
        return "patient/dashboard";
    }

    @GetMapping("/book")
    public String bookForm(@RequestParam(required = false) Long doctorId, Model model) {
        BookingForm form = new BookingForm();
        form.setDoctorId(doctorId);
        model.addAttribute("form", form);
        return showBookingPage(model, null);
    }

    /** Step 1: run the NLP analyzer on the symptoms and pre-select the suggested department. */
    @PostMapping("/book/analyze")
    public String analyze(@RequestParam String symptoms, Model model) {
        SymptomAnalysis analysis = symptomAnalyzer.analyze(symptoms);
        BookingForm form = new BookingForm();
        form.setSymptoms(symptoms);
        form.setSuggestedDepartment(analysis.department());
        model.addAttribute("form", form);
        return showBookingPage(model, analysis);
    }

    /** Step 2: book the selected doctor and slot. */
    @PostMapping("/book")
    public String book(@Valid @ModelAttribute("form") BookingForm form, BindingResult result,
                       Principal principal, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return showBookingPage(model, null);
        }
        try {
            appointmentService.book(principal.getName(), form.getDoctorId(), form.getDate(), form.getTime(),
                    form.getSymptoms(), form.getSuggestedDepartment());
        } catch (BookingException e) {
            result.reject("booking", e.getMessage());
            return showBookingPage(model, null);
        }
        redirect.addFlashAttribute("success", "Appointment booked! The doctor will confirm it shortly.");
        return "redirect:/patient/dashboard";
    }

    @PostMapping("/appointments/{id}/cancel")
    public String cancel(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            appointmentService.cancelByPatient(id, principal.getName());
            redirect.addFlashAttribute("success", "Appointment cancelled.");
        } catch (BookingException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/patient/dashboard";
    }

    private String showBookingPage(Model model, SymptomAnalysis analysis) {
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("doctors", doctorService.findBookable(null));
        model.addAttribute("analysis", analysis);
        if (analysis != null) {
            departmentService.findByName(analysis.department())
                    .ifPresent(d -> model.addAttribute("suggestedDepartmentId", d.getId()));
        }
        return "patient/book";
    }
}
