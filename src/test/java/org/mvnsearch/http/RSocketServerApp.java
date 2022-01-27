package org.mvnsearch.http;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketServer;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;


public class RSocketServerApp {
    public static void main(String[] args) {
        Hooks.onErrorDropped(throwable -> {
        });
        final CloseableChannel closeableChannel = RSocketServer.create()
                .acceptor((setup, sendingSocket) -> Mono.just(new RSocket() {
                    @Override
                    public @NotNull Mono<Payload> requestResponse(@NotNull Payload payload) {
                        System.out.println("received: " + payload.getDataUtf8());
                        return Mono.just(DefaultPayload.create("Hi"));
                    }

                    @Override
                    public @NotNull Flux<Payload> requestStream(@NotNull Payload payload) {
                        return Flux.just("first", "second", "third")
                                .map(s -> DefaultPayload.create(s, ""));
                    }
                }))
                .bind(TcpServerTransport.create("0.0.0.0", 42252))
                .block();
        System.out.println("RSocket Server started on 42252");
        closeableChannel.onClose().block();
    }
}
