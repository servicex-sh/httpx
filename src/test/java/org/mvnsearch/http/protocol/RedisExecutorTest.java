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
                RSET nick
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
                ### redis hmset
                HMSET user.1
                Host: localhost:6379
                Content-Type: application/json
                                
                { "id": 1, "name": "jackie", "age": 40}
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new RedisExecutor().execute(request);
    }

    @Test
    public void testRedisJsonSet() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### redis json set
                JSONSET user.1/$
                Host: localhost:16379
                Content-Type: application/json
                                
                { "id": 1, "name": "jackie", "age": 40}
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new RedisExecutor().execute(request);
    }

    @Test
    public void testRedisJsonGet() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### redis json get
                JSONGET user.1/$
                Host: localhost:16379
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new RedisExecutor().execute(request);
    }

    @Test
    public void testRedisEval() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### redis hset
                EVAL 0
                Host: localhost:6379
                Content-Type: text/x-lua
                                
                local welcome="Hello Compose" 
                return welcome
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new RedisExecutor().execute(request);
    }

    @Test
    public void testRedisEvalWithParams() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### redis eval
                EVAL 1 name Jackie
                Host: localhost:6379
                Content-Type: text/x-lua
                  
                local val="Hello "                 
                return val .. ARGV[1]
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new RedisExecutor().execute(request);
    }

    @Test
    public void testRedisLoad() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### redis 7 fuctions
                LOAD mylib
                Host: localhost:16379
                Content-Type: text/x-lua
                  
                redis.register_function(
                  'knockknock',
                  function() return 'Who\\'s there?' end
                )
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new RedisExecutor().execute(request);
    }
}
