package com.gaebalhagae.record;

import static org.assertj.core.api.Assertions.assertThat;

import com.gaebalhagae.record.service.IntentClassifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** 통합 입력창 질문/기록 판별 예문 30개 (질문 15, 기록 15). 발표 자료의 "규칙 기반 의도 분류 정확도 n/30" 근거 */
class IntentClassifierTest {

    @ParameterizedTest(name = "{0} → 질문 {1}")
    @CsvSource(delimiter = '|', value = {
            "포도 먹어도 돼요?|true",
            "병원 가야 하나요|true",
            "산책 시켜도 될까|true",
            "이거 괜찮은가요|true",
            "토했는데 어떡해|true",
            "사료를 바꿔도 될까요|true",
            "하루에 몇 번 줘야 해요?|true",
            "설사하는데 굶겨야 하나|true",
            "귀를 자꾸 긁는데 왜 그럴까|true",
            "목욕은 얼마나 자주 시키나요|true",
            "기침을 하는데 감기인가요|true",
            "우유 줘도 되나요|true",
            "발바닥을 핥는데 괜찮을까요ㅠㅠ|true",
            "다리를 절어요 병원 가봐야 할까|true",
            "양파 먹었는데 어떻게 해야 해|true",
            "아침에 사료 50g 먹었어요|false",
            "산책 30분 했다|false",
            "설사함|false",
            "저녁에 간식으로 당근 줬어요|false",
            "오늘 몸무게 4.2kg|false",
            "오후에 구토 한 번 했어요.|false",
            "병원 가서 예방접종 맞았어요|false",
            "물을 평소보다 많이 마셨다|false",
            "변이 좀 묽었어요|false",
            "산책하다가 다리를 절었어요|false",
            "심장사상충 약 먹였어요!|false",
            "하루 종일 잠만 잤어요|false",
            "기침을 두 번 했어요|false",
            "귀 청소해줬어요~|false",
            "밥을 반만 먹고 남겼어요|false",
    })
    void 예문_30개를_질문과_기록으로_나눈다(String text, boolean question) {
        assertThat(IntentClassifier.isQuestion(text)).isEqualTo(question);
    }
}
