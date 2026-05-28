package com.istrazidomace.backend.service;

import com.istrazidomace.backend.dto.request.ProductRequest;
import com.istrazidomace.backend.dto.response.ProductResponse;
import com.istrazidomace.backend.entity.Photo;
import com.istrazidomace.backend.entity.Product;
import com.istrazidomace.backend.entity.User;
import com.istrazidomace.backend.repository.PhotoRepository;
import com.istrazidomace.backend.repository.ProductRepository;
import com.istrazidomace.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int MAX_PHOTOS = 8;
    private static final String UPLOAD_DIR = "uploads/products/";
    private static final List<String> ALLOWED_TYPES =
            List.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final ProductRepository productRepository;
    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;

    public Page<ProductResponse> listAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    public Page<ProductResponse> search(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.search(keyword, pageable)
                .map(this::toResponse);
    }

    public ProductResponse getById(UUID id) {
        return toResponse(findProductOrThrow(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        User author = currentUser();
        Product product = Product.builder()
                .author(author)
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .unit(request.getUnit())
                .build();
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = findProductOrThrow(id);
        assertOwner(product);
        product.setTitle(request.getTitle());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setUnit(request.getUnit());
        return toResponse(productRepository.save(product));
    }


    @Transactional
    public ProductResponse uploadPhotos(UUID productId, List<MultipartFile> files) throws IOException {
        Product product = findProductOrThrow(productId);
        assertOwner(product);

        long existing = photoRepository.countByEntityIdAndEntityType(productId, Photo.EntityType.PRODUCT);
        if (existing + files.size() > MAX_PHOTOS) {
            throw new IllegalStateException(
                    "Maksimalan broj fotografija je " + MAX_PHOTOS +
                            ". Trenutno imate " + existing + ".");
        }

        for (MultipartFile file : files) {
            if (!ALLOWED_TYPES.contains(file.getContentType())) {
                throw new IllegalStateException(
                        "Dozvoljeni su samo JPEG, PNG, WebP i GIF fajlovi.");
            }
        }

        Path uploadPath = Paths.get(UPLOAD_DIR);
        Files.createDirectories(uploadPath);

        short order = (short) existing;
        for (MultipartFile file : files) {
            String original = file.getOriginalFilename();
            String ext = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf(".")).toLowerCase()
                    : ".jpg";
            String filename = UUID.randomUUID() + ext;
            Path dest = uploadPath.resolve(filename);
            file.transferTo(dest);

            Photo photo = Photo.builder()
                    .entityId(productId)
                    .entityType(Photo.EntityType.PRODUCT)
                    .url("/uploads/products/" + filename)
                    .sortOrder(order++)
                    .build();
            photoRepository.save(photo);
        }

        return toResponse(product);
    }

    @Transactional
    public void delete(UUID id) {
        Product product = findProductOrThrow(id);
        assertOwner(product);

        // Obrisi fajlove sa diska
        List<Photo> photos = photoRepository
                .findByEntityIdAndEntityTypeOrderBySortOrderAsc(id, Photo.EntityType.PRODUCT);
        for (Photo photo : photos) {
            deleteFileFromDisk(photo.getUrl());
        }

        photoRepository.deleteByEntityIdAndEntityType(id, Photo.EntityType.PRODUCT);
        productRepository.delete(product);
    }

    @Transactional
    public void deletePhoto(UUID productId, UUID photoId) {
        Product product = findProductOrThrow(productId);
        assertOwner(product);

        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Fotografija nije pronađena"));

        if (!photo.getEntityId().equals(productId)) {
            throw new RuntimeException("Fotografija ne pripada ovom proizvodu");
        }

        deleteFileFromDisk(photo.getUrl());
        photoRepository.delete(photo);
    }

    private void deleteFileFromDisk(String url) {
        try {
            Path filePath = Paths.get(url.substring(1));
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Nije moguće obrisati fajl: " + url + " - " + e.getMessage());
        }
    }


    public Page<ProductResponse> myProducts(int page, int size) {
        UUID authorId = currentUser().getId();
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, pageable)
                .map(this::toResponse);
    }

    private Product findProductOrThrow(UUID id) {
        return productRepository.findByIdWithAuthor(id)
                .orElseThrow(() -> new RuntimeException("Proizvod nije pronađen"));
    }

    private void assertOwner(Product product) {
        User current = currentUser();
        if (!product.getAuthor().getId().equals(current.getId())
                && current.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Nemate dozvolu za ovu akciju");
        }
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Korisnik nije pronađen"));
    }

    private ProductResponse toResponse(Product product) {
        List<String> urls = photoRepository
                .findByEntityIdAndEntityTypeOrderBySortOrderAsc(product.getId(), Photo.EntityType.PRODUCT)
                .stream()
                .map(Photo::getUrl)
                .toList();
        return ProductResponse.from(product, urls);
    }
}