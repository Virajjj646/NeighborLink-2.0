package com.neighborlink.payment_service.repository;

import com.neighborlink.payment_service.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(
            String idempotencyKey
    );

    Optional<Payment> findByRentalIdAndUserId(
            Long rentalId,
            String userId
    );

    List<Payment> findByUserId(
            String userId
    );

    List<Payment> findByRentalId(
            Long rentalId
    );

    Optional<Payment> findByProviderReference(String providerReference);
}