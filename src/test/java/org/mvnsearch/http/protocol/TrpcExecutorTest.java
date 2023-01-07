package org.mvnsearch.http.protocol;


import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.HttpRequestParser;

import java.util.HashMap;
import java.util.Map;

public class TrpcExecutorTest {

    @Test
    public void testTrpcQuery() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### trpc hello
                TRPC http://localhost:2022/greeting.hello
                Content-Type: application/json
                        
                {
                  "name": "world"
                }
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new TrpcExecutor().execute(request);
    }

    @Test
    public void testTrpcMutate() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### trpc hello
                TRPCM http://localhost:2022/post.createPost
                Content-Type: application/json
                        
                {
                  "title": "hello world",
                  "text": "check out https://tRPC.io"
                }
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new TrpcExecutor().execute(request);
    }

}
