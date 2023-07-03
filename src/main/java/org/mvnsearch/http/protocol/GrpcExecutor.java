package org.mvnsearch.http.protocol;

import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpHeader;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.utils.JsonUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GrpcExecutor implements BaseExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(GrpcExecutor.class);

    public List<byte[]> execute(HttpRequest httpRequest) {
        try {
            List<String> command = new ArrayList<>();
            command.add("grpcurl");
            final URI requestUri = httpRequest.getRequestTarget().getUri();
            final String grpcSchema = requestUri.getScheme();
            if (grpcSchema.equals("https") || grpcSchema.equals("grpcs")) {
                command.add("-insecure");
            } else {
                command.add("-plaintext");
            }
            for (HttpHeader header : httpRequest.getHeaders()) {
                command.add("-H");
                command.add(header.getName() + ": " + header.getValue());
            }
            final byte[] bodyBytes = httpRequest.getBodyBytes();
            if (bodyBytes.length > 0) {
                String jsonText = new String(bodyBytes, StandardCharsets.UTF_8);
                if (jsonText.startsWith("{")) {
                    final Map<?, ?> jsonData = JsonUtils.readValue(jsonText, Map.class);
                    final String oneLineJsonText = JsonUtils.writeValueAsString(jsonData);
                    command.add("-d");
                    command.add(oneLineJsonText);
                }
            }
            if (requestUri.getPort() > 0) {
                command.add(requestUri.getHost() + ":" + requestUri.getPort());
            } else {
                command.add(requestUri.getHost());
            }
            final String path = requestUri.getRawPath();
            if (path.equals("/services")) {
                command.add("list");
            } else {
                command.add(path.substring(1));
            }
            final Process process = new ProcessBuilder().command(command).start();
            process.waitFor();
            System.out.println("GRPC" + " " + requestUri);
            System.out.println();
            if (process.exitValue() != 0) {
                String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                if (error.contains("No such file")) {
                    if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                        System.err.println("Please use 'brew install grpcurl' to install grpcurl first! For more click https://github.com/fullstorydev/grpcurl");
                    } else {
                        System.err.println("Please install grpcurl first! Please click https://github.com/fullstorydev/grpcurl");
                    }
                } else {
                    System.err.println(error);
                }
            } else {
                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                System.out.println(prettyJsonFormat(output));
                runJsTest(httpRequest, 200, Collections.emptyMap(), "application/json", output);
                return List.of(output.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            log.error("HTX-101-500", e);
        }
        return Collections.emptyList();
    }

}
