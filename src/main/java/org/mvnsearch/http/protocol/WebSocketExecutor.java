package org.mvnsearch.http.protocol;

import org.apache.commons.io.IOUtils;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpHeader;
import org.mvnsearch.http.model.HttpRequest;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.WebsocketClientSpec;

import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class WebSocketExecutor extends HttpBaseExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(WebSocketExecutor.class);

    public List<byte[]> execute(HttpRequest httpRequest) {
        byte[] requestBody = httpRequest.getBodyBytes();
        MessageStore messageStore = new MessageStore();
        if (requestBody != null && requestBody.length > 0) {
            String textBody = new String(requestBody, StandardCharsets.UTF_8);
            int sendMsgSequence = 0;
            if (textBody.startsWith("===")) { // multi massages
                if (textBody.startsWith("=== wait-for-server")) {
                    sendMsgSequence = 1;
                }
                try {
                    List<String> msgLines = new ArrayList<>();
                    for (String line : IOUtils.readLines(new StringReader(textBody))) {
                        if (line.startsWith("===")) {
                            if (!msgLines.isEmpty()) {
                                messageStore.addMessage(sendMsgSequence, String.join("\n", msgLines));
                                msgLines.clear();
                            }
                            if (line.contains("wait-for-server")) {
                                sendMsgSequence++;
                            }
                        } else {
                            msgLines.add(line);
                        }
                    }
                    if (!msgLines.isEmpty()) {
                        messageStore.addMessage(sendMsgSequence, String.join("\n", msgLines));
                    }
                } catch (Exception ignore) {

                }
            } else { // single message
                messageStore.addMessage(sendMsgSequence, textBody);
            }
        }
        final URI requestUri = httpRequest.getRequestTarget().getUri();
        //construct http client
        HttpClient client = httpClient().headers(httpHeaders -> {
            for (HttpHeader header : httpRequest.getHeaders()) {
                httpHeaders.add(header.getName(), header.getValue());
            }
        });
        System.out.println("WEBSOCKET " + requestUri);
        System.out.println();
        return httpWebSocket(client, requestUri, httpRequest, messageStore);
    }


    @SuppressWarnings("CallingSubscribeInNonBlockingScope")
    public List<byte[]> httpWebSocket(HttpClient httpClient, URI requestUri, HttpRequest httpRequest, MessageStore messageStore) {
        return httpClient
                .websocket(WebsocketClientSpec.builder().build())
                .uri(requestUri)
                .handle((inbound, outbound) -> Flux.<byte[]>create(fluxSink -> {
                    AtomicInteger receivedMsgCounter = new AtomicInteger(1);
                    inbound.receive().asString(StandardCharsets.UTF_8).handle((responseText, sink) -> {
                        try {
                            final int msgSequence = receivedMsgCounter.get();
                            if (messageStore.containsKey(msgSequence)) {
                                final Flux<String> messages = Flux.fromIterable(messageStore.get(msgSequence))
                                        .doOnNext(sentMsg -> {
                                            System.out.println("sent: \n" + sentMsg + "\n");
                                        });
                                outbound.sendString(messages, StandardCharsets.UTF_8).then().subscribe();
                            }
                            receivedMsgCounter.incrementAndGet();
                            System.out.println("received:");
                            System.out.println(prettyJsonFormat(responseText));
                            System.out.println();
                            // complete
                            /*outbound.sendClose().subscribe();
                            sink.complete();
                            fluxSink.complete();
                            */
                        } catch (Exception e) {
                            sink.error(e);
                            fluxSink.error(e);
                        }
                    }).subscribe();
                    if (messageStore.containsKey(0)) {
                        final Flux<String> messages = Flux.fromIterable(messageStore.get(0)).doOnNext(sentMsg -> {
                            System.out.println("sent: \n" + sentMsg + "\n");
                        });
                        outbound.sendString(messages, StandardCharsets.UTF_8).then().subscribe();
                    } else {
                        outbound.then().subscribe();
                    }

                })).buffer().blockLast();
    }

}
