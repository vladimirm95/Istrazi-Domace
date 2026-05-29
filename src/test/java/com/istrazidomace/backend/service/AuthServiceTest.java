package com.istrazidomace.backend.service;

import com.istrazidomace.backend.dto.request.LoginRequest;
import com.istrazidomace.backend.dto.request.RegisterRequest;
import com.istrazidomace.backend.dto.response.AuthResponse;
import com.istrazidomace.backend.entity.User;
import com.istrazidomace.backend.repository.UserRepository;
import com.istrazidomace.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Kaže JUnit-u da koristi Mockito za kreiranje mockova
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // @Mock kreira lažni objekat - ne poziva pravu bazu/logiku
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    // @InjectMocks kreira pravi AuthService i ubacuje mockove u njega
    @InjectMocks
    private AuthService authService;

    // Test podaci koje koristimo u više testova
    private RegisterRequest validRegisterRequest;
    private User savedUser;

    // @BeforeEach se izvršava PRE svakog testa — priprema podatke
    @BeforeEach
    void setUp() {
        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setUsername("miki");
        validRegisterRequest.setEmail("miki@test.com");
        validRegisterRequest.setPassword("lozinka123");

        savedUser = User.builder()
                .id(UUID.randomUUID())
                .username("miki")
                .email("miki@test.com")
                .passwordHash("hashed_lozinka")
                .role(User.Role.USER)
                .isActive(true)
                .build();
    }

    // REGISTER TESTOVI

    @Test
    @DisplayName("Registracija sa validnim podacima treba da vrati token")
    void register_validniPodaci_vratiAuthResponse() {
        // ARRANGE — šta mockovi treba da vrate
        // Email nije zauzet
        when(userRepository.existsByEmail("miki@test.com")).thenReturn(false);
        // Username nije zauzet
        when(userRepository.existsByUsername("miki")).thenReturn(false);
        // Enkodovanje lozinke
        when(passwordEncoder.encode("lozinka123")).thenReturn("hashed_lozinka");
        // Čuvanje korisnika u "bazu"
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        // Generisanje tokena
        when(jwtService.generateToken(any())).thenReturn("jwt_token_123");

        // ACT — pozivamo metodu koju testiramo
        AuthResponse response = authService.register(validRegisterRequest);

        // ASSERT — proveravamo rezultat
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt_token_123");
        assertThat(response.getEmail()).isEqualTo("miki@test.com");
        assertThat(response.getUsername()).isEqualTo("miki");
        assertThat(response.getRole()).isEqualTo("USER");

        // Proveravamo da je userRepository.save() pozvan tačno jednom
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Registracija sa zauzetim emailom treba da baci RuntimeException")
    void register_emailZauzet_bacaException() {
        // ARRANGE — email JE zauzet
        when(userRepository.existsByEmail("miki@test.com")).thenReturn(true);

        // ACT + ASSERT - očekujemo exception
        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email je već zauzet");

        // Proveravamo da save() NIJE pozvan - ne treba čuvati ako email postoji
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Registracija sa zauzetim username-om treba da baci RuntimeException")
    void register_usernameZauzet_bacaException() {
        // Email slobodan, ali username zauzet
        when(userRepository.existsByEmail("miki@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("miki")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Username je već zauzet");

        verify(userRepository, never()).save(any(User.class));
    }

    // LOGIN TESTOVI

    @Test
    @DisplayName("Login sa ispravnim kredencijalima treba da vrati token")
    void login_ispravniKredencijali_vratiAuthResponse() {
        // ARRANGE
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("miki@test.com");
        loginRequest.setPassword("lozinka123");

        // AuthenticationManager ne baca exception = kredencijali su ispravni
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("miki@test.com"))
                .thenReturn(Optional.of(savedUser));
        when(jwtService.generateToken(any())).thenReturn("jwt_token_123");

        // ACT
        AuthResponse response = authService.login(loginRequest);

        // ASSERT
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt_token_123");
        assertThat(response.getEmail()).isEqualTo("miki@test.com");
    }

    @Test
    @DisplayName("Login sa pogrešnom lozinkom treba da baci exception")
    void login_pogresnaLozinka_bacaException() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("miki@test.com");
        loginRequest.setPassword("pogresna");

        // AuthenticationManager baca exception = loši kredencijali
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Pogrešni kredencijali"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);

        // findByEmail se NE poziva ako autentikacija nije prošla
        verify(userRepository, never()).findByEmail(any());
    }
}