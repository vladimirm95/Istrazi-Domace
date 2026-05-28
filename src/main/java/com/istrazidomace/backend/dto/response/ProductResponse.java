package com.istrazidomace.backend.dto.response;

import com.istrazidomace.backend.entity.Product;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProductResponse {

    private UUID id;
    private String title;
    private String description;
    private BigDecimal price;
    private String unit;
    private BigDecimal avgRating;
    private LocalDateTime createdAt;

    private UUID authorId;
    private String authorUsername;

    private List<String> photoUrls;

    public static ProductResponse from(Product product, List<String> photoUrls) {
        return ProductResponse.builder()
                .id(product.getId())
                .title(product.getTitle())
                .description(product.getDescription())
                .price(product.getPrice())
                .unit(product.getUnit())
                .avgRating(product.getAvgRating())
                .createdAt(product.getCreatedAt())
                .authorId(product.getAuthor().getId())
                .authorUsername(product.getAuthor().getUsername())
                .photoUrls(photoUrls)
                .build();
    }
}