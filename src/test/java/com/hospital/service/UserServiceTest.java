package com.hospital.service;

import com.hospital.dto.RegistrationForm;
import com.hospital.entity.Role;
import com.hospital.entity.User;
import com.hospital.exception.BookingException;
import com.hospital.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    @Test
    void registersPatientWithEncodedPasswordAndNormalizedEmail() {
        when(userRepository.existsByEmailIgnoreCase("new@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = userService.registerPatient(form("  New@Mail.com ", "secret1", "secret1"));

        assertThat(user.getRole()).isEqualTo(Role.PATIENT);
        assertThat(user.getEmail()).isEqualTo("new@mail.com");
        assertThat(user.getPassword()).isEqualTo("ENCODED");
    }

    @Test
    void rejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("taken@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerPatient(form("taken@mail.com", "secret1", "secret1")))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("already exists");
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsPasswordMismatch() {
        assertThatThrownBy(() -> userService.registerPatient(form("a@mail.com", "secret1", "secret2")))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("do not match");
        verifyNoInteractions(userRepository);
    }

    private static RegistrationForm form(String email, String password, String confirm) {
        RegistrationForm form = new RegistrationForm();
        form.setFullName("New Patient");
        form.setEmail(email);
        form.setPhone("9999999999");
        form.setPassword(password);
        form.setConfirmPassword(confirm);
        return form;
    }
}
