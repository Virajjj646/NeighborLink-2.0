package com.neighborlink.user_service.service;

import com.neighborlink.user_service.dto.CreateUserProfileRequest;
import com.neighborlink.user_service.dto.UserProfileRequest;
import com.neighborlink.user_service.dto.UserProfileResponse;
import com.neighborlink.user_service.entity.UserProfile;
import com.neighborlink.user_service.exception.UserServiceException;
import com.neighborlink.user_service.repository.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(
            UserProfileRepository userProfileRepository
    ) {
        this.userProfileRepository = userProfileRepository;
    }

    public UserProfileResponse getProfile(String userId, Authentication authentication) {
        checkAuthorization(userId,authentication);
        UserProfile profile = userProfileRepository
                .findById(userId)
                .orElseThrow(() ->
                        new UserServiceException(
                                HttpStatus.NOT_FOUND,
                                "User profile not found"
                        )
                );

        return toResponse(profile);
    }

    public UserProfileResponse updateProfile(
            String userId,
            UserProfileRequest request,
            Authentication authentication
    ) {
        checkAuthorization(userId,authentication);
        UserProfile profile = userProfileRepository
                .findById(userId)
                .orElseThrow(() ->
                        new UserServiceException(
                                HttpStatus.NOT_FOUND,
                                "User profile not found"
                        )
                );

        profile.setDisplayName(request.displayName());
        profile.setPhone(request.phone());
        profile.setProfileImage(request.profileImage());
        profile.setBio(request.bio());
        profile.setAddressReference(request.addressReference());

        UserProfile updatedProfile =
                userProfileRepository.save(profile);

        return toResponse(updatedProfile);
    }

    public UserProfileResponse createProfile(CreateUserProfileRequest request) {

        if (userProfileRepository.existsById(request.userId())) {
            throw new UserServiceException(
                    HttpStatus.CONFLICT,
                    "User profile already exists"
            );
        }

        UserProfile profile = UserProfile.builder()
                .userId(request.userId())
                .displayName(request.displayName())
                .phone(request.phone())
                .build();

        UserProfile savedProfile = userProfileRepository.save(profile);

        return toResponse(savedProfile);
    }

    private void checkAuthorization(String userReqId, Authentication authentication) {
        String authenticatedUserId = authentication.getName();
        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        if(!isAdmin&&!userReqId.equals(authenticatedUserId)){
            throw new UserServiceException(HttpStatus.FORBIDDEN,"You do not have permission to access this resource");
        }
    }

    private UserProfileResponse toResponse(UserProfile profile) {
        return new UserProfileResponse(
                profile.getUserId(),
                profile.getDisplayName(),
                profile.getPhone(),
                profile.getProfileImage(),
                profile.getBio(),
                profile.getAddressReference(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}