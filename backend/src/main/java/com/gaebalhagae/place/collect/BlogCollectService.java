package com.gaebalhagae.place.collect;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * 장소 하나의 블로그 글을 검색해 걸러 낸 뒤 저장합니다. (기능별_구현방법 5-3 전처리 1~4단계)
 * LLM 요약(5단계)은 저장된 글을 읽어 처리하는 다음 단계에서 붙입니다.
 */
@Service
@RequiredArgsConstructor
public class BlogCollectService {

    static final int MAX_POSTS = 30;
    private static final List<String> SPONSORED_WORDS = List.of("협찬", "원고료", "제공받아", "체험단", "소정의");
    private static final List<String> PET_WORDS = List.of(
            "강아지", "애견", "반려견", "댕댕", "멍멍", "대형견", "중형견", "소형견", "케이지", "이동장",
            "유모차", "실내", "테라스", "목줄", "매너벨트", "기저귀", "펫", "동반");
    private static final DateTimeFormatter POST_DATE = DateTimeFormatter.BASIC_ISO_DATE; // 20261030

    private final NaverBlogClient naverBlogClient;
    private final BlogPostStore blogPostStore;
    private final Clock clock;

    public record CollectResult(String placeName, int fetched, int saved, int sponsored, int total) {
    }

    public CollectResult collect(String placeName) {
        LocalDate oneYearAgo = LocalDate.now(clock).minusYears(1);
        List<NaverBlogClient.Item> items = naverBlogClient.search(placeName + " 애견동반", 100);

        List<BlogPost> newPosts = items.stream()
                .map(this::toPost)
                .filter(p -> p.postDate() != null && !p.postDate().isBefore(oneYearAgo))   // 최근 1년
                .filter(p -> !blogPostStore.existsByUrl(placeName, p.url()))                 // 이미 본 글 제외
                .sorted(Comparator.comparing(BlogPost::postDate).reversed())
                .limit(MAX_POSTS)
                .toList();

        if (!newPosts.isEmpty()) {
            blogPostStore.saveAll(placeName, newPosts);
        }
        int sponsored = (int) newPosts.stream().filter(BlogPost::sponsored).count();
        return new CollectResult(placeName, items.size(), newPosts.size(), sponsored,
                blogPostStore.findByPlace(placeName).size());
    }

    public List<BlogPost> findByPlace(String placeName) {
        return blogPostStore.findByPlace(placeName);
    }

    BlogPost toPost(NaverBlogClient.Item item) {
        String title = clean(item.title());
        String description = clean(item.description());
        String text = title + " " + description;
        boolean sponsored = SPONSORED_WORDS.stream().anyMatch(text::contains);
        return new BlogPost(title, description, item.link(), item.bloggername(),
                parseDate(item.postdate()), sponsored, petSentences(description));
    }

    // <b> 태그 제거 + &quot; 같은 HTML 문자 복원
    static String clean(String s) {
        return s == null ? "" : HtmlUtils.htmlUnescape(s.replaceAll("<[^>]+>", "")).trim();
    }

    // 문장 끝(. ! ? …)에서 나누고 반려견 키워드가 있는 문장만 남김
    static List<String> petSentences(String description) {
        return Arrays.stream(description.split("(?<=[.!?…])\\s+|\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(s -> PET_WORDS.stream().anyMatch(s::contains))
                .toList();
    }

    private static LocalDate parseDate(String postdate) {
        try {
            return postdate == null ? null : LocalDate.parse(postdate, POST_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
