package com.gaebalhagae.place.collect;

import java.util.List;

/** 찾은 장소 목록 저장소. 지금은 JSON 파일, DB가 생기면 place 테이블 구현체로 바꿉니다. */
public interface CollectedPlaceStore {

    /** 전체 목록을 통째로 저장합니다. */
    void saveAll(List<CollectedPlace> places);

    List<CollectedPlace> findAll();
}
