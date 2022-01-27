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
}
