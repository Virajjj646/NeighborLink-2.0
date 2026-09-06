package com.neighborlink.rental_service.repository;

import com.neighborlink.rental_service.entity.Rental;
import com.neighborlink.rental_service.entity.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RentalRepository extends JpaRepository<Rental, Long> {

    List<Rental> findByRenterId(String renterId);

    List<Rental> findByListingId(Long listingId);

    List<Rental> findByStatus(RentalStatus status);

    List<Rental> findByListingIdAndStatus(
            Long listingId,
            RentalStatus status
    );

    boolean existsByListingIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long listingId,
            List<RentalStatus> statuses,
            LocalDate endDate,
            LocalDate startDate
    );

    List<Rental> findByListingIdAndStatusIn(
            Long listingId,
            List<RentalStatus> statuses
    );
}