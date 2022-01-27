package org.mvnsearch.http;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpFileRunnerTest extends SpringBootBaseTest {
    @Autowired
    private HttpFileRunner runner;

    @Test
    public void testHttpRequest() throws Exception {
        runner.run("-f", "src/test/http-demo/index.http", "-p", "production", "myip");
        assertThat(runner.getExitCode()).isEqualTo(0);
    }
}
