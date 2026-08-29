package com.neighborlink.listing_service.service;

import com.neighborlink.listing_service.dto.ListingRequest;
import com.neighborlink.listing_service.dto.ListingResponse;
import com.neighborlink.listing_service.entity.Listing;
import com.neighborlink.listing_service.entity.ListingStatus;
import com.neighborlink.listing_service.exception.ListingNotFoundException;
import com.neighborlink.listing_service.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;

    @Transactional
    public ListingResponse createListing(
            ListingRequest request,
            String currentUserId) {

        Listing listing = Listing.builder()
                .ownerId(currentUserId)
                .societyId(request.getSocietyId())
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .pricePerDay(request.getPricePerDay())
                .status(parseStatus(request.getStatus()))
                .build();

        Listing savedListing =
                listingRepository.save(listing);

        return ListingResponse.from(savedListing);
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> getAllListings(
            Long societyId,
            String category,
            ListingStatus status) {

        List<Listing> listings;

        if (societyId != null
                && category != null
                && status != null) {

            listings =
                    listingRepository
                            .findBySocietyIdAndCategoryIgnoreCaseAndStatus(
                                    societyId,
                                    category,
                                    status
                            );

        } else if (societyId != null
                && status != null) {

            listings =
                    listingRepository
                            .findBySocietyIdAndStatus(
                                    societyId,
                                    status
                            );

        } else if (category != null
                && status != null) {

            listings =
                    listingRepository
                            .findByCategoryIgnoreCaseAndStatus(
                                    category,
                                    status
                            );

        } else if (societyId != null) {

            listings =
                    listingRepository
                            .findBySocietyId(societyId);

        } else if (category != null) {

            listings =
                    listingRepository
                            .findByCategoryIgnoreCase(category);

        } else if (status != null) {

            listings =
                    listingRepository
                            .findByStatus(status);

        } else {

            listings =
                    listingRepository.findAll();
        }

        return listings.stream()
                .map(ListingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ListingResponse getListingById(Long id) {

        Listing listing = findListing(id);

        return ListingResponse.from(listing);
    }

    @Transactional
    public ListingResponse updateListing(
            Long id,
            ListingRequest request,
            String currentUserId,
            String currentRole) {

        Listing listing = findListing(id);

        checkOwnershipOrAdmin(
                listing,
                currentUserId,
                currentRole
        );

        listing.setSocietyId(request.getSocietyId());
        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription());
        listing.setCategory(request.getCategory());
        listing.setPricePerDay(request.getPricePerDay());

        if (request.getStatus() != null) {
            listing.setStatus(
                    parseStatus(request.getStatus())
            );
        }

        Listing updatedListing =
                listingRepository.save(listing);

        return ListingResponse.from(updatedListing);
    }

    @Transactional
    public void deleteListing(
            Long id,
            String currentUserId,
            String currentRole) {

        Listing listing = findListing(id);

        checkOwnershipOrAdmin(
                listing,
                currentUserId,
                currentRole
        );

        listingRepository.delete(listing);
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> getListingsByOwner(
            String ownerId) {

        return listingRepository
                .findByOwnerId(ownerId)
                .stream()
                .map(ListingResponse::from)
                .toList();
    }

    private Listing findListing(Long id) {

        return listingRepository
                .findById(id)
                .orElseThrow(() ->
                        new ListingNotFoundException(
                                "Listing not found with id: " + id
                        )
                );
    }

    private void checkOwnershipOrAdmin(
            Listing listing,
            String currentUserId,
            String currentRole) {

        boolean isAdmin =
                "ADMIN".equals(currentRole);

        boolean isOwner =
                listing.getOwnerId()
                        .equals(currentUserId);

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException(
                    "You are not authorized to modify this listing"
            );
        }
    }

    private ListingStatus parseStatus(String status) {

        if (status == null || status.isBlank()) {
            return ListingStatus.ACTIVE;
        }

        try {
            return ListingStatus.valueOf(
                    status.trim().toUpperCase()
            );

        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid listing status. Allowed values: ACTIVE, INACTIVE"
            );
        }
    }
}