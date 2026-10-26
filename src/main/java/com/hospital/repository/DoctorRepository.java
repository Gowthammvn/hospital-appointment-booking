package com.hospital.repository;

import com.hospital.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByUserEmailIgnoreCase(String email);

    List<Doctor> findByAvailableTrueOrderByUserFullNameAsc();

    List<Doctor> findByDepartmentIdAndAvailableTrueOrderByUserFullNameAsc(Long departmentId);

    List<Doctor> findAllByOrderByUserFullNameAsc();
}
