
package com.neighborlink.listing_service.repository;

import com.neighborlink.listing_service.entity.Listing;
import com.neighborlink.listing_service.entity.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {

    List<Listing> findByOwnerId(String ownerId);

    List<Listing> findBySocietyId(Long societyId);

    List<Listing> findByCategoryIgnoreCase(String category);

    List<Listing> findByStatus(ListingStatus status);

    List<Listing> findBySocietyIdAndStatus(
            Long societyId,
            ListingStatus status
    );

    List<Listing> findByCategoryIgnoreCaseAndStatus(
            String category,
            ListingStatus status
    );

    List<Listing> findBySocietyIdAndCategoryIgnoreCaseAndStatus(
            Long societyId,
            String category,
            ListingStatus status
    );
}