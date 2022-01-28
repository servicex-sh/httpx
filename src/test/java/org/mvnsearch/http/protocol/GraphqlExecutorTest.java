package org.mvnsearch.http.protocol;


import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.HttpRequestParser;

import java.util.HashMap;
import java.util.Map;

@Disabled
public class GraphqlExecutorTest {
    @Test
    public void testHttpRequest() throws Exception {
        Map<String, Object> context = new HashMap<>();
        String httpFile = """
                ### graphql query
                GRAPHQL http://localhost:8080/graphql
                Content-Type: application/graphql
                 
                query {
                   bookById(id: "book-1") {
                     id
                     name
                     pageCount
                     author {
                       firstName
                       lastName
                     }
                   }
                }
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        new GraphqlExecutor().execute(request);
    }

    @Test
    public void testWebSocketRequest() throws Exception {
        Map<String, Object> context = new HashMap<>();
        String httpFile = """
                ### graphql query over WS
                GRAPHQL ws://localhost:4000/graphql
                Content-Type: application/graphql
                 
                query { hello }
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        new GraphqlExecutor().execute(request);
    }

    @Test
    public void testWebSocketSubscribe() throws Exception {
        Map<String, Object> context = new HashMap<>();
        String httpFile = """
                ### graphql query over WS
                GRAPHQL ws://localhost:4000/graphql
                Content-Type: application/graphql
                 
                subscription { greetings }
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        new GraphqlExecutor().execute(request);
    }

}
