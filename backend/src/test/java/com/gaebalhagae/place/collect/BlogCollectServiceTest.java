package com.gaebalhagae.place.collect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BlogCollectServiceTest {

    @TempDir
    Path tempDir;

    private final NaverBlogClient client = mock(NaverBlogClient.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-30T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    private BlogPostStore store;
    private BlogCollectService service;

    @BeforeEach
    void setUp() {
        ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());
        store = new JsonFileBlogPostStore(om, tempDir.toString());
        service = new BlogCollectService(client, store, clock);
    }

    private static NaverBlogClient.Item item(String link, String desc, String date) {
        return new NaverBlogClient.Item("<b>카페</b> 후기", link, desc, "블로거", date);
    }

    @Test
    void 일년_지난_글은_빼고_최근_글만_저장한다() {
        when(client.search(eq("카페A 애견동반"), anyInt())).thenReturn(List.of(
                item("u1", "강아지랑 다녀왔어요.", "20261001"),
                item("u2", "강아지랑 다녀왔어요.", "20240101")));

        var result = service.collect("카페A");

        assertThat(result.saved()).isEqualTo(1);
        assertThat(store.findByPlace("카페A")).extracting(BlogPost::url).containsExactly("u1");
    }

    @Test
    void 다시_수집하면_이미_본_글은_건너뛴다() {
        when(client.search(eq("카페A 애견동반"), anyInt())).thenReturn(List.of(item("u1", "강아지 동반", "20261001")));
        service.collect("카페A");

        var second = service.collect("카페A");

        assertThat(second.saved()).isZero();
        assertThat(second.total()).isEqualTo(1);
    }

    @Test
    void 협찬_글은_표시하고_반려견_문장만_남긴다() {
        var post = service.toPost(item("u1",
                "소정의 원고료를 받았어요. 대형견도 <b>실내</b> 동반 가능! 커피가 맛있어요.", "20261001"));

        assertThat(post.sponsored()).isTrue();
        assertThat(post.title()).isEqualTo("카페 후기");
        assertThat(post.petSentences()).containsExactly("대형견도 실내 동반 가능!");
    }
}
