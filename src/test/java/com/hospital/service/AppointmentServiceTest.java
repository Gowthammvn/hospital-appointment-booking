package com.hospital.service;

import com.hospital.entity.*;
import com.hospital.exception.BookingException;
import com.hospital.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    // "Now" is 2026-11-02 (Monday) 10:15
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 2);
    private static final Clock CLOCK = Clock.fixed(
            TODAY.atTime(10, 15).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    private static final LocalDate TOMORROW = TODAY.plusDays(1);

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private DoctorService doctorService;
    @Mock
    private UserService userService;

    private AppointmentService service;
    private User patient;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointmentRepository, doctorService, userService, CLOCK);

        patient = new User("Ravi", "patient@hospital.com", "x", "999", Role.PATIENT);
        patient.setId(10L);

        User doctorUser = new User("Dr. Ananya", "ananya@hospital.com", "x", "888", Role.DOCTOR);
        doctor = new Doctor(doctorUser, new Department("Cardiology", ""), "Cardiologist", "MD", 10, BigDecimal.TEN);
        doctor.setId(1L);
    }

    @Test
    void dailySlotsSkipLunchHour() {
        assertThat(AppointmentService.DAILY_SLOTS)
                .hasSize(14)
                .startsWith(LocalTime.of(9, 0))
                .endsWith(LocalTime.of(16, 30))
                .doesNotContain(LocalTime.of(13, 0), LocalTime.of(13, 30));
    }

    @Nested
    class AvailableSlots {

        @Test
        void excludesBookedSlots() {
            when(doctorService.findById(1L)).thenReturn(doctor);
            Appointment booked = appointment(AppointmentStatus.CONFIRMED);
            booked.setAppointmentTime(LocalTime.of(9, 30));
            when(appointmentRepository.findByDoctorIdAndAppointmentDateAndStatusIn(eq(1L), eq(TOMORROW), any()))
                    .thenReturn(List.of(booked));

            List<LocalTime> slots = service.getAvailableSlots(1L, TOMORROW);

            assertThat(slots).hasSize(13).doesNotContain(LocalTime.of(9, 30));
        }

        @Test
        void excludesSlotsAlreadyPassedToday() {
            when(doctorService.findById(1L)).thenReturn(doctor);
            when(appointmentRepository.findByDoctorIdAndAppointmentDateAndStatusIn(anyLong(), any(), any()))
                    .thenReturn(List.of());

            List<LocalTime> slots = service.getAvailableSlots(1L, TODAY);

            assertThat(slots).first().isEqualTo(LocalTime.of(10, 30));
        }

        @Test
        void emptyForPastDateOrUnavailableDoctor() {
            when(doctorService.findById(1L)).thenReturn(doctor);
            assertThat(service.getAvailableSlots(1L, TODAY.minusDays(1))).isEmpty();

            doctor.setAvailable(false);
            assertThat(service.getAvailableSlots(1L, TOMORROW)).isEmpty();
        }
    }

    @Nested
    class Booking {

        @BeforeEach
        void stubLookups() {
            when(userService.findByEmail("patient@hospital.com")).thenReturn(patient);
            when(doctorService.findById(1L)).thenReturn(doctor);
        }

        @Test
        void createsPendingAppointment() {
            when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Appointment result = service.book("patient@hospital.com", 1L, TOMORROW, LocalTime.of(10, 0),
                    "chest pain", "Cardiology");

            ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
            verify(appointmentRepository).save(captor.capture());
            Appointment saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(AppointmentStatus.PENDING);
            assertThat(saved.getPatient()).isSameAs(patient);
            assertThat(saved.getDoctor()).isSameAs(doctor);
            assertThat(saved.getSuggestedDepartment()).isEqualTo("Cardiology");
            assertThat(result).isSameAs(saved);
        }

        @Test
        void rejectsTakenSlot() {
            when(appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusIn(
                    eq(1L), eq(TOMORROW), eq(LocalTime.of(10, 0)), any())).thenReturn(true);

            assertThatThrownBy(() -> service.book("patient@hospital.com", 1L, TOMORROW, LocalTime.of(10, 0), null, null))
                    .isInstanceOf(BookingException.class)
                    .hasMessageContaining("already booked");
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        void rejectsPatientDoubleBooking() {
            when(appointmentRepository.existsByPatientIdAndAppointmentDateAndAppointmentTimeAndStatusIn(
                    eq(10L), eq(TOMORROW), eq(LocalTime.of(10, 0)), any())).thenReturn(true);

            assertThatThrownBy(() -> service.book("patient@hospital.com", 1L, TOMORROW, LocalTime.of(10, 0), null, null))
                    .isInstanceOf(BookingException.class)
                    .hasMessageContaining("already have an appointment");
        }

        @Test
        void rejectsPastTime() {
            assertThatThrownBy(() -> service.book("patient@hospital.com", 1L, TODAY, LocalTime.of(9, 0), null, null))
                    .isInstanceOf(BookingException.class)
                    .hasMessageContaining("future");
        }

        @Test
        void rejectsTimeOutsideSlots() {
            assertThatThrownBy(() -> service.book("patient@hospital.com", 1L, TOMORROW, LocalTime.of(13, 0), null, null))
                    .isInstanceOf(BookingException.class)
                    .hasMessageContaining("Invalid time slot");
        }

        @Test
        void rejectsUnavailableDoctor() {
            doctor.setAvailable(false);
            assertThatThrownBy(() -> service.book("patient@hospital.com", 1L, TOMORROW, LocalTime.of(10, 0), null, null))
                    .isInstanceOf(BookingException.class)
                    .hasMessageContaining("not accepting");
        }
    }

    @Nested
    class StatusChanges {

        @Test
        void patientCanCancelOwnAppointment() {
            Appointment a = appointment(AppointmentStatus.PENDING);
            when(appointmentRepository.findById(5L)).thenReturn(Optional.of(a));

            service.cancelByPatient(5L, "patient@hospital.com");

            assertThat(a.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        }

        @Test
        void patientCannotCancelSomeoneElsesAppointment() {
            when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment(AppointmentStatus.PENDING)));

            assertThatThrownBy(() -> service.cancelByPatient(5L, "other@hospital.com"))
                    .isInstanceOf(BookingException.class);
        }

        @Test
        void patientCannotCancelCompletedAppointment() {
            when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment(AppointmentStatus.COMPLETED)));

            assertThatThrownBy(() -> service.cancelByPatient(5L, "patient@hospital.com"))
                    .isInstanceOf(BookingException.class);
        }

        @Test
        void doctorConfirmsThenCompletesWithNotes() {
            Appointment a = appointment(AppointmentStatus.PENDING);
            when(appointmentRepository.findById(5L)).thenReturn(Optional.of(a));

            service.updateStatusByDoctor(5L, "ananya@hospital.com", AppointmentStatus.CONFIRMED, null);
            service.updateStatusByDoctor(5L, "ananya@hospital.com", AppointmentStatus.COMPLETED, "ECG normal");

            assertThat(a.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
            assertThat(a.getDoctorNotes()).isEqualTo("ECG normal");
        }

        @Test
        void doctorCannotCompletePendingAppointment() {
            when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment(AppointmentStatus.PENDING)));

            assertThatThrownBy(() ->
                    service.updateStatusByDoctor(5L, "ananya@hospital.com", AppointmentStatus.COMPLETED, null))
                    .isInstanceOf(BookingException.class)
                    .hasMessageContaining("Cannot change status");
        }

        @Test
        void doctorCannotUpdateAnotherDoctorsAppointment() {
            when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment(AppointmentStatus.PENDING)));

            assertThatThrownBy(() ->
                    service.updateStatusByDoctor(5L, "vikram@hospital.com", AppointmentStatus.CONFIRMED, null))
                    .isInstanceOf(BookingException.class)
                    .hasMessageContaining("not assigned to you");
        }
    }

    private Appointment appointment(AppointmentStatus status) {
        Appointment a = new Appointment();
        a.setId(5L);
        a.setPatient(patient);
        a.setDoctor(doctor);
        a.setAppointmentDate(TOMORROW);
        a.setAppointmentTime(LocalTime.of(10, 0));
        a.setStatus(status);
        return a;
    }
}
