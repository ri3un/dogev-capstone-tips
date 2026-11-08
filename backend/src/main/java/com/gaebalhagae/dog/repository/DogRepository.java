package com.gaebalhagae.dog.repository;

import com.gaebalhagae.dog.domain.Dog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DogRepository extends JpaRepository<Dog, Long> {

    List<Dog> findByFamilyGroupIdOrderByIdAsc(Long familyGroupId);

    /** 초대 수락 시 내 그룹의 반려견을 새 그룹으로 옮깁니다 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Dog d set d.familyGroupId = :to where d.familyGroupId = :from")
    int moveGroup(@Param("from") Long fromGroupId, @Param("to") Long toGroupId);
}
