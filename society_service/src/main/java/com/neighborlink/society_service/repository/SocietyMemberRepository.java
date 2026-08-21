package com.neighborlink.society_service.repository;

import com.neighborlink.society_service.entity.Society;
import com.neighborlink.society_service.entity.SocietyMember;
import com.neighborlink.society_service.entity.SocietyMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public interface SocietyMemberRepository extends JpaRepository<SocietyMember, SocietyMemberId> {
    List<SocietyMember> findBySocietyId(Long society);
    List<SocietyMember> findByUserId(Long userId);
    boolean existsBySocietyIdAndUserId(Long societyId, Long userId);
}
