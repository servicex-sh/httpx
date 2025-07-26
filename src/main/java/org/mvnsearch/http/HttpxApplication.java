package org.mvnsearch.http;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.rsocket.RSocketMessagingAutoConfiguration;
import org.springframework.boot.autoconfigure.rsocket.RSocketRequesterAutoConfiguration;
import org.springframework.boot.autoconfigure.rsocket.RSocketServerAutoConfiguration;
import org.springframework.boot.autoconfigure.rsocket.RSocketStrategiesAutoConfiguration;

@SpringBootApplication(
        exclude = {
                RSocketStrategiesAutoConfiguration.class,
                RSocketMessagingAutoConfiguration.class,
                RSocketRequesterAutoConfiguration.class,
                RSocketServerAutoConfiguration.class
        })
public class HttpxApplication implements RuntimeHintsRegistrar {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(HttpxApplication.class, args)));
    }

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("httpx/ErrorMessages.properties");
        hints.resources().registerPattern("http-client-execute.js");
        hints.resources().registerPattern("http-client-pre-stub.js");
    }
}
