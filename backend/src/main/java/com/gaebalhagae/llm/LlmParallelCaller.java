package com.gaebalhagae.llm;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AI 상담용: 무료 LLM 3개에 같은 질문을 동시에 보내고 모델별 결과를 모읍니다 (결정 2·14·15).
 * 모델마다 시간 제한(기본 15초), 형식이 틀리면 같은 모델에 1회 재요청, 또 틀리면 INVALID.
 */
@Slf4j
@Component
public class LlmParallelCaller {

    public enum Status {
        OK, TIMEOUT, INVALID, ERROR
    }

    public record Answer<T>(String model, Status status, T value, long latencyMs) {
    }

    private final List<LlmClient> clients;
    private final Duration timeout;
    /** 스레드 수 고정: 동시에 상담 몇 건이 몰려도 스레드가 끝없이 늘지 않습니다 */
    private final ExecutorService executor;

    @Autowired
    public LlmParallelCaller(GeminiClient gemini, GroqClient groq, OpenRouterClient openRouter,
                             @Value("${llm.consult-timeout-seconds:15}") long timeoutSeconds) {
        this(List.of(gemini, groq, openRouter), Duration.ofSeconds(timeoutSeconds));
    }

    LlmParallelCaller(List<LlmClient> clients, Duration timeout) {
        this.clients = clients;
        this.timeout = timeout;
        this.executor = Executors.newFixedThreadPool(clients.size() * 3, r -> {
            Thread t = new Thread(r, "llm-parallel");
            t.setDaemon(true);
            return t;
        });
    }

    public int size() {
        return clients.size();
    }

    /** invokeAll 은 시간 안에 못 끝난 작업을 취소(interrupt)해서, 버린 결과를 계속 기다리지 않습니다 */
    public <T> List<Answer<T>> call(String system, String user, Function<String, T> parser) {
        List<Callable<Answer<T>>> tasks = clients.stream()
                .<Callable<Answer<T>>>map(c -> () -> callOne(c, system, user, parser))
                .toList();
        List<Future<Answer<T>>> futures;
        try {
            futures = executor.invokeAll(tasks, timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("상담 호출이 중단되었습니다", e);
        }
        List<Answer<T>> answers = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            Future<Answer<T>> f = futures.get(i);
            try {
                answers.add(f.isCancelled() ? timedOut(clients.get(i)) : f.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                answers.add(timedOut(clients.get(i)));
            } catch (ExecutionException | CancellationException e) {
                answers.add(timedOut(clients.get(i)));
            }
        }
        return answers;
    }

    private <T> Answer<T> timedOut(LlmClient c) {
        return new Answer<>(c.name(), Status.TIMEOUT, null, timeout.toMillis());
    }

    private <T> Answer<T> callOne(LlmClient client, String system, String user, Function<String, T> parser) {
        long start = System.currentTimeMillis();
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                T value = parser.apply(client.completeJson(system, user));
                return new Answer<>(client.name(), Status.OK, value, System.currentTimeMillis() - start);
            } catch (LlmFormatException e) {
                log.warn("상담 형식 오류 model={} attempt={}", client.name(), attempt);
            } catch (RuntimeException e) {
                log.warn("상담 호출 실패 model={} reason={}", client.name(), e.getMessage());
                return new Answer<>(client.name(), Status.ERROR, null, System.currentTimeMillis() - start);
            }
        }
        return new Answer<>(client.name(), Status.INVALID, null, System.currentTimeMillis() - start);
    }
}
