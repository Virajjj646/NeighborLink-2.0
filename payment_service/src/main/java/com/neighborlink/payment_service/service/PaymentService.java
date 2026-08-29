package com.neighborlink.payment_service.service;

import com.neighborlink.payment_service.dto.InternalPaymentRequest;
import com.neighborlink.payment_service.dto.PaymentRequest;
import com.neighborlink.payment_service.dto.PaymentResponse;
import com.neighborlink.payment_service.entity.Payment;
import com.neighborlink.payment_service.entity.PaymentStatus;
import com.neighborlink.payment_service.exception.PaymentException;
import com.neighborlink.payment_service.exception.PaymentNotFoundException;
import com.neighborlink.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RentalClient rentalClient;

    @Transactional
    public PaymentResponse createPayment(
            PaymentRequest request,
            String currentUserId) {

        Payment existingPayment =
                paymentRepository
                        .findByIdempotencyKey(
                                request.getIdempotencyKey()
                        )
                        .orElse(null);

        if (existingPayment != null) {

            if (!existingPayment.getUserId()
                    .equals(currentUserId)) {

                throw new AccessDeniedException(
                        "Idempotency key belongs to another user"
                );
            }

            return PaymentResponse.from(
                    existingPayment
            );
        }

        /*
         * Temporary amount.
         *
         * Normal client-created payments should eventually
         * be created through Rental Service.
         *
         * Payment must never trust an amount supplied
         * directly by the frontend.
         */
        BigDecimal amount = BigDecimal.ZERO;

        Payment payment = Payment.builder()
                .rentalId(request.getRentalId())
                .userId(currentUserId)
                .amount(amount)
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

        payment.setStatus(
                PaymentStatus.FAILED
        );

        return PaymentResponse.from(
                paymentRepository.save(payment)
        );
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
}