package org.mvnsearch.http.protocol;


import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.HttpRequestParser;

import java.util.HashMap;
import java.util.Map;

@Disabled
public class RedisExecutorTest {
    @Test
    public void testRedisSet() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### redis set
                SET nick
                Host: localhost:6379
                Content-Type: text/plain
                     
                Jackie   
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new RedisExecutor().execute(request);
    }

    @Test
    public void testRedisHSet() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### redis hset
                HMSET user.1
                Host: localhost:6379
                Content-Type: application/json
                                
                { "id": 1, "name": "jackie", age: 40}
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new RedisExecutor().execute(request);
    }
}
