package com.istrazidomace.backend.repository;

import com.istrazidomace.backend.entity.Place;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PlaceRepository extends JpaRepository<Place, UUID> {

    @Query("SELECT p FROM Place p JOIN FETCH p.author ORDER BY p.createdAt DESC")
    Page<Place> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT p FROM Place p JOIN FETCH p.author WHERE p.author.id = :authorId ORDER BY p.createdAt DESC")
    Page<Place> findByAuthorIdOrderByCreatedAtDesc(@Param("authorId") UUID authorId, Pageable pageable);

    @Query("SELECT p FROM Place p JOIN FETCH p.author WHERE p.id = :id")
    Optional<Place> findByIdWithAuthor(@Param("id") UUID id);

    @Query("""
            SELECT p FROM Place p JOIN FETCH p.author
            WHERE (:keyword IS NULL OR
                   LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(p.location) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:category IS NULL OR p.category = :category)
            ORDER BY p.createdAt DESC
            """)
    Page<Place> search(
            @Param("keyword") String keyword,
            @Param("category") Place.Category category,
            Pageable pageable
    );
}