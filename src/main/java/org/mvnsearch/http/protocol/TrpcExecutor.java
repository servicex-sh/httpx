package org.mvnsearch.http.protocol;

import org.mvnsearch.http.model.HttpMethod;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.HttpRequestTarget;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TrpcExecutor extends HttpExecutor {
    public List<byte[]> execute(HttpRequest httpRequest) {
        String tprcMethod = httpRequest.getMethod().getName();
        String httpMethod = switch (tprcMethod) {
            case "TPRC" -> "GET";
            case "TPRCQ" -> "GET";
            case "TRPCM" -> "POST";
            default -> "GET";
        };
        final String bodyText = httpRequest.bodyText();
        HttpRequest realHttpRequest = new HttpRequest();
        // http method
        realHttpRequest.setMethod(HttpMethod.valueOf(httpMethod));
        // http url
        String httpUrl = httpRequest.getRequestTarget().getUri().toString();
        if (!bodyText.isEmpty()) {
            httpUrl = httpUrl + "?input=" + URLEncoder.encode(bodyText, StandardCharsets.UTF_8);
        }
        realHttpRequest.setRequestTarget(HttpRequestTarget.valueOf(httpMethod, httpUrl));
        // http headers
        realHttpRequest.setHeaders(httpRequest.getHeaders());
        // http body
        realHttpRequest.setBodyBytes(httpRequest.getBodyBytes());
        if (!httpMethod.equals("GET")) {
            realHttpRequest.setBodyBytes(bodyText.getBytes());
        }
        return super.execute(realHttpRequest);
    }

}
