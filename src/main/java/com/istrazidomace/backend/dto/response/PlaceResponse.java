package com.istrazidomace.backend.dto.response;

import com.istrazidomace.backend.entity.Place;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PlaceResponse {

    private UUID id;
    private String title;
    private String description;
    private String location;
    private String category;
    private String categoryLabel;
    private BigDecimal avgRating;
    private LocalDateTime createdAt;

    private UUID authorId;
    private String authorUsername;

    private List<String> photoUrls;

    public static PlaceResponse from(Place place, List<String> photoUrls) {
        return PlaceResponse.builder()
                .id(place.getId())
                .title(place.getTitle())
                .description(place.getDescription())
                .location(place.getLocation())
                .category(place.getCategory().name())
                .categoryLabel(place.getCategory().getLabel())
                .avgRating(place.getAvgRating())
                .createdAt(place.getCreatedAt())
                .authorId(place.getAuthor().getId())
                .authorUsername(place.getAuthor().getUsername())
                .photoUrls(photoUrls)
                .build();
    }
}