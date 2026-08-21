package com.neighborlink.society_service.entity;

import lombok.*;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SocietyMemberId implements Serializable {

    private Long societyId;
    private Long userId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SocietyMemberId that = (SocietyMemberId) o;
        return Objects.equals(societyId, that.societyId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(societyId, userId);
    }
}
