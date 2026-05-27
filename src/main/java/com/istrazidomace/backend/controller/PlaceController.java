package com.istrazidomace.backend.controller;

import com.istrazidomace.backend.dto.request.PlaceRequest;
import com.istrazidomace.backend.dto.response.PlaceResponse;
import com.istrazidomace.backend.entity.Place;
import com.istrazidomace.backend.service.PlaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    @GetMapping
    public ResponseEntity<Page<PlaceResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(placeService.listAll(page, size));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PlaceResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(placeService.search(keyword, category, page, size));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Map<String, String>>> categories() {
        List<Map<String, String>> categories = Arrays.stream(Place.Category.values())
                .map(c -> Map.of("value", c.name(), "label", c.getLabel()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlaceResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(placeService.getById(id));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<PlaceResponse>> myPlaces(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(placeService.myPlaces(page, size));
    }

    @PostMapping
    public ResponseEntity<PlaceResponse> create(
            @Valid @RequestBody PlaceRequest request) {
        return ResponseEntity.ok(placeService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlaceResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody PlaceRequest request) {
        return ResponseEntity.ok(placeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        placeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/photos")
    public ResponseEntity<PlaceResponse> uploadPhotos(
            @PathVariable UUID id,
            @RequestParam("files") List<MultipartFile> files) throws IOException {
        return ResponseEntity.ok(placeService.uploadPhotos(id, files));
    }

    @DeleteMapping("/{placeId}/photos/{photoId}")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable UUID placeId,
            @PathVariable UUID photoId) {
        placeService.deletePhoto(placeId, photoId);
        return ResponseEntity.noContent().build();
    }
}