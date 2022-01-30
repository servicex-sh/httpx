package org.mvnsearch.http.protocol;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.mvnsearch.http.model.HttpCookie;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class HttpBaseExecutor implements BaseExecutor {
    protected HttpClient httpClient() {
        return HttpClient.create().secure(sslContextSpec -> {
            try {
                sslContextSpec.sslContext(SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build());
            } catch (Exception ignore) {

            }
        });
    }

    protected List<HttpCookie> cookies(String domain) {
        try {
            final Path cookieFile = Path.of(".idea/httpRequests/http-client.cookies");
            if (cookieFile.toFile().exists()) {
                final List<String> lines = Files.readAllLines(cookieFile);
                if (lines.size() > 1) {
                    List<HttpCookie> cookies = new ArrayList<>();
                    final long now = System.currentTimeMillis();
                    for (String line : lines.subList(1, lines.size())) {
                        final HttpCookie cookie = HttpCookie.valueOf(line);
                        if (cookie.getDomain().equalsIgnoreCase(domain) && cookie.getExpired().getTime() > now) {
                            cookies.add(cookie);
                        }
                    }
                    return cookies;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public List<byte[]> request(HttpClient.ResponseReceiver<?> responseReceiver, URI requestUri) {
        return responseReceiver
                .uri(requestUri)
                .response((response, byteBufFlux) -> {
                    System.out.println("Status: " + response.status());
                    final HttpHeaders responseHeaders = response.responseHeaders();
                    responseHeaders.forEach(header -> System.out.println(header.getKey() + ": " + header.getValue()));
                    String contentType = responseHeaders.get("Content-Type");
                    return byteBufFlux.asByteArray().doOnNext(bytes -> {
                        if (contentType != null && isPrintable(contentType)) {
                            if (contentType.contains("json")) {
                                System.out.print(prettyJsonFormat(new String(bytes, StandardCharsets.UTF_8)));
                            } else {
                                System.out.print(new String(bytes));
                            }
                        }
                    });
                }).buffer().blockLast();
    }
}
