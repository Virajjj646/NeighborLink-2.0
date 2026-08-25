package com.neighborlink.society_service.entity;

import com.neighborlink.society_service.entity.SocietyMemberRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "society_members")
@IdClass(SocietyMemberId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocietyMember {

    @Id
    @Column(name = "society_id",nullable = false)
    private Long societyId;

    @Id
    @Column(name = "user_id",nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocietyMemberRole role;

    @Column(name = "joined_at", updatable = false,nullable = false)
    @Builder.Default
    private LocalDateTime joinedAt =  LocalDateTime.now();
}
