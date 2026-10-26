package com.hospital.controller;

import com.hospital.dto.RegistrationForm;
import com.hospital.exception.BookingException;
import com.hospital.security.RoleBasedSuccessHandler;
import com.hospital.service.DepartmentService;
import com.hospital.service.DoctorService;
import com.hospital.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    private final DepartmentService departmentService;
    private final DoctorService doctorService;
    private final UserService userService;

    public HomeController(DepartmentService departmentService, DoctorService doctorService, UserService userService) {
        this.departmentService = departmentService;
        this.doctorService = doctorService;
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("doctors", doctorService.findBookable(null));
        return "index";
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (isLoggedIn(authentication)) {
            return "redirect:" + RoleBasedSuccessHandler.dashboardFor(authentication);
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form, BindingResult result,
                           RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "register";
        }
        try {
            userService.registerPatient(form);
        } catch (BookingException e) {
            result.reject("register", e.getMessage());
            return "register";
        }
        redirect.addFlashAttribute("success", "Registration successful. Please log in.");
        return "redirect:/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        return "redirect:" + RoleBasedSuccessHandler.dashboardFor(authentication);
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }

    private static boolean isLoggedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
