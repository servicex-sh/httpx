package org.mvnsearch.http.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.Nullable;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
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
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(GraphqlExecutor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public List<byte[]> execute(HttpRequest httpRequest) {
        String contentType = httpRequest.getHeader("Content-Type");
        byte[] requestJsonBody = httpRequest.getBodyBytes();
        try {
            if (contentType != null) {
                Map<String, Object> jsonBody;
                if ("application/graphql".equals(contentType)) {  // convert graphql code into json object
                    jsonBody = Collections.singletonMap("query", new String(requestJsonBody, StandardCharsets.UTF_8));
                    requestJsonBody = objectMapper.writeValueAsBytes(jsonBody);
                }
            }
        } catch (Exception ignore) {
            log.error("HTX-102-500", new String(requestJsonBody, StandardCharsets.UTF_8));
            return Collections.emptyList();
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
            return httpWebSocket(client, requestUri, httpRequest, requestJsonBody);
        } else {  // http post
            return httpPost(client, requestUri, httpRequest, requestJsonBody);
        }
    }

    public List<byte[]> httpPost(HttpClient httpClient, URI requestUri, HttpRequest httpRequest, byte[] requestJsonBody) {
        HttpClient.ResponseReceiver<?> responseReceiver = httpClient.post().send(Mono.just(Unpooled.wrappedBuffer(requestJsonBody)));
        return request(responseReceiver, requestUri);
    }

    @SuppressWarnings("CallingSubscribeInNonBlockingScope")
    public List<byte[]> httpWebSocket(HttpClient httpClient, URI requestUri, HttpRequest httpRequest, byte[] requestJsonBody) {
        String id = UUID.randomUUID().toString();
        return httpClient
                .websocket(WebsocketClientSpec.builder().protocols("graphql-transport-ws").build())
                .uri(requestUri)
                .handle((inbound, outbound) -> Flux.<byte[]>create(fluxSink -> {
                    inbound.receive().asString().handle((responseJsonText, sink) -> {
                        try {
                            final Map<String, ?> response = objectMapper.readValue(responseJsonText, Map.class);
                            String type = (String) response.get("type");
                            if ("connection_ack".equals(type)) { //send query
                                byte[] queryBytes = graphqlWsMessage("subscribe", id, requestJsonBody);
                                outbound.send(Mono.just(Unpooled.wrappedBuffer(queryBytes))).then().subscribe();
                            } else if ("next".equals(type)) { //result received
                                final Object payload = response.get("payload");
                                final String jsonText = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
                                System.out.println(jsonText);
                                fluxSink.next(jsonText.getBytes(StandardCharsets.UTF_8));
                            } else if ("complete".equals(type)) {  // query completed
                                outbound.sendClose().subscribe();
                                sink.complete();
                                fluxSink.complete();
                            }
                        } catch (Exception e) {
                            sink.error(e);
                            fluxSink.error(e);
                        }
                    }).subscribe();
                    final byte[] connectionInitBytes = graphqlWsMessage("connection_init", null, null);
                    outbound.send(Flux.just(Unpooled.wrappedBuffer(connectionInitBytes))).then().subscribe();
                })).buffer().blockLast();
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

}
