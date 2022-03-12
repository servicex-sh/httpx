package org.mvnsearch.http.vendor;

import org.apache.commons.io.IOUtils;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.utils.JsonUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class Nodejs {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(Nodejs.class);
    private static String httpClientExecuteJS = null;

    public static String executeHttpClientCode(String jsBlockCode, int statusCode, Map<String, String> headers, String contentType, String body) {
        try {
            if (httpClientExecuteJS == null) {
                loadStubJs();
            }
            String base64Body = Base64.getEncoder().encodeToString(jsUrlEncode(body).getBytes(StandardCharsets.UTF_8));
            String jsCode = httpClientExecuteJS;
            jsCode = jsCode.replace("222", String.valueOf(statusCode));
            jsCode = jsCode.replace("encodeBody({})", "'" + base64Body + "'");
            jsCode = jsCode.replace("application/json", contentType);
            jsCode = jsCode.replace("{'header': 'value'}", JsonUtils.writeValueAsString(headers));
            jsCode = jsCode + jsBlockCode;
            ProcessBuilder pb = new ProcessBuilder("node");
            Process p = pb.start();
            p.getOutputStream().write(jsCode.getBytes(StandardCharsets.UTF_8));
            p.getOutputStream().close();
            final int exitCode = p.waitFor();
            if (exitCode == 0) {
                return IOUtils.toString(p.getInputStream(), StandardCharsets.UTF_8);
            } else {
                return IOUtils.toString(p.getErrorStream(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("HTX-001-503", jsBlockCode, e);
            return "";
        }
    }

    private static void loadStubJs() throws Exception {
        //noinspection ConstantConditions
        httpClientExecuteJS = IOUtils.toString(Nodejs.class.getResourceAsStream("/http-client-execute.js"), StandardCharsets.UTF_8);
    }

    private static String jsUrlEncode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20")
                .replaceAll("%21", "!")
                .replaceAll("%27", "'")
                .replaceAll("%28", "(")
                .replaceAll("%29", ")")
                .replaceAll("%7E", "~");
    }


}
