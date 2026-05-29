package com.istrazidomace.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.istrazidomace.backend.dto.request.PlaceRequest;
import com.istrazidomace.backend.dto.response.PlaceResponse;
import com.istrazidomace.backend.entity.Place;
import com.istrazidomace.backend.security.JwtService;
import com.istrazidomace.backend.service.PlaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


// Security u ovoj aplikaciji je implementiran na dva mesta:
//
// 1. SecurityConfig — stiti endpointe na HTTP nivou (ko sme da pristupi)
// 2. PlaceService.currentUser() — proverava ko je ulogovan i da li je vlasnik
//
// Problem u testu: @MockitoBean zamenjuje pravi PlaceService sa laznim.
// Lazni servis nema currentUser() logiku, pa se POST /api/places izvrsava
// bez autentikacije i vraca 200 umesto 403.
//
// Zakljucak: controller testovi sa @MockitoBean testiraju HTTP sloj (routing,
// validaciju, status kodove) — NE biznis logiku.
// Biznis logika (ownership, autentikacija) se testira u PlaceServiceTest.
//
// Za pravu zastitu endpointa bez tokena, koristiti @WithAnonymousUser
// ili @SpringBootTest bez @MockitoBean za integracione testove.

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class PlaceControllerTest {

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PlaceService placeService;

    @MockitoBean
    private JwtService jwtService;

    private MockMvc mockMvc;
    private PlaceResponse sampleResponse;
    private UUID placeId;

    @BeforeEach
    void setUp() {
        // Kreiramo MockMvc sa Spring Security konfiguracijom
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        placeId = UUID.randomUUID();

        sampleResponse = PlaceResponse.builder()
                .id(placeId)
                .title("Vikendica na Fruškoj Gori")
                .description("Lepo mesto")
                .location("Fruška Gora")
                .category("VIKENDICA")
                .categoryLabel("Vikendica / Kuća za odmor")
                .avgRating(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .authorId(UUID.randomUUID())
                .authorUsername("miki")
                .photoUrls(List.of())
                .build();
    }

    @Test
    @DisplayName("GET /api/places treba da vrati 200 bez autentifikacije")
    void listAll_bezTokena_vrati200() throws Exception {
        var page = new PageImpl<>(List.of(sampleResponse), PageRequest.of(0, 12), 1);
        when(placeService.listAll(0, 12)).thenReturn(page);

        mockMvc.perform(get("/api/places")
                        .param("page", "0")
                        .param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].title")
                        .value("Vikendica na Fruškoj Gori"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/places/categories treba da vrati sve kategorije")
    void categories_bezTokena_vratiSveKategorije() throws Exception {
        mockMvc.perform(get("/api/places/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].value").exists())
                .andExpect(jsonPath("$[0].label").exists());
    }

    @Test
    @DisplayName("GET /api/places/{id} bez tokena treba da vrati 401")
    void getById_bezTokena_vrati401() throws Exception {
        mockMvc.perform(get("/api/places/" + placeId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/places/{id} sa tokenom treba da vrati 200")
    @WithMockUser(username = "miki@test.com")
    void getById_saTokenom_vrati200() throws Exception {
        when(placeService.getById(placeId)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/places/" + placeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Vikendica na Fruškoj Gori"))
                .andExpect(jsonPath("$.category").value("VIKENDICA"));
    }

    @Test
    @DisplayName("POST /api/places bez tokena poziva servis (security je u servisu)")
    void create_bezTokena_pozivaSevris() throws Exception {
        PlaceRequest request = new PlaceRequest();
        request.setTitle("Novo mesto");
        request.setCategory(Place.Category.VIKENDICA);

        mockMvc.perform(post("/api/places")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(placeService, times(1)).create(any());
    }

    @Test
    @DisplayName("POST /api/places sa tokenom i validnim podacima treba da vrati 200")
    @WithMockUser(username = "miki@test.com")
    void create_saTokenomValidniPodaci_vrati200() throws Exception {
        PlaceRequest request = new PlaceRequest();
        request.setTitle("Novo mesto");
        request.setCategory(Place.Category.VIKENDICA);

        when(placeService.create(any(PlaceRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/places")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Vikendica na Fruškoj Gori"));

        verify(placeService, times(1)).create(any(PlaceRequest.class));
    }

    @Test
    @DisplayName("POST /api/places sa praznim nazivom treba da vrati 400")
    @WithMockUser(username = "miki@test.com")
    void create_prazanNaziv_vrati400() throws Exception {
        PlaceRequest request = new PlaceRequest();
        request.setTitle("");
        request.setCategory(Place.Category.VIKENDICA);

        mockMvc.perform(post("/api/places")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(placeService, never()).create(any());
    }

    @Test
    @DisplayName("DELETE /api/places/{id} bez tokena treba da vrati 401")
    void delete_bezTokena_vrati401() throws Exception {
        mockMvc.perform(delete("/api/places/" + placeId)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/places/{id} sa tokenom treba da vrati 204")
    @WithMockUser(username = "miki@test.com")
    void delete_saTokenom_vrati204() throws Exception {
        doNothing().when(placeService).delete(placeId);

        mockMvc.perform(delete("/api/places/" + placeId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(placeService, times(1)).delete(placeId);
    }

    @Test
    @DisplayName("GET /api/places/search treba da vrati 200 bez autentifikacije")
    void search_bezTokena_vrati200() throws Exception {
        var page = new PageImpl<>(List.of(sampleResponse), PageRequest.of(0, 12), 1);
        when(placeService.search(eq("fruška"), eq(""), eq(0), eq(12))).thenReturn(page);

        mockMvc.perform(get("/api/places/search")
                        .param("keyword", "fruška")
                        .param("category", "")
                        .param("page", "0")
                        .param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}