package org.mvnsearch.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mvnsearch.http.model.HttpMethod;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.HttpRequestParser;
import org.mvnsearch.http.protocol.GrpcExecutor;
import org.mvnsearch.http.protocol.HttpExecutor;
import org.mvnsearch.http.protocol.RSocketExecutor;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "unused"})
@Component
@Command(name = "httpx", version = "0.3.0", description = "CLI to run http file", mixinStandardHelpOptions = true)
public class HttpxCommand implements Callable<Integer> {
    @Option(names = {"-p", "--profile"}, description = "Profile")
    private String[] profile;
    @Option(names = {"-f", "--httpfile"}, description = "Http file", defaultValue = "index.http")
    private String httpFile;
    @Option(names = {"-l", "--list"}, description = "Display list")
    private boolean listRequests;
    @Option(names = {"-s", "--summary"}, description = "Display summary")
    private boolean summary;
    @Parameters(description = "positional params")
    private List<String> targets;

    @Override
    public Integer call() {
        final Path httpFilePath = Path.of(httpFile);
        if (!httpFilePath.toFile().exists()) {
            System.out.println("http file not found: " + httpFile);
            return -1;
        }
        try {
            Map<String, Object> context = constructHttpClientContext(httpFilePath);
            if (profile != null && profile.length > 0) {
                String activeProfile = profile[profile.length - 1];
                if (!context.containsKey(activeProfile)) {
                    System.out.println("profile not found: " + activeProfile);
                    return -1;
                }
                //noinspection unchecked
                context = (Map<String, Object>) context.get(activeProfile);
            }
            final List<HttpRequest> requests = parseHttpFile(context);
            if (summary) {
                for (HttpRequest request : requests) {
                    String comment = request.getComment();
                    if (comment == null) {
                        comment = "";
                    } else {
                        comment = " # " + comment;
                    }
                    if (request.getName() != null) {
                        System.out.println(request.getName() + comment);
                    } else {
                        System.out.println(request.getIndex() + comment);
                    }
                }
                return 0;
            }
            if (listRequests) {
                for (HttpRequest request : requests) {
                    String comment = request.getComment();
                    if (comment == null) {
                        comment = "";
                    } else {
                        comment = ": " + comment;
                    }
                    if (request.getName() != null) {
                        System.out.println(request.getIndex() + ". " + request.getName() + comment + " - " + request.getRequestTarget().getUri());
                    } else {
                        System.out.println(request.getIndex() + ". " + comment + " - " + request.getRequestTarget().getUri());
                    }
                }
                return 0;
            }
            if (targets != null && !targets.isEmpty()) {
                boolean targetFound = false;
                for (String target : targets) {
                    for (HttpRequest request : requests) {
                        if (request.getName().equals(target)) {
                            if (targetFound) {
                                System.out.println("=========================================");
                            }
                            targetFound = true;
                            execute(request);
                        }
                    }
                }
                if (!targetFound) {
                    System.err.println("Target not found in http file: " + String.join(",", targets));
                }
            } else {
                System.out.println("Please supply HTTP target!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    public List<HttpRequest> parseHttpFile(Map<String, Object> context) throws Exception {
        final Path httpFilePath = Path.of(httpFile);
        final String fileContent = Files.readString(httpFilePath, StandardCharsets.UTF_8);
        return HttpRequestParser.parse(fileContent, context);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> constructHttpClientContext(Path httpFilePath) throws Exception {
        Map<String, Object> context = new HashMap<>();
        final Path parentDir = httpFilePath.toAbsolutePath().getParent();
        final File envJsonFile = parentDir.resolve("http-client.env.json").toFile();
        final File envPrivateJsonFile = parentDir.resolve("http-client.private.env.json").toFile();
        ObjectMapper objectMapper = new ObjectMapper();
        if (envJsonFile.exists()) { // load env.json into context
            final Map<String, Object> env = objectMapper.readValue(envJsonFile, Map.class);
            context.putAll(env);
        }
        if (envPrivateJsonFile.exists()) { //load private.env.json
            final Map<String, Object> privateEnv = objectMapper.readValue(envPrivateJsonFile, Map.class);
            for (Map.Entry<String, Object> entry : privateEnv.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    Map<String, Object> privateProfileContext = (Map<String, Object>) entry.getValue();
                    if (context.containsKey(entry.getKey())) {
                        final Object profileContext = context.get(entry.getKey());
                        if (profileContext instanceof Map) { // replace entry by private.env.json
                            ((Map<String, Object>) profileContext).putAll(privateProfileContext);
                        } else {
                            context.put(entry.getKey(), entry.getValue());
                        }
                    } else {
                        context.put(entry.getKey(), entry.getValue());
                    }
                } else {
                    context.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return context;
    }

    public void execute(HttpRequest httpRequest) {
        final HttpMethod requestMethod = httpRequest.getMethod();
        if (requestMethod.isHttpMethod()) {
            new HttpExecutor().execute(httpRequest);
        } else if (requestMethod.isRSocketMethod()) {
            new RSocketExecutor().execute(httpRequest);
        } else if (requestMethod.isGrpcMethod()) {
            new GrpcExecutor().execute(httpRequest);
        } else {
            System.out.println("Not support: " + requestMethod.getName());
        }
    }

}