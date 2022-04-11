package org.mvnsearch.http.protocol;


import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.HttpRequestParser;

import java.util.HashMap;
import java.util.Map;

@Disabled
public class MsgpackRpcExecutorTest {
    @Test
    public void testReqResp() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### msgpack request
                MSGPACK 127.0.0.1:18800/add
                Content-Type: application/json
                     
                [1, 2]  
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MsgpackRpcExecutor().execute(request);
    }

    @Test
    public void testArgsHeader() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### msgpack request
                MSGPACK 127.0.0.1:18800/add
                X-Args-1: 1
                Content-Type: application/json
                     
                4 
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MsgpackRpcExecutor().execute(request);
    }

    @Test
    public void testNeovimCurrentBuf() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### msgpack request
                MSGPACK localhost:6666/nvim_get_current_buf
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MsgpackRpcExecutor().execute(request);
    }

    @Test
    public void testNeovimLuaEval() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### msgpack request
                MSGPACK 127.0.0.1:6666/nvim_exec_lua
                X-Args-1: []
                Content-Type: text/x-lua
                  
                name = "line: "  
                return name .. vim.api.nvim_win_get_cursor(0)[1]
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MsgpackRpcExecutor().execute(request);
    }
}
