package com.istrazidomace.backend.service;

import com.istrazidomace.backend.dto.request.PlaceRequest;
import com.istrazidomace.backend.dto.response.PlaceResponse;
import com.istrazidomace.backend.entity.Photo;
import com.istrazidomace.backend.entity.Place;
import com.istrazidomace.backend.entity.User;
import com.istrazidomace.backend.repository.PhotoRepository;
import com.istrazidomace.backend.repository.PlaceRepository;
import com.istrazidomace.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlaceServiceTest {

    @Mock private PlaceRepository placeRepository;
    @Mock private PhotoRepository photoRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private PlaceService placeService;

    private User vlasnik;
    private User drugiKorisnik;
    private Place place;
    private UUID placeId;

    @BeforeEach
    void setUp() {
        placeId = UUID.randomUUID();

        vlasnik = User.builder()
                .id(UUID.randomUUID())
                .username("miki")
                .email("miki@test.com")
                .role(User.Role.USER)
                .isActive(true)
                .build();

        drugiKorisnik = User.builder()
                .id(UUID.randomUUID())
                .username("pera")
                .email("pera@test.com")
                .role(User.Role.USER)
                .isActive(true)
                .build();

        place = Place.builder()
                .id(placeId)
                .author(vlasnik)
                .title("Vikendica na Fruškoj Gori")
                .description("Lepo mesto")
                .location("Fruška Gora")
                .category(Place.Category.VIKENDICA)
                .avgRating(BigDecimal.ZERO)
                .build();

        // Podešavamo SecurityContext da vraća ulogovanog korisnika
        // Ovo simulira @WithMockUser — kaže "trenutno je ulogovan miki@test.com"
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("miki@test.com");
    }

    // ── CREATE TESTOVI ────────────────────────────────────────────────────

    @Test
    @DisplayName("Kreiranje mesta sa validnim podacima treba da uspe")
    void create_validniPodaci_vratiPlaceResponse() {
        // ARRANGE
        PlaceRequest request = new PlaceRequest();
        request.setTitle("Vikendica na Fruškoj Gori");
        request.setDescription("Lepo mesto");
        request.setLocation("Fruška Gora");
        request.setCategory(Place.Category.VIKENDICA);

        when(userRepository.findByEmail("miki@test.com"))
                .thenReturn(Optional.of(vlasnik));
        when(placeRepository.save(any(Place.class)))
                .thenReturn(place);
        when(photoRepository.findByEntityIdAndEntityTypeOrderBySortOrderAsc(any(), any()))
                .thenReturn(List.of());

        // ACT
        PlaceResponse response = placeService.create(request);

        // ASSERT
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Vikendica na Fruškoj Gori");
        assertThat(response.getAuthorUsername()).isEqualTo("miki");
        assertThat(response.getCategoryLabel()).isEqualTo("Vikendica / Kuća za odmor");

        verify(placeRepository, times(1)).save(any(Place.class));
    }

    // ── GET BY ID TESTOVI ─────────────────────────────────────────────────

    @Test
    @DisplayName("getById sa nepostojećim ID-em treba da baci RuntimeException")
    void getById_nePostojeciId_bacaException() {
        // ARRANGE — mesto ne postoji u bazi
        UUID nepostojeciId = UUID.randomUUID();
        when(placeRepository.findByIdWithAuthor(nepostojeciId))
                .thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> placeService.getById(nepostojeciId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Mesto nije pronađeno");
    }

    // ── DELETE TESTOVI ────────────────────────────────────────────────────

    @Test
    @DisplayName("Brisanje mesta od strane vlasnika treba da uspe")
    void delete_vlasnik_uspesnoBrisanje() {
        // ARRANGE
        when(userRepository.findByEmail("miki@test.com"))
                .thenReturn(Optional.of(vlasnik));
        when(placeRepository.findByIdWithAuthor(placeId))
                .thenReturn(Optional.of(place));
        when(photoRepository.findByEntityIdAndEntityTypeOrderBySortOrderAsc(any(), any()))
                .thenReturn(List.of()); // nema fotografija

        // ACT
        placeService.delete(placeId);

        // ASSERT — proveravamo da je delete pozvan
        verify(placeRepository, times(1)).delete(place);
        verify(photoRepository, times(1))
                .deleteByEntityIdAndEntityType(placeId, Photo.EntityType.PLACE);
    }

    @Test
    @DisplayName("Brisanje tuđeg mesta treba da baci RuntimeException")
    void delete_nijeVlasnik_bacaException() {
        // ARRANGE — ulogovan je drugiKorisnik, ali place pripada vlasniku
        when(authentication.getName()).thenReturn("pera@test.com");
        when(userRepository.findByEmail("pera@test.com"))
                .thenReturn(Optional.of(drugiKorisnik));
        when(placeRepository.findByIdWithAuthor(placeId))
                .thenReturn(Optional.of(place));

        // ACT + ASSERT
        assertThatThrownBy(() -> placeService.delete(placeId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Nemate dozvolu");

        // delete() se NE sme pozvati
        verify(placeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Admin može da briše tuđe mesto")
    void delete_admin_mozeObrisatiTudeMesto() {
        // ARRANGE — admin je ulogovan
        User admin = User.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .email("admin@test.com")
                .role(User.Role.ADMIN)
                .isActive(true)
                .build();

        when(authentication.getName()).thenReturn("admin@test.com");
        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(admin));
        when(placeRepository.findByIdWithAuthor(placeId))
                .thenReturn(Optional.of(place));
        when(photoRepository.findByEntityIdAndEntityTypeOrderBySortOrderAsc(any(), any()))
                .thenReturn(List.of());

        // ACT
        placeService.delete(placeId);

        // ASSERT — admin može
        verify(placeRepository, times(1)).delete(place);
    }

    // ── UPLOAD FOTO TESTOVI ───────────────────────────────────────────────

    @Test
    @DisplayName("Upload fotografija preko limita od 8 treba da baci exception")
    void uploadPhotos_prekoracenLimit_bacaException() {
        // ARRANGE — već ima 7 fotografija
        when(userRepository.findByEmail("miki@test.com"))
                .thenReturn(Optional.of(vlasnik));
        when(placeRepository.findByIdWithAuthor(placeId))
                .thenReturn(Optional.of(place));
        when(photoRepository.countByEntityIdAndEntityType(placeId, Photo.EntityType.PLACE))
                .thenReturn(7L);

        // Pokušavamo da uploadujemo 2 nove = ukupno 9, što je > 8
        MockMultipartFile foto1 = new MockMultipartFile(
                "files", "foto1.jpg", "image/jpeg", new byte[100]);
        MockMultipartFile foto2 = new MockMultipartFile(
                "files", "foto2.jpg", "image/jpeg", new byte[100]);

        // ACT + ASSERT
        assertThatThrownBy(() -> placeService.uploadPhotos(placeId, List.of(foto1, foto2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Maksimalan broj fotografija");
    }

    @Test
    @DisplayName("Upload fajla koji nije slika treba da baci exception")
    void uploadPhotos_pogresanTipFajla_bacaException() {
        // ARRANGE
        when(userRepository.findByEmail("miki@test.com"))
                .thenReturn(Optional.of(vlasnik));
        when(placeRepository.findByIdWithAuthor(placeId))
                .thenReturn(Optional.of(place));
        when(photoRepository.countByEntityIdAndEntityType(placeId, Photo.EntityType.PLACE))
                .thenReturn(0L);

        // Pokušavamo da uploadujemo PDF umesto slike
        MockMultipartFile pdf = new MockMultipartFile(
                "files", "dokument.pdf", "application/pdf", new byte[100]);

        // ACT + ASSERT
        assertThatThrownBy(() -> placeService.uploadPhotos(placeId, List.of(pdf)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Dozvoljeni su samo JPEG");
    }

    @Test
    @DisplayName("Upload validne slike treba da uspe")
    void uploadPhotos_validnaSlikaJpeg_uspesnoUploadovanje() throws IOException {
        // ARRANGE
        when(userRepository.findByEmail("miki@test.com"))
                .thenReturn(Optional.of(vlasnik));
        when(placeRepository.findByIdWithAuthor(placeId))
                .thenReturn(Optional.of(place));
        when(photoRepository.countByEntityIdAndEntityType(placeId, Photo.EntityType.PLACE))
                .thenReturn(0L);
        when(photoRepository.save(any(Photo.class)))
                .thenReturn(new Photo());
        when(photoRepository.findByEntityIdAndEntityTypeOrderBySortOrderAsc(any(), any()))
                .thenReturn(List.of());

        // MockMultipartFile simulira pravi fajl bez diska
        MockMultipartFile slika = new MockMultipartFile(
                "files", "slika.jpg", "image/jpeg", new byte[100]);

        // ACT + ASSERT — ne sme baciti exception
        // Koristimo assertThatCode umesto assertThatThrownBy
        // jer ovde očekujemo da PROĐE bez greške
        assertThatCode(() -> placeService.uploadPhotos(placeId, List.of(slika)))
                .doesNotThrowAnyException();

        verify(photoRepository, times(1)).save(any(Photo.class));
    }

    // ── UPDATE TESTOVI ────────────────────────────────────────────────────

    @Test
    @DisplayName("Izmena tuđeg mesta treba da baci exception")
    void update_nijeVlasnik_bacaException() {
        // ARRANGE
        when(authentication.getName()).thenReturn("pera@test.com");
        when(userRepository.findByEmail("pera@test.com"))
                .thenReturn(Optional.of(drugiKorisnik));
        when(placeRepository.findByIdWithAuthor(placeId))
                .thenReturn(Optional.of(place));

        PlaceRequest request = new PlaceRequest();
        request.setTitle("Novi naziv");
        request.setCategory(Place.Category.KAFANA);

        // ACT + ASSERT
        assertThatThrownBy(() -> placeService.update(placeId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Nemate dozvolu");

        verify(placeRepository, never()).save(any());
    }
}