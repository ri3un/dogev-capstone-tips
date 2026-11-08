package com.gaebalhagae.family.repository;

import com.gaebalhagae.family.domain.FamilyGroup;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyGroupRepository extends JpaRepository<FamilyGroup, Long> {

    Optional<FamilyGroup> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);
}
