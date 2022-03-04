///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 17
//DEPS org.springframework:spring-webflux:5.3.16

package http;

import org.springframework.web.reactive.function.client.WebClient;

public class HelloWebClient {
    public static void main(String... args) throws Exception {
        doHttp();
    }

    /**
     * For more please visit https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-client
     */
    public static void doHttp() {
        WebClient client = WebClient.create();
        String result = client.get()
                .uri("https://httpbin.org/ip")
                .retrieve()
                .bodyToMono(String.class)
                .block();
        System.out.println(result);
    }
}
