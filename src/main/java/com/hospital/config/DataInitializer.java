package com.hospital.config;

import com.hospital.entity.Department;
import com.hospital.entity.Doctor;
import com.hospital.entity.Role;
import com.hospital.entity.User;
import com.hospital.repository.DepartmentRepository;
import com.hospital.repository.DoctorRepository;
import com.hospital.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Seeds departments, an admin, sample doctors and a sample patient on first start. */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, DepartmentRepository departmentRepository,
                           DoctorRepository doctorRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        log.info("Empty database detected - loading demo data");

        // Names must match the categories in nlp/symptom-training.txt
        Department cardiology = dept("Cardiology", "Heart and blood vessel conditions");
        Department neurology = dept("Neurology", "Brain, spine and nervous system disorders");
        Department orthopedics = dept("Orthopedics", "Bones, joints, muscles and injuries");
        Department dermatology = dept("Dermatology", "Skin, hair and nail conditions");
        Department ent = dept("ENT", "Ear, nose and throat");
        Department gastro = dept("Gastroenterology", "Digestive system and liver");
        Department pulmonology = dept("Pulmonology", "Lungs and breathing");
        Department general = dept("General Medicine", "Fever, infections, check-ups and chronic care");

        userRepository.save(new User("System Admin", "admin@hospital.com",
                passwordEncoder.encode("admin123"), "9000000000", Role.ADMIN));
        userRepository.save(new User("Ravi Kumar", "patient@hospital.com",
                passwordEncoder.encode("patient123"), "9000000001", Role.PATIENT));

        doctor("Dr. Ananya Sharma", "ananya@hospital.com", cardiology, "Interventional Cardiologist", "MBBS, MD, DM (Cardiology)", 12, 800);
        doctor("Dr. Vikram Rao", "vikram@hospital.com", neurology, "Neurologist", "MBBS, MD, DM (Neurology)", 10, 900);
        doctor("Dr. Priya Nair", "priya@hospital.com", orthopedics, "Orthopedic Surgeon", "MBBS, MS (Ortho)", 8, 700);
        doctor("Dr. Arjun Mehta", "arjun@hospital.com", dermatology, "Dermatologist", "MBBS, MD (Dermatology)", 6, 600);
        doctor("Dr. Kavya Reddy", "kavya@hospital.com", ent, "ENT Specialist", "MBBS, MS (ENT)", 9, 600);
        doctor("Dr. Suresh Iyer", "suresh@hospital.com", gastro, "Gastroenterologist", "MBBS, MD, DM (Gastro)", 15, 1000);
        doctor("Dr. Meera Pillai", "meera@hospital.com", pulmonology, "Pulmonologist", "MBBS, MD (Pulmonary Medicine)", 7, 700);
        doctor("Dr. Rahul Verma", "rahul@hospital.com", general, "General Physician", "MBBS, MD (General Medicine)", 5, 400);

        log.info("Demo data loaded: admin@hospital.com/admin123, patient@hospital.com/patient123, "
                + "doctors <name>@hospital.com/doctor123");
    }

    private Department dept(String name, String description) {
        return departmentRepository.save(new Department(name, description));
    }

    private void doctor(String name, String email, Department department, String specialization,
                        String qualification, int experience, int fee) {
        User user = new User(name, email, passwordEncoder.encode("doctor123"), "9100000000", Role.DOCTOR);
        doctorRepository.save(new Doctor(user, department, specialization, qualification, experience,
                BigDecimal.valueOf(fee)));
    }
}
