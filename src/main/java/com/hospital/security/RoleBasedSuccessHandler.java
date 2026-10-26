package com.hospital.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Sends each user to their role's dashboard after login. */
@Component
public class RoleBasedSuccessHandler implements AuthenticationSuccessHandler {

    public static String dashboardFor(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            switch (authority.getAuthority()) {
                case "ROLE_ADMIN" -> { return "/admin/dashboard"; }
                case "ROLE_DOCTOR" -> { return "/doctor/dashboard"; }
                case "ROLE_PATIENT" -> { return "/patient/dashboard"; }
                default -> { }
            }
        }
        return "/";
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        response.sendRedirect(request.getContextPath() + dashboardFor(authentication));
    }
}
