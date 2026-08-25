package com.neighborlink.society_service.repository;

import com.neighborlink.society_service.entity.SocietyMember;
import com.neighborlink.society_service.entity.SocietyMemberId;
import com.neighborlink.society_service.entity.SocietyMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SocietyMemberRepository extends JpaRepository<SocietyMember, SocietyMemberId> {

    List<SocietyMember> findBySocietyId(Long societyId);

    List<SocietyMember> findByUserId(String userId);

    boolean existsBySocietyIdAndUserId(Long societyId, String userId);

    Optional<SocietyMember> findBySocietyIdAndUserId(Long societyId, String userId);

    boolean existsBySocietyIdAndUserIdAndRole (Long societyId,String userId, SocietyMemberRole role);

    void deleteBySocietyId(Long societyId);
}
