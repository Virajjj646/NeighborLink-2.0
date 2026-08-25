package com.neighborlink.society_service.dto;

import com.neighborlink.society_service.entity.SocietyMember;
import com.neighborlink.society_service.entity.SocietyMemberRole;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResponse {

    private Long societyId;
    private String userId;
    private SocietyMemberRole role;
    private LocalDateTime joinedAt;

    public static MemberResponse from(SocietyMember member) {
        return MemberResponse.builder()
                .societyId(member.getSocietyId())
                .userId(member.getUserId())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
