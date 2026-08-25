package com.neighborlink.society_service.entity;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SocietyMemberId implements Serializable {

    private Long societyId;
    private String userId;
}