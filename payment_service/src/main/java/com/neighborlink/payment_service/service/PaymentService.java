package com.neighborlink.payment_service.service;

import com.neighborlink.payment_service.dto.InternalPaymentRequest;
import com.neighborlink.payment_service.dto.PaymentResponse;
import com.neighborlink.payment_service.entity.Payment;
import com.neighborlink.payment_service.entity.PaymentStatus;
import com.neighborlink.payment_service.event.PaymentInitiatedEvent;
import com.neighborlink.payment_service.exception.PaymentException;
import com.neighborlink.payment_service.exception.PaymentNotFoundException;
import com.neighborlink.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;


import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RentalClient rentalClient;
    private final ApplicationEventPublisher eventPublisher;


    @Transactional(readOnly = true)
    public PaymentResponse getPayment(
            Long paymentId,
            String currentUserId,
            String currentRole) {

        Payment payment = findPayment(paymentId);

        checkOwnership(
                payment,
                currentUserId,
                currentRole
        );

        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPayments(
            String currentUserId) {

        return paymentRepository
                .findByUserId(currentUserId)
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Transactional
    public PaymentResponse markPending(
            Long paymentId,
            String currentRole) {

        requireAdmin(currentRole);

        Payment payment = findPayment(paymentId);

        if (payment.getStatus()
                != PaymentStatus.CREATED) {

            throw new PaymentException(
                    "Only CREATED payments can become PENDING"
            );
        }

        payment.setStatus(
                PaymentStatus.PENDING
        );

        return PaymentResponse.from(
                paymentRepository.save(payment)
        );
    }

    @Transactional
    public PaymentResponse markSuccess(
            Long paymentId,
            String providerTransactionId,
            String currentRole) {

        requireAdmin(currentRole);

        Payment payment = findPayment(paymentId);

        if (payment.getStatus()
                != PaymentStatus.PENDING) {

            throw new PaymentException(
                    "Only PENDING payments can become SUCCESS"
            );
        }

        payment.setStatus(
                PaymentStatus.SUCCESS
        );

        payment.setProviderTransactionId(
                providerTransactionId
        );

        Payment savedPayment =
                paymentRepository.save(payment);

        /*
         * Payment is now successfully completed.
         *
         * Notify Rental Service so that the rental can
         * move from PAYMENT_PENDING to CONFIRMED.
         */
        try {

            rentalClient.confirmRental(
                    savedPayment.getRentalId()
            );

        } catch (RestClientException ex) {

            throw new PaymentException(
                    "Payment succeeded but rental confirmation failed"
            );
        }

        return PaymentResponse.from(
                savedPayment
        );
    }

    @Transactional
    public PaymentResponse markFailed(
            Long paymentId,
            String currentRole) {

        requireAdmin(currentRole);

        Payment payment = findPayment(paymentId);

        if (payment.getStatus()
                != PaymentStatus.PENDING) {

            throw new PaymentException(
                    "Only PENDING payments can become FAILED"
            );
        }

        payment.setStatus(PaymentStatus.FAILED);

        Payment savedPayment = paymentRepository.save(payment);

        try {
            rentalClient.failRental(savedPayment.getRentalId());
        } catch (RestClientException ex) {
            throw new PaymentException(
                    "Payment marked failed but rental update failed"
            );
        }

        return PaymentResponse.from(savedPayment);
    }

    @Transactional
    public PaymentResponse refund(
            Long paymentId,
            String currentRole) {

        requireAdmin(currentRole);

        Payment payment = findPayment(paymentId);

        if (payment.getStatus()
                != PaymentStatus.SUCCESS) {

            throw new PaymentException(
                    "Only SUCCESS payments can be refunded"
            );
        }

        payment.setStatus(
                PaymentStatus.REFUNDED
        );

        Payment savedPayment =
                paymentRepository.save(payment);

        try {

            rentalClient.cancelRental(
                    savedPayment.getRentalId()
            );

        } catch (RestClientException ex) {

            throw new PaymentException(
                    "Payment refunded but rental cancellation failed"
            );
        }

        return PaymentResponse.from(savedPayment);
    }

    private Payment findPayment(Long paymentId) {

        return paymentRepository
                .findById(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment not found with id: "
                                        + paymentId
                        )
                );
    }

    private void checkOwnership(
            Payment payment,
            String currentUserId,
            String currentRole) {

        boolean isAdmin =
                "ADMIN".equals(currentRole);

        boolean isOwner =
                payment.getUserId()
                        .equals(currentUserId);

        if (!isAdmin && !isOwner) {

            throw new AccessDeniedException(
                    "You are not authorized to access this payment"
            );
        }
    }

    private void requireAdmin(
            String currentRole) {

        if (!"ADMIN".equals(currentRole)) {

            throw new AccessDeniedException(
                    "Only ADMIN can perform this operation"
            );
        }
    }

    @Transactional
    public PaymentResponse createInternalPayment(
            InternalPaymentRequest request) {

        Payment existingPayment =
                paymentRepository
                        .findByIdempotencyKey(
                                request.getIdempotencyKey()
                        )
                        .orElse(null);

        if (existingPayment != null) {
            return PaymentResponse.from(existingPayment);
        }

        Payment payment = Payment.builder()
                .rentalId(request.getRentalId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .status(PaymentStatus.CREATED)
                .idempotencyKey(
                        request.getIdempotencyKey()
                )
                .build();

        return PaymentResponse.from(
                paymentRepository.save(payment)
        );
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByRental(
            Long rentalId,
            String currentUserId,
            String currentRole) {

        boolean isAdmin = "ADMIN".equals(currentRole);

        return paymentRepository
                .findByRentalId(rentalId)
                .stream()
                .filter(payment -> isAdmin
                        || payment.getUserId().equals(currentUserId))
                .map(PaymentResponse::from)
                .toList();
    }

    @Transactional
    public PaymentResponse initiatePayment(
            Long paymentId,
            String currentUserId) {

        Payment payment = findPayment(paymentId);

        if (!payment.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException(
                    "You can only pay for your own rental"
            );
        }

        if (payment.getStatus() != PaymentStatus.CREATED
                && payment.getStatus() != PaymentStatus.FAILED) {

            throw new PaymentException(
                    "This payment cannot be started from its current state"
            );
        }

        payment.setStatus(PaymentStatus.PENDING);
        payment.setProviderReference(generateProviderReference());
        payment.setProviderTransactionId(null);

        Payment savedPayment = paymentRepository.save(payment);

        eventPublisher.publishEvent(
                new PaymentInitiatedEvent(savedPayment.getId())
        );

        return PaymentResponse.from(savedPayment);
    }

    /*
     * Single settlement path. Used by the simulated provider and by
     * the real provider callback. Never reachable from a user request.
     */
    @Transactional
    public PaymentResponse settlePayment(
            Long paymentId,
            PaymentStatus result,
            String providerTransactionId) {

        if (result != PaymentStatus.SUCCESS
                && result != PaymentStatus.FAILED) {

            throw new PaymentException(
                    "A provider result must be SUCCESS or FAILED"
            );
        }

        Payment payment = findPayment(paymentId);

        if (payment.getStatus() == result) {
            // Providers retry callbacks. Settling twice is not an error.
            return PaymentResponse.from(payment);
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentException(
                    "Only PENDING payments can be settled"
            );
        }

        payment.setStatus(result);
        payment.setProviderTransactionId(providerTransactionId);

        Payment savedPayment = paymentRepository.save(payment);

        try {

            if (result == PaymentStatus.SUCCESS) {
                rentalClient.confirmRental(savedPayment.getRentalId());
            } else {
                rentalClient.failRental(savedPayment.getRentalId());
            }

        } catch (RestClientException ex) {

            throw new PaymentException(
                    "Payment settled but rental update failed"
            );
        }

        return PaymentResponse.from(savedPayment);
    }

    @Transactional
    public PaymentResponse settleByProviderReference(
            String providerReference,
            PaymentStatus result,
            String providerTransactionId) {

        Payment payment = paymentRepository
                .findByProviderReference(providerReference)
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "No payment found for that provider reference"
                        ));

        return settlePayment(
                payment.getId(),
                result,
                providerTransactionId
        );
    }

    private String generateProviderReference() {

        return "NLP-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();
    }

    @Transactional
    public void refundForRental(Long rentalId) {

        paymentRepository.findByRentalId(rentalId)
                .stream()
                .filter(payment ->
                        payment.getStatus() == PaymentStatus.SUCCESS)
                .forEach(payment -> {

                    payment.setStatus(PaymentStatus.REFUNDED);
                    paymentRepository.save(payment);
                });
    }
}