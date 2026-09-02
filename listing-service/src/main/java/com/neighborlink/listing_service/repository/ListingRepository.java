
package com.neighborlink.listing_service.repository;

import com.neighborlink.listing_service.entity.Listing;
import com.neighborlink.listing_service.entity.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingRepository
        extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {

    List<Listing> findByOwnerId(String ownerId);

    @Query("SELECT DISTINCT l.category FROM Listing l ORDER BY l.category")
    List<String> findDistinctCategories();

}