package org.mvnsearch.http.protocol;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.mvnsearch.http.model.HttpCookie;
import reactor.netty.http.client.HttpClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HttpBaseExecutor {
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
}
