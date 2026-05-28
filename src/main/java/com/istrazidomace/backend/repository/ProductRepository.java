package com.istrazidomace.backend.repository;

import com.istrazidomace.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query("SELECT p FROM Product p JOIN FETCH p.author ORDER BY p.createdAt DESC")
    Page<Product> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT p FROM Product p JOIN FETCH p.author WHERE p.author.id = :authorId ORDER BY p.createdAt DESC")
    Page<Product> findByAuthorIdOrderByCreatedAtDesc(@Param("authorId") UUID authorId, Pageable pageable);

    @Query("SELECT p FROM Product p JOIN FETCH p.author WHERE p.id = :id")
    Optional<Product> findByIdWithAuthor(@Param("id") UUID id);

    @Query("""
            SELECT p FROM Product p JOIN FETCH p.author
            WHERE (:keyword IS NULL OR
                   LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY p.createdAt DESC
            """)
    Page<Product> search(
            @Param("keyword") String keyword,
            Pageable pageable
    );
}