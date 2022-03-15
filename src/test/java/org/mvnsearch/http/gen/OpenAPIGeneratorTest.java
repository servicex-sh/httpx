package org.mvnsearch.http.gen;

import org.junit.jupiter.api.Test;

import java.util.List;

public class OpenAPIGeneratorTest {
    @Test
    public void testGenerate() throws Exception {
        String url = "http://localhost:8080/v3/api-docs";
        String httpCode = new OpenAPIGenerator().generateHttpFileFromOpenAPI(url, List.of("/user/save"));
        System.out.println(httpCode);
    }
}
