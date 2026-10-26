package com.hospital.service;

import com.hospital.entity.Appointment;
import com.hospital.entity.AppointmentStatus;
import com.hospital.entity.Doctor;
import com.hospital.entity.User;
import com.hospital.exception.BookingException;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class AppointmentService {

    /** Statuses that occupy a time slot. */
    static final Set<AppointmentStatus> ACTIVE = EnumSet.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);

    /** Consultation slots: 09:00-17:00, every 30 minutes, with a lunch break 13:00-14:00. */
    public static final List<LocalTime> DAILY_SLOTS = buildDailySlots();

    private static final int MAX_DAYS_AHEAD = 60;

    private final AppointmentRepository appointmentRepository;
    private final DoctorService doctorService;
    private final UserService userService;
    private final Clock clock;

    public AppointmentService(AppointmentRepository appointmentRepository, DoctorService doctorService,
                              UserService userService, Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.doctorService = doctorService;
        this.userService = userService;
        this.clock = clock;
    }

    private static List<LocalTime> buildDailySlots() {
        List<LocalTime> slots = new ArrayList<>();
        for (LocalTime t = LocalTime.of(9, 0); t.isBefore(LocalTime.of(17, 0)); t = t.plusMinutes(30)) {
            if (t.getHour() != 13) {
                slots.add(t);
            }
        }
        return List.copyOf(slots);
    }

    @Transactional(readOnly = true)
    public List<LocalTime> getAvailableSlots(Long doctorId, LocalDate date) {
        Doctor doctor = doctorService.findById(doctorId);
        LocalDateTime now = LocalDateTime.now(clock);
        if (!doctor.isAvailable() || date.isBefore(now.toLocalDate())
                || date.isAfter(now.toLocalDate().plusDays(MAX_DAYS_AHEAD))) {
            return List.of();
        }
        Set<LocalTime> booked = new HashSet<>();
        appointmentRepository.findByDoctorIdAndAppointmentDateAndStatusIn(doctorId, date, ACTIVE)
                .forEach(a -> booked.add(a.getAppointmentTime()));

        return DAILY_SLOTS.stream()
                .filter(slot -> !booked.contains(slot))
                .filter(slot -> LocalDateTime.of(date, slot).isAfter(now))
                .toList();
    }

    @Transactional
    public Appointment book(String patientEmail, Long doctorId, LocalDate date, LocalTime time,
                            String symptoms, String suggestedDepartment) {
        User patient = userService.findByEmail(patientEmail);
        Doctor doctor = doctorService.findById(doctorId);
        LocalDateTime now = LocalDateTime.now(clock);

        if (!doctor.isAvailable()) {
            throw new BookingException("Dr. " + doctor.getName() + " is not accepting appointments");
        }
        if (!DAILY_SLOTS.contains(time)) {
            throw new BookingException("Invalid time slot: " + time);
        }
        if (!LocalDateTime.of(date, time).isAfter(now)) {
            throw new BookingException("Appointment time must be in the future");
        }
        if (date.isAfter(now.toLocalDate().plusDays(MAX_DAYS_AHEAD))) {
            throw new BookingException("Appointments can be booked at most " + MAX_DAYS_AHEAD + " days ahead");
        }
        if (appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusIn(
                doctorId, date, time, ACTIVE)) {
            throw new BookingException("This slot is already booked. Please choose another time.");
        }
        if (appointmentRepository.existsByPatientIdAndAppointmentDateAndAppointmentTimeAndStatusIn(
                patient.getId(), date, time, ACTIVE)) {
            throw new BookingException("You already have an appointment at this time");
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(date);
        appointment.setAppointmentTime(time);
        appointment.setSymptoms(symptoms);
        appointment.setSuggestedDepartment(suggestedDepartment);
        appointment.setStatus(AppointmentStatus.PENDING);
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment cancelByPatient(Long appointmentId, String patientEmail) {
        Appointment appointment = findById(appointmentId);
        if (!appointment.getPatient().getEmail().equalsIgnoreCase(patientEmail)) {
            throw new BookingException("You can only cancel your own appointments");
        }
        if (!ACTIVE.contains(appointment.getStatus())) {
            throw new BookingException("Only pending or confirmed appointments can be cancelled");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        return appointment;
    }

    /**
     * Allowed doctor transitions: PENDING -> CONFIRMED | CANCELLED, CONFIRMED -> COMPLETED | CANCELLED.
     */
    @Transactional
    public Appointment updateStatusByDoctor(Long appointmentId, String doctorEmail,
                                            AppointmentStatus newStatus, String notes) {
        Appointment appointment = findById(appointmentId);
        if (!appointment.getDoctor().getUser().getEmail().equalsIgnoreCase(doctorEmail)) {
            throw new BookingException("This appointment is not assigned to you");
        }
        AppointmentStatus current = appointment.getStatus();
        boolean allowed = switch (current) {
            case PENDING -> newStatus == AppointmentStatus.CONFIRMED || newStatus == AppointmentStatus.CANCELLED;
            case CONFIRMED -> newStatus == AppointmentStatus.COMPLETED || newStatus == AppointmentStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!allowed) {
            throw new BookingException("Cannot change status from " + current + " to " + newStatus);
        }
        appointment.setStatus(newStatus);
        if (notes != null && !notes.isBlank()) {
            appointment.setDoctorNotes(notes.trim());
        }
        return appointment;
    }

    @Transactional(readOnly = true)
    public Appointment findById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Appointment> findForPatient(String patientEmail) {
        User patient = userService.findByEmail(patientEmail);
        return appointmentRepository.findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(patient.getId());
    }

    @Transactional(readOnly = true)
    public List<Appointment> findForDoctor(String doctorEmail) {
        Doctor doctor = doctorService.findByEmail(doctorEmail);
        return appointmentRepository.findByDoctorIdOrderByAppointmentDateDescAppointmentTimeDesc(doctor.getId());
    }

    @Transactional(readOnly = true)
    public List<Appointment> findAll() {
        return appointmentRepository.findAllByOrderByAppointmentDateDescAppointmentTimeDesc();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> statistics() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", appointmentRepository.count());
        stats.put("today", appointmentRepository.countByAppointmentDate(LocalDate.now(clock)));
        for (AppointmentStatus status : AppointmentStatus.values()) {
            stats.put(status.name().toLowerCase(), appointmentRepository.countByStatus(status));
        }
        return stats;
    }
}
