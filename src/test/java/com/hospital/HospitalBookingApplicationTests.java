package com.hospital;

import com.hospital.repository.DepartmentRepository;
import com.hospital.repository.DoctorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Starts the full application against an in-memory H2 database (see src/test/resources). */
@SpringBootTest
class HospitalBookingApplicationTests {

    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private DoctorRepository doctorRepository;

    @Test
    void contextLoadsAndDemoDataIsSeeded() {
        assertThat(departmentRepository.count()).isEqualTo(8);
        assertThat(doctorRepository.count()).isEqualTo(8);
    }
}
