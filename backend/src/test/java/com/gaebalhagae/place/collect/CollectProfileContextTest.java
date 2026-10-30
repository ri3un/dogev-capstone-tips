package com.gaebalhagae.place.collect;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** collect 프로필은 MySQL 없이도 서버가 켜져야 합니다. */
@SpringBootTest(properties = "collect.dir=build/test-blog")
@ActiveProfiles("collect")
class CollectProfileContextTest {

    @Autowired
    private BlogPostStore store;

    @Test
    void DB_없이_실행되고_JSON_저장소가_선택된다() {
        assertThat(store).isInstanceOf(JsonFileBlogPostStore.class);
    }
}
