package com.neighborlink.user_service.repository;

import com.neighborlink.user_service.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile,String> {
}
