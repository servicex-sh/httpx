package org.mvnsearch.http.protocol;


import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.HttpRequestParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Disabled
public class GraphqlExecutorTest {
    @Test
    public void testHttpRequest() throws Exception {
        Map<String, Object> context = new HashMap<>();
        String httpFile = """
                ### graphql query
                GRAPHQL https://countries.trevorblades.com/
                Content-Type: application/graphql
                 
                query {
                   continents {
                     name
                   }
                }
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
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
        request.cleanBody();
        final List<byte[]> bytesList = new GraphqlExecutor().execute(request);
        System.out.println("======size: " + bytesList.size());
    }

    @Test
    public void testGraphQLWithVariables() throws Exception {
        Map<String, Object> context = new HashMap<>();
        String httpFile = """
                ### GraphQL query with variables
                GRAPHQL https://httpbin.org/post
                                
                query demo($bookId: ID){
                    bookById(id: $bookId) {
                        id
                        name
                        pageCount
                        author {
                            firstName
                            lastName
                        }
                    }
                }
                                
                {
                  "bookId": "book-1"
                }
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        final List<byte[]> bytesList = new GraphqlExecutor().execute(request);
        System.out.println("======size: " + bytesList.size());
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
        request.cleanBody();
        final List<byte[]> bytesList = new GraphqlExecutor().execute(request);
        System.out.println("======size: " + bytesList.size());
    }

}
