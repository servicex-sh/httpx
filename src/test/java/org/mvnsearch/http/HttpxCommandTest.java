package org.mvnsearch.http;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpxCommandTest {
    @Test
    public void testParseContext() throws Exception {
        final Path httpFilePath = Path.of("").resolve("src/test/http-demo/index.http");
        HttpxCommand command = new HttpxCommand();
        final Map<String, Object> context = command.constructHttpClientContext(httpFilePath);
        assertThat(context).containsKey("development");
        final Map<String, String> development = (Map<String, String>) context.get("development");
        assertThat(development).containsKey("base-url");
    }

    @Test
    public void testExtensionRequest() throws Exception {
        String extensionRequestJson = """
                {
                  "method": "POST",
                  "path": "https://httpbin.org/post",
                  "protocol": "HTTP/1.1",
                  "uri": "https://httpbin.org/post",
                  "headers": {
                    "Content-Type": "application/json"
                  },
                  "body": "e1wiaWRcIjoxfQ=="
                }
                """.trim();
        HttpxCommand httpxCommand = new HttpxCommand();
        httpxCommand.executeExtensionRequest(extensionRequestJson);
    }
}
