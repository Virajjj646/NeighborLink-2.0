package com.neighborlink.listing_service.controller;

import com.neighborlink.listing_service.dto.ListingRequest;
import com.neighborlink.listing_service.dto.ListingResponse;
import com.neighborlink.listing_service.entity.ListingStatus;
import com.neighborlink.listing_service.service.ListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    @PostMapping
    public ResponseEntity<ListingResponse> createListing(
            @Valid @RequestBody ListingRequest request,
            Authentication authentication) {

        String currentUserId =
                extractUserId(authentication);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        listingService.createListing(
                                request,
                                currentUserId
                        )
                );
    }

    @GetMapping
    public ResponseEntity<List<ListingResponse>> getAllListings(
            @RequestParam(required = false) Long societyId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ListingStatus status,
            @RequestParam(required = false) String search) {

        return ResponseEntity.ok(
                listingService.getAllListings(
                        societyId,
                        category,
                        status,
                        search
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> getListingById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                listingService.getListingById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ListingResponse> updateListing(
            @PathVariable Long id,
            @Valid @RequestBody ListingRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                listingService.updateListing(
                        id,
                        request,
                        extractUserId(authentication),
                        extractRole(authentication)
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteListing(
            @PathVariable Long id,
            Authentication authentication) {

        listingService.deleteListing(
                id,
                extractUserId(authentication),
                extractRole(authentication)
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<ListingResponse>> getListingsByOwner(
            @PathVariable String ownerId) {

        return ResponseEntity.ok(
                listingService.getListingsByOwner(ownerId)
        );
    }

    private String extractUserId(
            Authentication authentication) {

        return authentication.getName();
    }

    private String extractRole(
            Authentication authentication) {

        return authentication.getAuthorities()
                .stream()
                .findFirst()
                .map(authority ->
                        authority.getAuthority()
                                .replace("ROLE_", ""))
                .orElse("USER");
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories(){
        return ResponseEntity.ok(listingService.getCategories());
    }
}