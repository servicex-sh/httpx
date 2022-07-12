package org.mvnsearch.http.protocol;


import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.HttpRequestParser;

import java.util.HashMap;

@Disabled
public class RSocketExecutorTest {

    @Test
    public void testRequestResponse() throws Exception {
        @Language("HTTP Request")
        String httpFileCode = """
                ### RSocket Request
                RSOCKET com.example.UserService.findById
                Host: 127.0.0.1:42252
                Content-Type: application/json
                                 
                1
                """;
        final HttpRequest httpRequest = HttpRequestParser.parse(httpFileCode, new HashMap<>()).get(0);
        httpRequest.cleanBody();
        new RSocketExecutor().execute(httpRequest);
    }

    @Test
    public void testRequestStream() throws Exception {
        @Language("HTTP Request")
        String httpFileCode = """
                ### RSocket Request
                STREAM com.example.UserService.findAll
                Host: 127.0.0.1:42252
                Content-Type: application/json
                                 
                1
                """;
        final HttpRequest httpRequest = HttpRequestParser.parse(httpFileCode, new HashMap<>()).get(0);
        httpRequest.cleanBody();
        new RSocketExecutor().execute(httpRequest);
    }

    @Test
    public void testGraphQLOverRequest() throws Exception {
        @Language("HTTP Request")
        String httpFileCode = """
                ### GraphQL query over RSocket request/response
                //@name graphql-rs-req
                GRAPHQL rsocketws://localhost:8080/rsocket/graphql
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
        final HttpRequest httpRequest = HttpRequestParser.parse(httpFileCode, new HashMap<>()).get(0);
        httpRequest.cleanBody();
        new RSocketExecutor().execute(httpRequest);
    }

    @Test
    public void testGraphQLOverRequestWithVariables() throws Exception {
        @Language("HTTP Request")
        String httpFileCode = """
                ### GraphQL query over RSocket request/response
                //@name graphql-rs-req
                GRAPHQL rsocketws://localhost:8080/rsocket/graphql
                Content-Type: application/graphql
                               
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
        final HttpRequest httpRequest = HttpRequestParser.parse(httpFileCode, new HashMap<>()).get(0);
        httpRequest.cleanBody();
        new RSocketExecutor().execute(httpRequest);
    }

    @Test
    public void testGraphQLOverStream() throws Exception {
        @Language("HTTP Request")
        String httpFileCode = """
                ### GraphQL subscription over RSocket Stream
                //@name graphql-rs-sub
                GRAPHQL rsocketws://localhost:8080/rsocket/graphql
                Content-Type: application/graphql
                                 
                subscription { greetings }
                """;
        final HttpRequest httpRequest = HttpRequestParser.parse(httpFileCode, new HashMap<>()).get(0);
        httpRequest.cleanBody();
        new RSocketExecutor().execute(httpRequest);
    }
}
