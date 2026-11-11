package com.gaebalhagae.place.repository;

import com.gaebalhagae.place.domain.EditRequestStatus;
import com.gaebalhagae.place.domain.PlaceEditRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceEditRequestRepository extends JpaRepository<PlaceEditRequest, Long> {

    List<PlaceEditRequest> findByMemberIdOrderByIdDesc(Long memberId);

    List<PlaceEditRequest> findByStatusOrderByIdAsc(EditRequestStatus status);

    List<PlaceEditRequest> findAllByOrderByIdDesc();
}
