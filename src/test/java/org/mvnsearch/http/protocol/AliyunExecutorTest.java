package org.mvnsearch.http.protocol;


import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.HttpRequestParser;

import java.util.HashMap;
import java.util.Map;

public class AliyunExecutorTest {

    @Test
    public void testEcsRequest() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### hello aliyun
                ALIYUN https://ecs.ap-southeast-6.aliyuncs.com?Action=DescribeInstances&Version=2014-05-26
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new AliyunExecutor().execute(request);
    }
}
