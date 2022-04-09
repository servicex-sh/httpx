package org.mvnsearch.http.protocol;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpCookie;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.vendor.Nodejs;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public abstract class HttpBaseExecutor implements BaseExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(HttpBaseExecutor.class);

    protected HttpClient httpClient() {
        HttpClient httpClient = HttpClient.create().secure(sslContextSpec -> {
            try {
                sslContextSpec.sslContext(SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build());
            } catch (Exception ignore) {

            }
        });
        if (System.getProperty("http.proxyHost") != null) {
            httpClient = httpClient.proxy(spec -> spec.type(ProxyProvider.Proxy.HTTP)
                    .host(System.getProperty("http.proxyHost"))
                    .port(Integer.parseInt(System.getProperty("http.proxyPort")))
                    .connectTimeoutMillis(20_000));
        }
        return httpClient;
    }

    protected List<HttpCookie> cookies(String domain) {
        try {
            final String httpFilePath = System.getProperty("http.file");
            Path cookieFile;
            if (httpFilePath != null) {
                cookieFile = Path.of(httpFilePath).getParent().resolve(".idea/httpRequests/http-client.cookies").toAbsolutePath();
            } else {
                cookieFile = Path.of(".idea/httpRequests/http-client.cookies");
            }
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
            log.error("HTX-100-600", e);
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public List<byte[]> request(HttpClient.ResponseReceiver<?> responseReceiver, URI requestUri, HttpRequest httpRequest) {
        final byte[] bytes = responseReceiver
                .uri(requestUri)
                .responseSingle((response, byteBufMono) -> {
                    final HttpResponseStatus httpStatus = response.status();
                    if (httpStatus == HttpResponseStatus.OK) {
                        System.out.println(colorOutput("bold,green", "Status: " + httpStatus));
                    } else {
                        System.out.println(colorOutput("bold,red", "Status: " + httpStatus));
                    }
                    final HttpHeaders responseHeaders = response.responseHeaders();
                    final Map<String, String> httpResponseHeaders = new HashMap<>();
                    //color header
                    responseHeaders.forEach(header -> {
                        httpResponseHeaders.put(header.getKey(), header.getValue());
                        System.out.println(colorOutput("green", header.getKey()) + ": " + header.getValue());
                    });
                    System.out.println();
                    String contentType = responseHeaders.get("Content-Type");
                    return byteBufMono.asByteArray().doOnNext(content -> {
                        if (contentType != null && isPrintable(contentType)) {
                            if (contentType.contains("json")) {
                                final String result = new String(content, StandardCharsets.UTF_8);
                                final String body = prettyJsonFormatWithJsonPath(result, httpRequest.getHeader("X-JSON-Path"));
                                System.out.print(body);
                                final String javaScriptTestCode = httpRequest.getJavaScriptTestCode();
                                if (javaScriptTestCode != null && !javaScriptTestCode.isEmpty()) {
                                    System.out.println();
                                    System.out.println("============Execute JS Test============");
                                    final String jsTestOutput = Nodejs.executeHttpClientCode(javaScriptTestCode, httpStatus.code(), httpResponseHeaders, contentType, result);
                                    System.out.println(jsTestOutput);
                                }
                            } else {
                                System.out.print(new String(content));
                            }
                        }
                    });
                }).block();
        //noinspection ConstantConditions
        return List.of(bytes);
    }
}
