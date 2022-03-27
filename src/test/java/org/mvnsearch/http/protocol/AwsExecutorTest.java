package org.mvnsearch.http.protocol;


import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.HttpRequestParser;

import java.util.HashMap;
import java.util.Map;

/**
 * Guides and API References: https://docs.aws.amazon.com/index.html
 */
public class AwsExecutorTest {
    @Test
    public void testAwsRequest() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### hello aws
                GET https://iam.amazonaws.com/?Action=ListUsers&Version=2010-05-08
                X-Region-Id: us-east-1
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new AwsExecutor().execute(request);
    }
}
