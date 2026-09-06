package com.neighborlink.rental_service.service;

import com.neighborlink.rental_service.dto.ListingResponse;
import com.neighborlink.rental_service.dto.RentalAvailabilityResponse;
import com.neighborlink.rental_service.dto.RentalRequest;
import com.neighborlink.rental_service.dto.RentalResponse;
import com.neighborlink.rental_service.entity.Rental;
import com.neighborlink.rental_service.entity.RentalStatus;
import com.neighborlink.rental_service.exception.InvalidRentalException;
import com.neighborlink.rental_service.exception.RentalNotFoundException;
import com.neighborlink.rental_service.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RentalService {

    private final RentalRepository rentalRepository;
    private final ListingClient listingClient;
    private final PaymentClient paymentClient;

    @Transactional
    public RentalResponse createRental(
            RentalRequest request,
            String currentUserId,
            String authorizationHeader) {

        validateDates(
                request.getStartDate(),
                request.getEndDate()
        );

        ListingResponse listing;

        try {
            listing = listingClient.getListing(
                    request.getListingId(),
                    authorizationHeader
            );
        } catch (RestClientException ex) {
            throw new InvalidRentalException(
                    "Unable to verify listing availability"
            );
        }

        if (listing == null) {
            throw new InvalidRentalException(
                    "Listing not found"
            );
        }

        if (!"ACTIVE".equalsIgnoreCase(listing.getStatus())) {
            throw new InvalidRentalException(
                    "Listing is not available for rental"
            );
        }

        boolean overlapping =
                rentalRepository
                        .existsByListingIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                request.getListingId(),
                                List.of(
                                        RentalStatus.PENDING,
                                        RentalStatus.PAYMENT_PENDING,
                                        RentalStatus.CONFIRMED,
                                        RentalStatus.ACTIVE
                                ),
                                request.getEndDate(),
                                request.getStartDate()
                        );

        if (overlapping) {
            throw new InvalidRentalException(
                    "Listing is not available for the selected dates"
            );
        }

        BigDecimal pricePerDay =
                listing.getPricePerDay();

        if (pricePerDay == null
                || pricePerDay.compareTo(BigDecimal.ZERO) < 0) {

            throw new InvalidRentalException(
                    "Listing has an invalid rental price"
            );
        }

        long rentalDays =
                ChronoUnit.DAYS.between(
                        request.getStartDate(),
                        request.getEndDate()
                ) + 1;

        BigDecimal totalAmount =
                pricePerDay.multiply(
                        BigDecimal.valueOf(rentalDays)
                );

        Rental rental = Rental.builder()
                .renterId(currentUserId)
                .listingId(request.getListingId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalAmount(totalAmount)
                .status(RentalStatus.PAYMENT_PENDING)
                .build();

        Rental savedRental =
                rentalRepository.save(rental);

        try {

            paymentClient.createPayment(
                    savedRental.getId(),
                    currentUserId,
                    totalAmount,
                    "rental-" + savedRental.getId()
            );

        } catch (RestClientException ex) {

            throw new InvalidRentalException(
                    "Unable to create payment for rental"
            );
        }

        return RentalResponse.from(savedRental);
    }

    @Transactional(readOnly = true)
    public RentalResponse getRentalById(
            Long id,
            String currentUserId,
            String currentRole) {

        Rental rental = findRental(id);

        checkOwnerOrAdmin(
                rental,
                currentUserId,
                currentRole
        );

        return RentalResponse.from(rental);
    }

    @Transactional(readOnly = true)
    public List<RentalResponse> getMyRentals(
            String currentUserId) {

        return rentalRepository
                .findByRenterId(currentUserId)
                .stream()
                .map(RentalResponse::from)
                .toList();
    }

    @Transactional
    public RentalResponse cancelRental(
            Long id,
            String currentUserId,
            String currentRole) {

        Rental rental = findRental(id);

        checkOwnerOrAdmin(
                rental,
                currentUserId,
                currentRole
        );

        if (rental.getStatus() == RentalStatus.COMPLETED) {
            throw new InvalidRentalException(
                    "Completed rentals cannot be cancelled"
            );
        }

        if (rental.getStatus() == RentalStatus.CANCELLED) {
            throw new InvalidRentalException(
                    "Rental is already cancelled"
            );
        }

        rental.setStatus(RentalStatus.CANCELLED);

        return RentalResponse.from(
                rentalRepository.save(rental)
        );
    }

    @Transactional
    public RentalResponse completeRental(
            Long id,
            String currentRole) {

        if (!"ADMIN".equals(currentRole)) {
            throw new AccessDeniedException(
                    "Only ADMIN can complete a rental"
            );
        }

        Rental rental = findRental(id);

        // completeRental(...) — allow CONFIRMED as well as ACTIVE
        if (rental.getStatus() != RentalStatus.CONFIRMED
                && rental.getStatus() != RentalStatus.ACTIVE) {

            throw new InvalidRentalException(
                    "Only CONFIRMED or ACTIVE rentals can be completed"
            );
        }

        rental.setStatus(RentalStatus.COMPLETED);

        return RentalResponse.from(
                rentalRepository.save(rental)
        );
    }

    @Transactional
    public RentalResponse updateStatus(
            Long id,
            RentalStatus newStatus,
            String currentRole) {

        if (!"ADMIN".equals(currentRole)) {
            throw new AccessDeniedException(
                    "Only ADMIN can update rental status"
            );
        }

        Rental rental = findRental(id);

        rental.setStatus(newStatus);

        return RentalResponse.from(
                rentalRepository.save(rental)
        );
    }
    @Transactional(readOnly = true)
    public List<RentalResponse> getRentalsByListing(
            Long listingId,
            String currentUserId,
            String currentRole,
            String authorizationHeader) {

        if (!"ADMIN".equals(currentRole)) {

            ListingResponse listing;

            try {
                listing = listingClient.getListing(listingId, authorizationHeader);
            } catch (RestClientException ex) {
                throw new InvalidRentalException(
                        "Unable to verify listing ownership"
                );
            }

            if (listing == null
                    || !currentUserId.equals(listing.getOwnerId())) {

                throw new AccessDeniedException(
                        "You are not authorized to view rentals for this listing"
                );
            }
        }

        return rentalRepository
                .findByListingId(listingId)
                .stream()
                .map(RentalResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RentalAvailabilityResponse> getListingAvailability(Long listingId) {

        return rentalRepository
                .findByListingIdAndStatusIn(
                        listingId,
                        List.of(
                                RentalStatus.PENDING,
                                RentalStatus.PAYMENT_PENDING,
                                RentalStatus.CONFIRMED,
                                RentalStatus.ACTIVE
                        )
                )
                .stream()
                .map(rental -> new RentalAvailabilityResponse(
                        rental.getStartDate(),
                        rental.getEndDate()
                ))
                .toList();
    }

    private Rental findRental(Long id) {

        return rentalRepository
                .findById(id)
                .orElseThrow(() ->
                        new RentalNotFoundException(
                                "Rental not found with id: " + id
                        )
                );
    }

    private void validateDates(
            LocalDate startDate,
            LocalDate endDate) {

        if (endDate.isBefore(startDate)) {
            throw new InvalidRentalException(
                    "End date cannot be before start date"
            );
        }
    }

    private void checkOwnerOrAdmin(
            Rental rental,
            String currentUserId,
            String currentRole) {

        boolean isOwner =
                rental.getRenterId()
                        .equals(currentUserId);

        boolean isAdmin =
                "ADMIN".equals(currentRole);

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException(
                    "You are not authorized to access this rental"
            );
        }
    }

    @Transactional
    public RentalResponse confirmRental(Long id) {

        Rental rental = findRental(id);

        if (rental.getStatus() != RentalStatus.PAYMENT_PENDING) {
            throw new InvalidRentalException(
                    "Only PAYMENT_PENDING rentals can be confirmed"
            );
        }

        rental.setStatus(RentalStatus.CONFIRMED);

        return RentalResponse.from(
                rentalRepository.save(rental)
        );
    }

    // cancelRentalInternal(...) — idempotent, and no longer requires CONFIRMED
    @Transactional
    public RentalResponse cancelRentalInternal(Long id) {

        Rental rental = findRental(id);

        if (rental.getStatus() == RentalStatus.CANCELLED) {
            return RentalResponse.from(rental);
        }

        if (rental.getStatus() == RentalStatus.COMPLETED) {
            throw new InvalidRentalException(
                    "Completed rentals cannot be cancelled"
            );
        }

        rental.setStatus(RentalStatus.CANCELLED);

        return RentalResponse.from(
                rentalRepository.save(rental)
        );
    }

    @Transactional(readOnly = true)
    public List<RentalResponse> getAllRentals(
            String currentRole,
            RentalStatus status) {

        if (!"ADMIN".equals(currentRole)) {
            throw new AccessDeniedException(
                    "Only ADMIN can list all rentals"
            );
        }

        List<Rental> rentals = (status == null)
                ? rentalRepository.findAll()
                : rentalRepository.findByStatus(status);

        return rentals.stream()
                .map(RentalResponse::from)
                .toList();
    }

    // new — makes PAYMENT_FAILED reachable
    @Transactional
    public RentalResponse failRentalInternal(Long id) {

        Rental rental = findRental(id);

        if (rental.getStatus() != RentalStatus.PAYMENT_PENDING) {
            throw new InvalidRentalException(
                    "Only PAYMENT_PENDING rentals can be marked payment failed"
            );
        }

        rental.setStatus(RentalStatus.PAYMENT_FAILED);

        return RentalResponse.from(
                rentalRepository.save(rental)
        );
    }
}