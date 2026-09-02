package com.neighborlink.society_service.service;

import com.neighborlink.society_service.dto.AddMemberRequest;
import com.neighborlink.society_service.dto.MemberResponse;
import com.neighborlink.society_service.dto.SocietyRequest;
import com.neighborlink.society_service.dto.SocietyResponse;
import com.neighborlink.society_service.entity.Society;
import com.neighborlink.society_service.entity.SocietyMember;
import com.neighborlink.society_service.entity.SocietyMemberId;
import com.neighborlink.society_service.entity.SocietyMemberRole;
import com.neighborlink.society_service.exception.ResourceNotFoundException;
import com.neighborlink.society_service.exception.UnauthorizedException;
import com.neighborlink.society_service.repository.SocietyMemberRepository;
import com.neighborlink.society_service.repository.SocietyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SocietyService {

    private final SocietyRepository societyRepository;
    private final SocietyMemberRepository societyMemberRepository;

    @Transactional
    public SocietyResponse createSociety(
            SocietyRequest request,
            String currentUserId) {

        Society society = Society.builder()
                .name(request.getName().trim())
                .addressReference(request.getAddressReference())
                .build();

        Society savedSociety = societyRepository.save(society);

        SocietyMember creator = SocietyMember.builder()
                .societyId(savedSociety.getId())
                .userId(currentUserId)
                .role(SocietyMemberRole.ADMIN)
                .build();

        societyMemberRepository.save(creator);

        return SocietyResponse.from(savedSociety);
    }

    @Transactional(readOnly = true)
    public List<SocietyResponse> getAllSocieties() {

        return societyRepository.findAll()
                .stream()
                .map(SocietyResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SocietyResponse getSocietyById(Long id) {

        Society society = societyRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Society not found with id: " + id
                        ));

        return SocietyResponse.from(society);
    }

    @Transactional
    public SocietyResponse updateSociety(
            Long id,
            SocietyRequest request,
            String currentUserRole,
            String currentUserId) {

        Society society = societyRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Society not found with id: " + id
                        ));

        if (!isGlobalAdmin(currentUserRole)
                && !isSocietyAdmin(id, currentUserId)) {

            throw new UnauthorizedException(
                    "Only society admins can update society details"
            );
        }

        society.setName(request.getName().trim());
        society.setAddressReference(request.getAddressReference());

        return SocietyResponse.from(
                societyRepository.save(society)
        );
    }

    @Transactional
    public void deleteSociety(
            Long id,
            String currentUserRole) {

        if (!isGlobalAdmin(currentUserRole)) {
            throw new UnauthorizedException(
                    "Only system admins can delete societies"
            );
        }

        Society society = societyRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Society not found with id: " + id
                        ));

        societyMemberRepository.deleteBySocietyId(id);
        societyRepository.delete(society);
    }

    @Transactional
    public MemberResponse addMember(
            Long societyId,
            AddMemberRequest request,
            String currentUserRole,
            String currentUserId) {

        societyRepository.findById(societyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Society not found with id: " + societyId
                        ));

        if (!isGlobalAdmin(currentUserRole)
                && !isSocietyAdmin(societyId, currentUserId)) {

            throw new UnauthorizedException(
                    "Only society admins can add members"
            );
        }

        if (societyMemberRepository.existsBySocietyIdAndUserId(
                societyId,
                request.getUserId())) {

            throw new IllegalArgumentException(
                    "User is already a member of this society"
            );
        }

        SocietyMemberRole role = parseMemberRole(request.getRole());

        SocietyMember member = SocietyMember.builder()
                .societyId(societyId)
                .userId(request.getUserId())
                .role(role)
                .build();

        return MemberResponse.from(
                societyMemberRepository.save(member)
        );
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getMembers(Long societyId) {

        societyRepository.findById(societyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Society not found with id: " + societyId
                        ));

        return societyMemberRepository.findBySocietyId(societyId)
                .stream()
                .map(MemberResponse::from)
                .toList();
    }

    @Transactional
    public void removeMember(
            Long societyId,
            String targetUserId,
            String currentUserRole,
            String currentUserId) {

        societyRepository.findById(societyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Society not found with id: " + societyId
                        ));

        boolean removingSelf = currentUserId.equals(targetUserId);

        boolean authorizedAdmin =
                isGlobalAdmin(currentUserRole)
                        || isSocietyAdmin(societyId, currentUserId);

        if (!removingSelf && !authorizedAdmin) {
            throw new UnauthorizedException(
                    "You can only remove yourself or must be an admin"
            );
        }

        SocietyMemberId memberId =
                new SocietyMemberId(societyId, targetUserId);

        if (!societyMemberRepository.existsById(memberId)) {
            throw new ResourceNotFoundException(
                    "Member not found in this society"
            );
        }

        societyMemberRepository.deleteById(memberId);
    }

    @Transactional(readOnly = true)
    public List<SocietyResponse> getSocietiesByUser(String userId) {

        return societyMemberRepository.findByUserId(userId)
                .stream()
                .map(member ->
                        societyRepository.findById(member.getSocietyId()))
                .flatMap(java.util.Optional::stream)
                .map(SocietyResponse::from)
                .toList();
    }

    private boolean isSocietyAdmin(
            Long societyId,
            String userId) {

        return societyMemberRepository
                .existsBySocietyIdAndUserIdAndRole(
                        societyId,
                        userId,
                        SocietyMemberRole.ADMIN
                );
    }

    private boolean isGlobalAdmin(String role) {
        return "ADMIN".equals(role);
    }

    private SocietyMemberRole parseMemberRole(String role) {

        if (role == null || role.isBlank()) {
            return SocietyMemberRole.MEMBER;
        }

        try {
            return SocietyMemberRole.valueOf(
                    role.toUpperCase()
            );
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid society member role: " + role
            );
        }
    }

    @Transactional
    public MemberResponse joinSociety(Long societyId, String currentUserId) {

        societyRepository.findById(societyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Society not found with id: " + societyId
                        ));

        if (societyMemberRepository.existsBySocietyIdAndUserId(societyId, currentUserId)) {
            throw new IllegalArgumentException(
                    "You are already a member of this society"
            );
        }

        SocietyMember member = SocietyMember.builder()
                .societyId(societyId)
                .userId(currentUserId)
                .role(SocietyMemberRole.MEMBER)
                .build();

        return MemberResponse.from(
                societyMemberRepository.save(member)
        );
    }
}