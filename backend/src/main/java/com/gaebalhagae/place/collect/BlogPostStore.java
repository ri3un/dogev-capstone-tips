package com.gaebalhagae.place.collect;

import java.util.List;

/**
 * 수집한 블로그 글을 저장하는 곳. 지금은 JSON 파일, DB를 만든 뒤에는 JPA 구현체로 바꿉니다.
 * 수집 로직(BlogCollectService)은 이 인터페이스에만 의존합니다.
 */
public interface BlogPostStore {

    /** 장소의 기존 글 뒤에 새 글을 덧붙여 저장합니다. */
    void saveAll(String placeName, List<BlogPost> posts);

    List<BlogPost> findByPlace(String placeName);

    /** 이미 수집한 글인지 확인해 재수집 때 새 글만 남깁니다. */
    boolean existsByUrl(String placeName, String url);
}
