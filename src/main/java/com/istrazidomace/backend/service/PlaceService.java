package com.istrazidomace.backend.service;

import com.istrazidomace.backend.dto.request.PlaceRequest;
import com.istrazidomace.backend.dto.response.PlaceResponse;
import com.istrazidomace.backend.entity.Photo;
import com.istrazidomace.backend.entity.Place;
import com.istrazidomace.backend.entity.User;
import com.istrazidomace.backend.repository.PhotoRepository;
import com.istrazidomace.backend.repository.PlaceRepository;
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
public class PlaceService {

    private static final int MAX_PHOTOS = 8;
    private static final String UPLOAD_DIR = "uploads/places/";
    private static final List<String> ALLOWED_TYPES =
            List.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final PlaceRepository placeRepository;
    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;

    public Page<PlaceResponse> listAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return placeRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    public Page<PlaceResponse> search(String keyword, String category, int page, int size) {
        Place.Category cat = null;
        if (category != null && !category.isBlank()) {
            cat = Place.Category.valueOf(category);
        }
        Pageable pageable = PageRequest.of(page, size);
        return placeRepository.search(keyword, cat, pageable)
                .map(this::toResponse);
    }

    public PlaceResponse getById(UUID id) {
        Place place = findPlaceOrThrow(id);
        return toResponse(place);
    }

    @Transactional
    public PlaceResponse create(PlaceRequest request) {
        User author = currentUser();
        Place place = Place.builder()
                .author(author)
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .category(request.getCategory())
                .build();
        return toResponse(placeRepository.save(place));
    }

    @Transactional
    public PlaceResponse update(UUID id, PlaceRequest request) {
        Place place = findPlaceOrThrow(id);
        assertOwner(place);
        place.setTitle(request.getTitle());
        place.setDescription(request.getDescription());
        place.setLocation(request.getLocation());
        place.setCategory(request.getCategory());
        return toResponse(placeRepository.save(place));
    }

    @Transactional
    public void delete(UUID id) {
        Place place = findPlaceOrThrow(id);
        assertOwner(place);
        photoRepository.deleteByEntityIdAndEntityType(id, Photo.EntityType.PLACE);
        placeRepository.delete(place);
    }

    @Transactional
    public PlaceResponse uploadPhotos(UUID placeId, List<MultipartFile> files) throws IOException {
        Place place = findPlaceOrThrow(placeId);
        assertOwner(place);

        long existing = photoRepository.countByEntityIdAndEntityType(placeId, Photo.EntityType.PLACE);
        if (existing + files.size() > MAX_PHOTOS) {
            throw new IllegalStateException(
                    "Maksimalan broj fotografija je " + MAX_PHOTOS +
                            ". Trenutno imate " + existing + ".");
        }

        // Validacija tipa fajla
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
                    .entityId(placeId)
                    .entityType(Photo.EntityType.PLACE)
                    .url("/uploads/places/" + filename)
                    .sortOrder(order++)
                    .build();
            photoRepository.save(photo);
        }

        return toResponse(place);
    }

    @Transactional
    public void deletePhoto(UUID placeId, UUID photoId) {
        Place place = findPlaceOrThrow(placeId);
        assertOwner(place);

        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Fotografija nije pronađena"));

        if (!photo.getEntityId().equals(placeId)) {
            throw new RuntimeException("Fotografija ne pripada ovom mestu");
        }

        photoRepository.delete(photo);
    }

    public Page<PlaceResponse> myPlaces(int page, int size) {
        UUID authorId = currentUser().getId();
        Pageable pageable = PageRequest.of(page, size);
        return placeRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, pageable)
                .map(this::toResponse);
    }

    private Place findPlaceOrThrow(UUID id) {
        return placeRepository.findByIdWithAuthor(id)
                .orElseThrow(() -> new RuntimeException("Mesto nije pronađeno"));
    }

    private void assertOwner(Place place) {
        User current = currentUser();
        if (!place.getAuthor().getId().equals(current.getId())
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

    private PlaceResponse toResponse(Place place) {
        List<String> urls = photoRepository
                .findByEntityIdAndEntityTypeOrderBySortOrderAsc(place.getId(), Photo.EntityType.PLACE)
                .stream()
                .map(Photo::getUrl)
                .toList();
        return PlaceResponse.from(place, urls);
    }
}