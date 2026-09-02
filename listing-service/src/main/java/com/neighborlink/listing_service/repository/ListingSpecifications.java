package com.neighborlink.listing_service.repository;

import com.neighborlink.listing_service.entity.Listing;
import com.neighborlink.listing_service.entity.ListingStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ListingSpecifications {

    private ListingSpecifications() {
    }

    public static Specification<Listing> withFilters(
            Long societyId,
            String category,
            ListingStatus status,
            String search) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (societyId != null) {
                predicates.add(cb.equal(root.get("societyId"), societyId));
            }

            if (category != null && !category.isBlank()) {
                predicates.add(
                        cb.equal(
                                cb.lower(root.get("category")),
                                category.trim().toLowerCase()
                        )
                );
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (search != null && !search.isBlank()) {

                String pattern = "%" + search.trim().toLowerCase() + "%";

                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("title")), pattern),
                                cb.like(cb.lower(root.get("description")), pattern)
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}