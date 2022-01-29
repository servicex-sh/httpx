package org.mvnsearch.http.protocol;

import org.mvnsearch.http.model.HttpCookie;
import org.mvnsearch.http.model.HttpHeader;
import org.mvnsearch.http.model.HttpRequest;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.util.List;


public class HttpExecutor extends HttpBaseExecutor {
    public List<byte[]> execute(HttpRequest httpRequest) {
        final URI requestUri = httpRequest.getRequestTarget().getUri();
        HttpClient client = httpClient().headers(httpHeaders -> {
            if (httpRequest.getHeaders() != null) {
                for (HttpHeader header : httpRequest.getHeaders()) {
                    httpHeaders.add(header.getName(), header.getValue());
                }
            }
        });
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
        return request(responseReceiver, requestUri);
    }

}
