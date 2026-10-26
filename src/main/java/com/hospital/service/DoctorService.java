package com.hospital.service;

import com.hospital.dto.DoctorForm;
import com.hospital.entity.Department;
import com.hospital.entity.Doctor;
import com.hospital.entity.Role;
import com.hospital.entity.User;
import com.hospital.exception.BookingException;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.repository.DoctorRepository;
import com.hospital.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final DepartmentService departmentService;
    private final PasswordEncoder passwordEncoder;

    public DoctorService(DoctorRepository doctorRepository, UserRepository userRepository,
                         DepartmentService departmentService, PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.departmentService = departmentService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Doctor> findAll() {
        return doctorRepository.findAllByOrderByUserFullNameAsc();
    }

    /** Doctors that patients can currently book, optionally filtered by department. */
    @Transactional(readOnly = true)
    public List<Doctor> findBookable(Long departmentId) {
        return departmentId == null
                ? doctorRepository.findByAvailableTrueOrderByUserFullNameAsc()
                : doctorRepository.findByDepartmentIdAndAvailableTrueOrderByUserFullNameAsc(departmentId);
    }

    @Transactional(readOnly = true)
    public Doctor findById(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + id));
    }

    @Transactional(readOnly = true)
    public Doctor findByEmail(String email) {
        return doctorRepository.findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No doctor profile for: " + email));
    }

    @Transactional
    public Doctor create(DoctorForm form) {
        String email = form.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BookingException("An account with this email already exists");
        }
        Department department = departmentService.findById(form.getDepartmentId());
        User user = new User(form.getFullName().trim(), email,
                passwordEncoder.encode(form.getPassword()), form.getPhone(), Role.DOCTOR);
        Doctor doctor = new Doctor(user, department, form.getSpecialization(), form.getQualification(),
                form.getExperienceYears(), form.getConsultationFee());
        return doctorRepository.save(doctor);
    }

    @Transactional
    public Doctor toggleAvailability(Long id) {
        Doctor doctor = findById(id);
        doctor.setAvailable(!doctor.isAvailable());
        return doctor;
    }

    @Transactional(readOnly = true)
    public long count() {
        return doctorRepository.count();
    }
}
