package com.hospital.controller;

import com.hospital.dto.DoctorForm;
import com.hospital.entity.Role;
import com.hospital.exception.BookingException;
import com.hospital.service.AppointmentService;
import com.hospital.service.DepartmentService;
import com.hospital.service.DoctorService;
import com.hospital.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AppointmentService appointmentService;
    private final DoctorService doctorService;
    private final DepartmentService departmentService;
    private final UserService userService;

    public AdminController(AppointmentService appointmentService, DoctorService doctorService,
                           DepartmentService departmentService, UserService userService) {
        this.appointmentService = appointmentService;
        this.doctorService = doctorService;
        this.departmentService = departmentService;
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", appointmentService.statistics());
        model.addAttribute("doctorCount", doctorService.count());
        model.addAttribute("patientCount", userService.countByRole(Role.PATIENT));
        model.addAttribute("departmentCount", departmentService.findAll().size());
        model.addAttribute("appointments", appointmentService.findAll());
        return "admin/dashboard";
    }

    @GetMapping("/doctors")
    public String doctors(Model model) {
        model.addAttribute("form", new DoctorForm());
        return showDoctorsPage(model);
    }

    @PostMapping("/doctors")
    public String addDoctor(@Valid @ModelAttribute("form") DoctorForm form, BindingResult result,
                            Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return showDoctorsPage(model);
        }
        try {
            doctorService.create(form);
        } catch (BookingException e) {
            result.reject("doctor", e.getMessage());
            return showDoctorsPage(model);
        }
        redirect.addFlashAttribute("success", "Doctor " + form.getFullName() + " added.");
        return "redirect:/admin/doctors";
    }

    @PostMapping("/doctors/{id}/toggle")
    public String toggleDoctor(@PathVariable Long id, RedirectAttributes redirect) {
        var doctor = doctorService.toggleAvailability(id);
        redirect.addFlashAttribute("success", doctor.getName()
                + (doctor.isAvailable() ? " is now accepting appointments." : " is now unavailable."));
        return "redirect:/admin/doctors";
    }

    @GetMapping("/departments")
    public String departments(Model model) {
        model.addAttribute("departments", departmentService.findAll());
        return "admin/departments";
    }

    @PostMapping("/departments")
    public String addDepartment(@RequestParam String name, @RequestParam(required = false) String description,
                                RedirectAttributes redirect) {
        try {
            departmentService.create(name, description);
            redirect.addFlashAttribute("success", "Department " + name + " added.");
        } catch (BookingException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/departments";
    }

    @GetMapping("/patients")
    public String patients(Model model) {
        model.addAttribute("patients", userService.findPatients());
        return "admin/patients";
    }

    private String showDoctorsPage(Model model) {
        model.addAttribute("doctors", doctorService.findAll());
        model.addAttribute("departments", departmentService.findAll());
        return "admin/doctors";
    }
}
