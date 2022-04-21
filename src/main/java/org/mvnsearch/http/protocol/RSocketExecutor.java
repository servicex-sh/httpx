package org.mvnsearch.http.protocol;

import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketConnector;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import io.rsocket.util.DefaultPayload;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.RSocketRequest;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public class RSocketExecutor implements BaseExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(RSocketExecutor.class);

    public List<byte[]> execute(HttpRequest httpRequest) {
        RSocketRequest rsocketRequest = new RSocketRequest(httpRequest);
        final String requestType = rsocketRequest.getRequestType();
        final URI requestUri = httpRequest.getRequestTarget().getUri();
        System.out.println(requestType + " " + requestUri);
        System.out.println();
        return switch (requestType) {
            case "RSOCKET", "RPC" -> requestResponse(rsocketRequest);
            case "FNF" -> fireAndForget(rsocketRequest);
            case "STREAM" -> requestStream(rsocketRequest);
            case "METADATA_PUSH" -> metadataPush(rsocketRequest);
            case "GRAPHQLRS" -> Objects.equals(rsocketRequest.getGraphqlOperationName(), "subscription") ? requestStream(rsocketRequest) : requestResponse(rsocketRequest);
            default -> Collections.emptyList();
        };
    }

    private List<byte[]> requestResponse(RSocketRequest rsocketRequest) {
        var dataMimeType = rsocketRequest.getAcceptMimeType();
        RSocket clientRSocket = null;
        try {
            clientRSocket = createRSocket(rsocketRequest);
            var result = clientRSocket.requestResponse(rsocketRequest.createPayload()).block();
            String text = convertPayloadText(dataMimeType, result);
            System.out.println(text);
            runJsTest(rsocketRequest.getHttpRequest(), 200, Collections.emptyMap(), dataMimeType, text);
            return List.of(text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("HTX-105-408", e);
        } finally {
            if (clientRSocket != null) {
                clientRSocket.dispose();
            }
        }
        return Collections.emptyList();
    }

    private List<byte[]> fireAndForget(RSocketRequest rsocketRequest) {
        var dataMimeType = rsocketRequest.getAcceptMimeType();
        RSocket clientRSocket = null;
        try {
            clientRSocket = createRSocket(rsocketRequest);
            clientRSocket.fireAndForget(rsocketRequest.createPayload()).block();
            System.out.println("[RSocket] payload sent by FNF");
        } catch (Exception e) {
            log.error("HTX-105-408", e);
        } finally {
            if (clientRSocket != null) {
                clientRSocket.dispose();
            }
        }
        return Collections.emptyList();
    }

    private List<byte[]> metadataPush(RSocketRequest rsocketRequest) {
        RSocket clientRSocket = null;
        try {
            clientRSocket = createRSocket(rsocketRequest);
            var payload = DefaultPayload.create(Unpooled.EMPTY_BUFFER, Unpooled.wrappedBuffer(rsocketRequest.getBodyBytes()));
            clientRSocket.metadataPush(payload).block();
            System.out.println("[RSocket] payload sent by METADATA_PUSH");
        } catch (Exception e) {
            log.error("HTX-105-408", e);
        } finally {
            if (clientRSocket != null) {
                clientRSocket.dispose();
            }
        }
        return Collections.emptyList();
    }

    private List<byte[]> requestStream(RSocketRequest rsocketRequest) {
        var dataMimeType = rsocketRequest.getAcceptMimeType();
        RSocket clientRSocket = null;
        try {
            clientRSocket = createRSocket(rsocketRequest);
            return clientRSocket.requestStream(rsocketRequest.createPayload())
                    .doOnNext(payload -> {
                        String text = convertPayloadText(dataMimeType, payload);
                        System.out.println(text);
                    }).map(payload -> payload.getData().array())
                    .buffer()
                    .blockLast();
        } catch (Exception e) {
            log.error("HTX-105-408", e);
        } finally {
            if (clientRSocket != null) {
                clientRSocket.dispose();
            }
        }
        return Collections.emptyList();
    }


    private RSocket createRSocket(RSocketRequest rsocketRequest) {
        var rsocketURI = rsocketRequest.getUri();
        ClientTransport clientTransport;
        if (rsocketURI.getScheme().equalsIgnoreCase("tcp")) {
            clientTransport = TcpClientTransport.create(rsocketURI.getHost(), rsocketURI.getPort());
        } else {
            clientTransport = WebsocketClientTransport.create(rsocketRequest.getWebsocketRequestURI());
        }
        return RSocketConnector.create()
                .dataMimeType(rsocketRequest.getDataMimeType())
                .metadataMimeType(rsocketRequest.getMetadataMimeType())
                .setupPayload(rsocketRequest.setupPayload())
                .connect(clientTransport)
                .block();
    }

    private String convertPayloadText(String dataMimeType, Payload payload) {
        if (isPrintable(dataMimeType)) {
            final String dataUtf8 = payload.getDataUtf8();
            if (dataMimeType.contains("json") && (dataUtf8.startsWith("{") || dataUtf8.startsWith("["))) {
                return prettyJsonFormat(dataUtf8);
            }
            return dataUtf8;
        } else {
            return Base64.getEncoder().encodeToString(payload.getData().array());
        }
    }
}
