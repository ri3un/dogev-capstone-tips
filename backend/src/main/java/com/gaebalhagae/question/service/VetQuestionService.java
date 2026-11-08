package com.gaebalhagae.question.service;

import com.gaebalhagae.dog.domain.DogDeletedEvent;
import com.gaebalhagae.family.service.FamilyAccessService;
import com.gaebalhagae.global.response.BusinessException;
import com.gaebalhagae.global.response.ErrorCode;
import com.gaebalhagae.question.domain.QuestionSource;
import com.gaebalhagae.question.domain.VetQuestion;
import com.gaebalhagae.question.dto.VetQuestionResponse;
import com.gaebalhagae.question.repository.VetQuestionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class VetQuestionService {

    private final VetQuestionRepository repository;
    private final FamilyAccessService access;

    @Transactional(readOnly = true)
    public List<VetQuestionResponse> list(Long memberId, Long dogId, Boolean resolved) {
        access.checkDog(memberId, dogId);
        List<VetQuestion> list = resolved == null
                ? repository.findByDogIdOrderByIdDesc(dogId)
                : repository.findByDogIdAndResolvedOrderByIdDesc(dogId, resolved);
        return list.stream().map(VetQuestionResponse::from).toList();
    }

    public VetQuestionResponse create(Long memberId, Long dogId, String content, QuestionSource source) {
        access.checkDog(memberId, dogId);
        return VetQuestionResponse.from(repository.save(new VetQuestion(dogId, memberId, content,
                source == null ? QuestionSource.MEMO : source)));
    }

    public VetQuestionResponse edit(Long memberId, Long questionId, String content, Boolean resolved) {
        VetQuestion q = find(memberId, questionId);
        q.edit(content, resolved);
        return VetQuestionResponse.from(q);
    }

    public void delete(Long memberId, Long questionId) {
        repository.delete(find(memberId, questionId));
    }

    @EventListener
    public void onDogDeleted(DogDeletedEvent event) {
        repository.deleteByDogId(event.dogId());
    }

    /** 남의 반려견 질문은 존재 여부를 드러내지 않도록 404 */
    private VetQuestion find(Long memberId, Long questionId) {
        VetQuestion q = repository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
        try {
            access.checkDog(memberId, q.getDogId());
        } catch (BusinessException e) {
            throw new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
        }
        return q;
    }
}
