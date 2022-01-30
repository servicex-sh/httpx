package org.mvnsearch.http.protocol;


import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.HttpRequestParser;

import java.util.HashMap;
import java.util.Map;

@Disabled
public class GrpcExecutorTest {
    @Test
    public void testGrpcRequest() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### grpc call SayHello
                //@name SayHello
                GRPC localhost:50052/org.mvnsearch.service.Greeter/SayHello
                                
                {
                   "name": "Jackie"
                }
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        System.out.println(request.getRequestTarget().getUri());
        new GrpcExecutor().execute(request);
    }
}
