package com.hospital.service;

import com.hospital.entity.Department;
import com.hospital.exception.BookingException;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<Department> findAll() {
        return departmentRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Department findById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<Department> findByName(String name) {
        return departmentRepository.findByNameIgnoreCase(name);
    }

    @Transactional
    public Department create(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new BookingException("Department name is required");
        }
        if (departmentRepository.existsByNameIgnoreCase(name.trim())) {
            throw new BookingException("Department already exists: " + name);
        }
        return departmentRepository.save(new Department(name.trim(), description));
    }
}
