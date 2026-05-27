package com.istrazidomace.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "places")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Place extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 200)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Category category = Category.OSTALO;

    @Builder.Default
    @Column(name = "avg_rating")
    private BigDecimal avgRating = BigDecimal.ZERO;

    public enum Category {
        VIKENDICA("Vikendica / Kuća za odmor"),
        ETNO_SELO("Etno selo"),
        PRENOCISTE("Prenoćište / Motel"),
        RESTORAN("Restoran"),
        KAFANA("Kafana"),
        SEOSKO_DOMACINSTVO("Seosko domaćinstvo"),
        RANC("Ranč"),
        VINARIJA("Vinarija"),
        BANJA("Banja / Spa"),
        IZLETISTE("Izletište"),
        OSTALO("Ostalo");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}