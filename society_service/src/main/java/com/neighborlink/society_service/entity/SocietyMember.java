package com.neighborlink.society_service.entity;

import com.neighborlink.society_service.entity.SocietyMemberRole;
import com.neighborlink.society_service.entity.SocietyMemberId;
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
    @Column(name = "society_id")
    private Long societyId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocietyMemberRole role;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;
}
