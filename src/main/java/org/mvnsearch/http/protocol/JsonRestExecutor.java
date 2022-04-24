package org.mvnsearch.http.protocol;

import org.mvnsearch.http.model.HttpMethod;
import org.mvnsearch.http.model.HttpRequest;

import java.nio.charset.StandardCharsets;
import java.util.List;


public class JsonRestExecutor extends HttpExecutor {
    public List<byte[]> execute(HttpRequest httpRequest) {
        final String httpMethod = httpRequest.getHeader("X-Method", "POST");
        httpRequest.setMethod(new HttpMethod(httpMethod));
        final String xBodyName = httpRequest.getHeader("X-Body-Name");
        if (xBodyName != null) {
            String body = httpRequest.jsonObjectBodyWithArgsHeaders();
            httpRequest.setBodyBytes(body.getBytes(StandardCharsets.UTF_8));
            httpRequest.replaceHeader("Content-Type", "application/json");
        } else {
            if (httpRequest.containsArgsHeader()) {
                String body = httpRequest.jsonArrayBodyWithArgsHeaders();
                httpRequest.setBodyBytes(body.getBytes(StandardCharsets.UTF_8));
                httpRequest.replaceHeader("Content-Type", "application/json");
            }
        }
        return super.execute(httpRequest);
    }

}
