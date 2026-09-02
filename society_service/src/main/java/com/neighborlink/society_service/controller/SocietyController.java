package com.neighborlink.society_service.controller;

import com.neighborlink.society_service.dto.AddMemberRequest;
import com.neighborlink.society_service.dto.MemberResponse;
import com.neighborlink.society_service.dto.SocietyRequest;
import com.neighborlink.society_service.dto.SocietyResponse;
import com.neighborlink.society_service.service.SocietyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SocietyController {

    private final SocietyService societyService;

    @PostMapping
    public ResponseEntity<SocietyResponse> createSociety(
            @Valid @RequestBody SocietyRequest request,
            Authentication authentication) {

        String currentUserId = extractUserId(authentication);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        societyService.createSociety(
                                request,
                                currentUserId
                        )
                );
    }

    @GetMapping
    public ResponseEntity<List<SocietyResponse>> getAllSocieties() {

        return ResponseEntity.ok(
                societyService.getAllSocieties()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<SocietyResponse> getSocietyById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                societyService.getSocietyById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<SocietyResponse> updateSociety(
            @PathVariable Long id,
            @Valid @RequestBody SocietyRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                societyService.updateSociety(
                        id,
                        request,
                        extractRole(authentication),
                        extractUserId(authentication)
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSociety(
            @PathVariable Long id,
            Authentication authentication) {

        societyService.deleteSociety(
                id,
                extractRole(authentication)
        );

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<MemberResponse> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddMemberRequest request,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        societyService.addMember(
                                id,
                                request,
                                extractRole(authentication),
                                extractUserId(authentication)
                        )
                );
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<MemberResponse>> getMembers(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                societyService.getMembers(id)
        );
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @PathVariable String userId,
            Authentication authentication) {

        societyService.removeMember(
                id,
                userId,
                extractRole(authentication),
                extractUserId(authentication)
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SocietyResponse>> getUserSocieties(
            @PathVariable String userId,
            Authentication authentication) {

        String currentUserId = extractUserId(authentication);
        String currentRole = extractRole(authentication);

        if (!"ADMIN".equals(currentRole)
                && !currentUserId.equals(userId)) {

            throw new AccessDeniedException(
                    "You can only view your own societies"
            );
        }

        return ResponseEntity.ok(
                societyService.getSocietiesByUser(userId)
        );
    }

    private String extractUserId(Authentication authentication) {
        return authentication.getName();
    }

    private String extractRole(Authentication authentication) {

        return authentication.getAuthorities()
                .stream()
                .findFirst()
                .map(authority ->
                        authority.getAuthority()
                                .replace("ROLE_", ""))
                .orElse("USER");
    }

    public ResponseEntity<MemberResponse> joinSociety(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        societyService.joinSociety(id, extractUserId(authentication))
                );
    }
}