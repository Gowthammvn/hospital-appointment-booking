package com.hospital.service;

import com.hospital.dto.RegistrationForm;
import com.hospital.entity.Role;
import com.hospital.entity.User;
import com.hospital.exception.BookingException;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerPatient(RegistrationForm form) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new BookingException("Passwords do not match");
        }
        String email = form.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BookingException("An account with this email already exists");
        }
        User user = new User(form.getFullName().trim(), email,
                passwordEncoder.encode(form.getPassword()), form.getPhone(), Role.PATIENT);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    @Transactional(readOnly = true)
    public List<User> findPatients() {
        return userRepository.findByRoleOrderByFullNameAsc(Role.PATIENT);
    }

    @Transactional(readOnly = true)
    public long countByRole(Role role) {
        return userRepository.countByRole(role);
    }
}
