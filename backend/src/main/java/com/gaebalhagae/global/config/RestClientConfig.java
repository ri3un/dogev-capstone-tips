package com.gaebalhagae.global.config;

import java.time.Duration;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/** 외부 API(LLM·카카오·네이버)가 응답하지 않을 때 스레드가 무한정 묶이지 않도록 시간 제한을 둡니다 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClientCustomizer timeoutCustomizer() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(60));
        return builder -> builder.requestFactory(factory);
    }
}
