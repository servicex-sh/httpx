package org.mvnsearch.http.protocol;


import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.HttpRequestParser;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Disabled
public class WebSocketExecutorTest {
    @Test
    public void testWebEcho() throws Exception {
        Map<String, Object> context = new HashMap<>();
        String httpFile = """
                ### websocket test
                WEBSOCKET wss://ws.postman-echo.com/raw
                Content-Type: application/json
                 
                { "name": "jackie" }
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new WebSocketExecutor().execute(request);
    }

    @Test
    public void testWebSocketInteractive() throws Exception {
        Map<String, Object> context = new HashMap<>();
        String httpFile = """
                ### WebSocket testing with sequence messages
                WEBSOCKET wss://ws.postman-echo.com/raw
                Content-Type: application/json
                                
                ===
                { "name": "Hello Server" }
                === wait-for-server
                {
                  "message": "msg-1"
                }
                === wait-for-server
                {
                  "message": "msg-2"
                }
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new WebSocketExecutor().execute(request);
    }

    @Test
    public void testWebSocketEcho() {
        HttpClient client = HttpClient.create();
        client.websocket()
                .uri("wss://ws.postman-echo.com/raw")
                // .uri("ws://localhost:8080/")
                .handle((inbound, outbound) -> {
                    inbound.receive()
                            .asString(StandardCharsets.UTF_8)
                            .subscribe(data -> System.out.println("Received: " + data));
                    return outbound.sendString(Flux.just("hello world!"), StandardCharsets.UTF_8)
                            .neverComplete();
                })
                .blockLast();
    }

}
