package org.mvnsearch.http.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.Nullable;
import org.mvnsearch.http.model.HttpCookie;
import org.mvnsearch.http.model.HttpHeader;
import org.mvnsearch.http.model.HttpRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.WebsocketClientSpec;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GraphqlExecutor extends HttpBaseExecutor {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public void execute(HttpRequest httpRequest) {
        String contentType = httpRequest.getHeader("Content-Type");
        byte[] requestJsonBody;
        try {
            requestJsonBody = httpRequest.getBodyBytes();
            if (contentType != null) {
                Map<String, Object> jsonBody;
                if ("application/graphql".equals(contentType)) {  // convert graphql code into json object
                    jsonBody = Collections.singletonMap("query", new String(requestJsonBody, StandardCharsets.UTF_8));
                    requestJsonBody = objectMapper.writeValueAsBytes(jsonBody);
                }
            }
        } catch (Exception ignore) {
            System.err.println("Failed to parse json body");
            return;
        }
        final URI requestUri = httpRequest.getRequestTarget().getUri();
        //construct http client
        HttpClient client = httpClient().headers(httpHeaders -> {
            if (httpRequest.getHeaders() != null) {
                for (HttpHeader header : httpRequest.getHeaders()) {
                    if (header.getName().equalsIgnoreCase("Content-Type") && !header.getValue().contains("json")) {
                        httpHeaders.add(header.getName(), "application/json"); // convert application/graphql to application/json
                    } else {
                        httpHeaders.add(header.getName(), header.getValue());
                    }
                }
            }
        });
        for (HttpCookie cookie : cookies(requestUri.getHost())) {
            client.cookie(cookie.toNettyCookie());
        }
        System.out.println("GRAPHQL " + requestUri);
        System.out.println();
        if (requestUri.getScheme().startsWith("ws")) {  //websocket with graphql-ws spec
            httpWebSocket(client, requestUri, httpRequest, requestJsonBody);
        } else {  // http post
            httpPost(client, requestUri, httpRequest, requestJsonBody);
        }
    }

    public void httpPost(HttpClient httpClient, URI requestUri, HttpRequest httpRequest, byte[] requestJsonBody) {
        HttpClient.ResponseReceiver<?> responseReceiver = httpClient.post().send(Mono.just(Unpooled.wrappedBuffer(requestJsonBody)));
        String body = responseReceiver
                .uri(requestUri)
                .response((response, byteBufFlux) -> {
                    System.out.println("Status: " + response.status());
                    response.responseHeaders().forEach(header -> System.out.println(header.getKey() + ": " + header.getValue()));
                    return byteBufFlux.asString();
                }).map(this::prettyJsonFormat)
                .blockLast();
        System.out.println(body);
    }

    public void httpWebSocket(HttpClient httpClient, URI requestUri, HttpRequest httpRequest, byte[] requestJsonBody) {
        String id = UUID.randomUUID().toString();
        httpClient
                .websocket(WebsocketClientSpec.builder().protocols("graphql-transport-ws").build())
                .uri(requestUri)
                .handle((inbound, outbound) -> {
                    final byte[] connectionInitBytes = graphqlWsMessage("connection_init", null, null);
                    return outbound.send(Flux.just(Unpooled.wrappedBuffer(connectionInitBytes)))
                            .then(inbound.receive().asString().handle((responseJsonText, sink) -> {
                                        try {
                                            final Map<String, ?> response = objectMapper.readValue(responseJsonText, Map.class);
                                            String type = (String) response.get("type");
                                            if ("connection_ack".equals(type)) { //send query
                                                byte[] queryBytes = graphqlWsMessage("subscribe", id, requestJsonBody);
                                                outbound.send(Mono.just(Unpooled.wrappedBuffer(queryBytes))).then().subscribe();
                                            } else if ("next".equals(type)) { //result received
                                                final Object payload = response.get("payload");
                                                System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload));
                                            } else if ("complete".equals(type)) {  // query completed
                                                outbound.sendClose().subscribe();
                                                sink.complete();
                                            }
                                        } catch (Exception e) {
                                            sink.error(e);
                                        }
                                    })
                                    .then());
                }).blockLast();
    }

    private byte[] graphqlWsMessage(String type, @Nullable String id, byte[] payload) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", type);
        try {
            if (id != null) {
                msg.put("id", id);
            }
            if (payload != null) {
                msg.put("payload", objectMapper.readValue(payload, Map.class));
            }
            return objectMapper.writeValueAsBytes(msg);
        } catch (Exception e) {
            return new byte[]{};
        }
    }

    private String prettyJsonFormat(String jsonText) {
        try {
            if (jsonText.startsWith("{")) {
                final Map<?, ?> jsonObject = objectMapper.readValue(jsonText, Map.class);
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
            } else if (jsonText.startsWith("[")) {
                final List<?> jsonArray = objectMapper.readValue(jsonText, List.class);
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonArray);
            } else {
                return jsonText;
            }
        } catch (Exception e) {
            return jsonText;
        }
    }
}
