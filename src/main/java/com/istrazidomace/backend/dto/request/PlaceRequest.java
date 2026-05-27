package com.istrazidomace.backend.dto.request;

import com.istrazidomace.backend.entity.Place;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PlaceRequest {

    @NotBlank(message = "Naziv je obavezan")
    @Size(max = 100, message = "Naziv može imati najviše 100 karaktera")
    private String title;

    @Size(max = 5000, message = "Opis može imati najviše 5000 karaktera")
    private String description;

    @Size(max = 200, message = "Lokacija može imati najviše 200 karaktera")
    private String location;

    @NotNull(message = "Kategorija je obavezna")
    private Place.Category category;
}