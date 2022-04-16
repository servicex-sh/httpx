package org.mvnsearch.http.protocol;


import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.HttpRequestParser;

import java.util.HashMap;
import java.util.Map;

@Disabled
public class NeovimExecutorTest {
    @Test
    public void testReqResp() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### neovim request
                NVIM nvim_exec_lua
                Content-Type: text/x-lua
                     
                vim.api.nvim_command('!ls')
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new NeovimExecutor().execute(request);
    }

    @Test
    public void testAddLine() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### neovim request
                NVIM nvim_buf_set_lines
                Content-Type: application/json
                                           
                [0, 0, 0, true, ["hell222o"]]
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new NeovimExecutor().execute(request);
    }
}
