package com.neighborlink.user_service.service;

import com.neighborlink.user_service.dto.UserProfileRequest;
import com.neighborlink.user_service.dto.UserProfileResponse;
import com.neighborlink.user_service.entity.UserProfile;
import com.neighborlink.user_service.repository.UserProfileRepository;
import org.springframework.http.HttpStatus;
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

    public UserProfileResponse getProfile(String userId) {

        UserProfile profile = userProfileRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User profile not found"
                        )
                );

        return toResponse(profile);
    }

    public UserProfileResponse updateProfile(
            String userId,
            UserProfileRequest request
    ) {

        UserProfile profile = userProfileRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
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