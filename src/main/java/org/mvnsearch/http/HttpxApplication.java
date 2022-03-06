package org.mvnsearch.http;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.rsocket.RSocketRequesterAutoConfiguration;
import org.springframework.boot.autoconfigure.rsocket.RSocketStrategiesAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.nativex.hint.NativeHint;
import org.springframework.nativex.hint.ResourceHint;

@SpringBootApplication(exclude = {RSocketStrategiesAutoConfiguration.class, RSocketRequesterAutoConfiguration.class})
@NativeHint(resources = {@ResourceHint(patterns = {"httpx/ErrorMessages.properties",})})
public class HttpxApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(HttpxApplication.class, args)));
    }

    @Bean
    public RSocketStrategies rSocketStrategies() {
        return RSocketStrategies.builder().build();
    }

}
