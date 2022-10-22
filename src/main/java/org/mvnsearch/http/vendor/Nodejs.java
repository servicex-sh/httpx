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
    private static String httpClientPreScriptJS = null;

    public static String executeHttpClientCode(String jsBlockCode, int statusCode, Map<String, String> headers, String contentType, String body) {
        try {
            if (httpClientExecuteJS == null) {
                //noinspection ConstantConditions
                httpClientExecuteJS = IOUtils.toString(Nodejs.class.getResourceAsStream("/http-client-execute.js"), StandardCharsets.UTF_8);
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
                final String error = IOUtils.toString(p.getErrorStream(), StandardCharsets.UTF_8);
                if (error.contains("No such file")) {
                    if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                        System.err.println("Please use 'brew install node' to install Node.js first!");
                    } else {
                        System.err.println("Please install node.js first! Please click https://nodejs.org/en/download/");
                    }
                } else {
                    System.err.println(error);
                    System.out.println("=========JavaScript Code===========");
                    System.out.println(jsCode);
                }
                return "";
            }
        } catch (Exception e) {
            log.error("HTX-001-503", jsBlockCode, e);
            return "";
        }
    }

    public static String executePreScriptCode(String preScriptCode) {
        try {
            if (httpClientPreScriptJS == null) {
                //noinspection ConstantConditions
                httpClientPreScriptJS = IOUtils.toString(Nodejs.class.getResourceAsStream("/http-client-pre-stub.js"), StandardCharsets.UTF_8);
            }
            String jsCode = httpClientPreScriptJS + preScriptCode;
            ProcessBuilder pb = new ProcessBuilder("node");
            Process p = pb.start();
            p.getOutputStream().write(jsCode.getBytes(StandardCharsets.UTF_8));
            p.getOutputStream().close();
            final int exitCode = p.waitFor();
            if (exitCode == 0) {
                return IOUtils.toString(p.getInputStream(), StandardCharsets.UTF_8);
            } else {
                final String error = IOUtils.toString(p.getErrorStream(), StandardCharsets.UTF_8);
                if (error.contains("No such file")) {
                    if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                        System.err.println("Please use 'brew install node' to install Node.js first!");
                    } else {
                        System.err.println("Please install node.js first! Please click https://nodejs.org/en/download/");
                    }
                }
                return "";
            }
        } catch (Exception e) {
            log.error("HTX-001-503", preScriptCode, e);
            return "";
        }
    }

    public static String jsUrlEncode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20")
                .replaceAll("%21", "!")
                .replaceAll("%27", "'")
                .replaceAll("%28", "(")
                .replaceAll("%29", ")")
                .replaceAll("%7E", "~");
    }


}
