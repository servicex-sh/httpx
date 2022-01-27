package org.mvnsearch.http.protocol;

import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketConnector;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import io.rsocket.util.DefaultPayload;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.RSocketRequest;
import reactor.core.publisher.Hooks;

import java.net.URI;
import java.util.Base64;


public class RSocketExecutor {
    static {
        Hooks.onErrorDropped(throwable -> {

        });
    }

    public void execute(HttpRequest httpRequest) {
        RSocketRequest rsocketRequest = new RSocketRequest(httpRequest);
        final String requestType = rsocketRequest.getRequestType();
        final URI requestUri = httpRequest.getRequestTarget().getUri();
        System.out.println(requestType + " " + requestUri);
        System.out.println();
        switch (requestType) {
            case "RSOCKET", "RPC" -> requestResponse(rsocketRequest);
            case "FNF" -> fireAndForget(rsocketRequest);
            case "STREAM" -> requestStream(rsocketRequest);
            case "METADATA_PUSH" -> metadataPush(rsocketRequest);
        }
    }

    private void requestResponse(RSocketRequest rsocketRequest) {
        var dataMimeType = rsocketRequest.getAcceptMimeType();
        RSocket clientRSocket = null;
        try {
            clientRSocket = createRSocket(rsocketRequest);
            var result = clientRSocket.requestResponse(rsocketRequest.createPayload()).block();
            String text = convertPayloadText(dataMimeType, result);
            System.out.println(text);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (clientRSocket != null) {
                clientRSocket.dispose();
            }
        }
    }

    private void fireAndForget(RSocketRequest rsocketRequest) {
        var dataMimeType = rsocketRequest.getAcceptMimeType();
        RSocket clientRSocket = null;
        try {
            clientRSocket = createRSocket(rsocketRequest);
            clientRSocket.fireAndForget(rsocketRequest.createPayload()).block();
            System.out.println("[RSocket] payload sent by FNF");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (clientRSocket != null) {
                clientRSocket.dispose();
            }
        }
    }

    private void metadataPush(RSocketRequest rsocketRequest) {
        var dataMimeType = rsocketRequest.getAcceptMimeType();
        RSocket clientRSocket = null;
        try {
            clientRSocket = createRSocket(rsocketRequest);
            var payload = DefaultPayload.create(Unpooled.EMPTY_BUFFER, Unpooled.wrappedBuffer(rsocketRequest.getBodyBytes()));
            clientRSocket.metadataPush(payload).block();
            System.out.println("[RSocket] payload sent by METADATA_PUSH");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (clientRSocket != null) {
                clientRSocket.dispose();
            }
        }
    }

    private void requestStream(RSocketRequest rsocketRequest) {
        var dataMimeType = rsocketRequest.getAcceptMimeType();
        RSocket clientRSocket = null;
        try {
            clientRSocket = createRSocket(rsocketRequest);
            clientRSocket.requestStream(rsocketRequest.createPayload())
                    .doOnNext(payload -> {
                        String text = convertPayloadText(dataMimeType, payload);
                        System.out.println(text);
                    }).blockLast();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (clientRSocket != null) {
                clientRSocket.dispose();
            }
        }
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
        if (dataMimeType.contains("text") || dataMimeType.contains("json") || dataMimeType.contains("xml")) {
            return payload.getDataUtf8();
        } else {
            return Base64.getEncoder().encodeToString(payload.getData().array());
        }
    }
}
