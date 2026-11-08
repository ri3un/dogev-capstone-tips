package com.gaebalhagae.family.repository;

import com.gaebalhagae.family.domain.FamilyMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {

    Optional<FamilyMember> findByMemberId(Long memberId);

    List<FamilyMember> findByFamilyGroupIdOrderByJoinedAtAsc(Long familyGroupId);

    long countByFamilyGroupId(Long familyGroupId);
}
