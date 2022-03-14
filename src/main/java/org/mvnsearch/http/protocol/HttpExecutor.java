package org.mvnsearch.http.protocol;

import org.mvnsearch.http.model.HttpCookie;
import org.mvnsearch.http.model.HttpHeader;
import org.mvnsearch.http.model.HttpRequest;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.util.List;


public class HttpExecutor extends HttpBaseExecutor {
    private static final List<String> IGNORED_HEADERS = List.of("x-json-type", "x-json-schema");

    public List<byte[]> execute(HttpRequest httpRequest) {
        final URI requestUri = httpRequest.getRequestTarget().getUri();
        HttpClient client = httpClient().headers(httpHeaders -> {
            for (HttpHeader header : httpRequest.getHeaders()) {
                if (!IGNORED_HEADERS.contains(header.getName().toLowerCase())) {
                    httpHeaders.add(header.getName(), header.getValue());
                }
            }
        });
        if (httpRequest.containsTag("no-redirect")) {
            client.followRedirect(false);
        }
        for (HttpCookie cookie : cookies(requestUri.getHost())) {
            client.cookie(cookie.toNettyCookie());
        }
        final String httpMethod = httpRequest.getMethod().getName();
        HttpClient.ResponseReceiver<?> responseReceiver = switch (httpMethod) {
            case "POST" -> client.post().send(httpRequest.requestBody());
            case "PUT" -> client.put().send(httpRequest.requestBody());
            case "DELETE" -> client.delete();
            case "HEAD" -> client.head();
            default -> client.get();
        };
        System.out.println(httpMethod + " " + requestUri);
        System.out.println();
        return request(responseReceiver, requestUri, httpRequest);
    }

}
