package org.mvnsearch.http;

import org.jetbrains.annotations.Nullable;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpMethod;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.HttpRequestParser;
import org.mvnsearch.http.protocol.*;
import org.mvnsearch.http.utils.JsonUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;

@SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "unused"})
@Component
@Command(name = "httpx", version = "0.9.0", description = "CLI to run http file", mixinStandardHelpOptions = true)
public class HttpxCommand implements Callable<Integer> {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(HttpxCommand.class);
    @Option(names = {"--completions"}, description = "Shell Completion, such as zsh, bash")
    private String completions;
    @Option(names = {"-p", "--profile"}, description = "Profile")
    private String[] profile;
    @Option(names = {"-e", "--env"}, description = "Environment variables")
    private String[] variables;
    @Option(names = {"-f", "--httpfile"}, description = "Http file", defaultValue = "index.http")
    private String httpFile;
    @Option(names = {"-t", "--target"}, description = "Targets to run")
    private String target;
    @Option(names = {"-l", "--list"}, description = "Display list")
    private boolean listRequests;
    @Option(names = {"-s", "--summary"}, description = "Display summary")
    private boolean summary;
    @Parameters(description = "positional params")
    private List<String> targets;
    private boolean fromStdin = false;

    @Override
    public Integer call() {
        if (completions != null) {
            printShellCompletion();
            return 0;
        }
        String httpCode = readHttpCodeFromStdin();
        if (httpCode == null) {
            Path httpFilePath;
            if (Objects.equals(httpFile, "index.http")) {  //resolve index.http with parent directory support
                httpFilePath = resolveIndexHttpFile(Path.of(httpFile).toAbsolutePath());
            } else { //resolve normal http file
                httpFilePath = Path.of(httpFile);
                if (!httpFilePath.toFile().exists()) {
                    httpFilePath = null;
                }
            }
            if (httpFilePath == null) {
                System.out.println("http file not found: " + httpFile);
                return -1;
            } else {
                try {
                    this.httpFile = httpFilePath.toAbsolutePath().toString();
                    httpCode = Files.readString(httpFilePath, StandardCharsets.UTF_8);
                } catch (Exception ignore) {
                    log.error("HTX-001-501", httpFile);
                }
            }
        }
        try {
            Map<String, Object> context = fromStdin ? new HashMap<>() : constructHttpClientContext(Path.of(httpFile));
            if (!context.isEmpty()) {
                String activeProfile;
                if (profile != null && profile.length > 0) { // get profile from command line
                    activeProfile = profile[profile.length - 1];
                } else {  // get first profile
                    TreeSet<String> treeSet = new TreeSet<>(context.keySet());
                    activeProfile = treeSet.first();
                }
                //noinspection unchecked
                context = (Map<String, Object>) context.get(activeProfile);
            }
            // profile variables overwrite by definition: `-e user=xxx`
            if (variables != null && variables.length > 0) {
                for (String variable : variables) {
                    final String[] parts = variable.split("=", 2);
                    if (parts.length == 2) {
                        context.put(parts[0], parts[1]);
                    } else {
                        context.put(parts[0], "true");
                    }
                }
            }
            final List<HttpRequest> requests = HttpRequestParser.parse(httpCode, context);
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
            //set targets from --target option if targets empty
            if ((targets == null || targets.isEmpty()) && target != null) {
                targets = List.of(target.split(","));
            }
            // set default target to first if empty
            if (targets == null || targets.isEmpty()) {
                targets = List.of("1");
            }
            boolean targetFound = false;
            for (String target : targets) {
                for (HttpRequest request : requests) {
                    if (request.match(target)) {
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
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> constructHttpClientContext(Path httpFilePath) throws Exception {
        Map<String, Object> context = new HashMap<>();
        final Path parentDir = httpFilePath.toAbsolutePath().getParent();
        final File envJsonFile = parentDir.resolve("http-client.env.json").toFile();
        final File envPrivateJsonFile = parentDir.resolve("http-client.private.env.json").toFile();
        if (envJsonFile.exists()) { // load env.json into context
            final Map<String, Object> env = JsonUtils.readValue(envJsonFile, Map.class);
            context.putAll(env);
        }
        if (envPrivateJsonFile.exists()) { //load private.env.json
            final Map<String, Object> privateEnv = JsonUtils.readValue(envPrivateJsonFile, Map.class);
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

    public void execute(HttpRequest httpRequest) throws Exception {
        httpRequest.cleanBody();
        final HttpMethod requestMethod = httpRequest.getMethod();
        List<byte[]> result;
        if (requestMethod.isHttpMethod()) {
            result = new HttpExecutor().execute(httpRequest);
        } else if (requestMethod.isRSocketMethod()) {
            result = new RSocketExecutor().execute(httpRequest);
        } else if (requestMethod.isGrpcMethod()) {
            result = new GrpcExecutor().execute(httpRequest);
        } else if (requestMethod.isGraphQLMethod()) {
            result = new GraphqlExecutor().execute(httpRequest);
        } else if (requestMethod.isDubboMethod()) {
            result = new DubboExecutor().execute(httpRequest);
        } else if (requestMethod.isMailMethod()) {
            result = new MailExecutor().execute(httpRequest);
        } else if (requestMethod.isPubMethod()) {
            result = new MessagePublishExecutor().execute(httpRequest);
        } else if (requestMethod.isSubMethod()) {
            result = new MessageSubscribeExecutor().execute(httpRequest);
        } else {
            result = Collections.emptyList();
            System.out.print("Not support: " + requestMethod.getName());
        }
        System.out.println();
        if (!fromStdin) {
            if (httpRequest.getRedirectResponse() != null && !result.isEmpty()) {
                writeResponse(httpRequest.getRedirectResponse(), result);
            }
        }
    }

    void writeResponse(String redirectResponse, List<byte[]> content) {
        String[] parts = redirectResponse.split("\\s+", 2);
        String responseFile = parts[1];
        try {
            final Path httpFilePath = Path.of(httpFile);
            Path responseFilePath;
            if (responseFile.startsWith("/") || responseFile.contains(":\\")) {
                responseFilePath = Path.of(responseFile);
            } else {
                responseFilePath = httpFilePath.toAbsolutePath().getParent().resolve(responseFile);
            }
            final File parentDir = responseFilePath.toAbsolutePath().getParent().toFile();
            if (!parentDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                parentDir.mkdirs();
            }
            try (FileOutputStream fos = new FileOutputStream(responseFilePath.toFile())) {
                for (byte[] bytes : content) {
                    fos.write(bytes);
                }
            }
            System.out.println("---------------------------------");
            System.out.println("Write to " + responseFile + " successfully!");
        } catch (Exception e) {
            log.error("HTX-001-500", responseFile);
        }
    }

    private void printShellCompletion() {
        String zshCompletion = """
                #compdef httpx
                #autload
                                    
                local subcmds=()
                                    
                while read -r line ; do
                   if [[ ! $line == Available* ]] ;
                   then
                      subcmds+=(${line/[[:space:]]*\\#/:})
                   fi
                done < <(httpx --summary)
                                    
                _describe 'command' subcmds
                """;
        System.out.println(zshCompletion);
    }

    @Nullable
    private String readHttpCodeFromStdin() {
        try {
            if (System.in.available() > 0) {
                fromStdin = true;
                byte[] bytes = System.in.readAllBytes();
                if (bytes != null && bytes.length > 0) {
                    return new String(bytes, StandardCharsets.UTF_8);
                }
            }
        } catch (Exception ignore) {

        }
        return null;
    }

    private Path resolveIndexHttpFile(Path httpFilePath) {
        if (!httpFilePath.toFile().exists()) {
            final Path currentDir = httpFilePath.getParent();
            final Path parentDir = currentDir.getParent();
            if (parentDir == null) { // can not find index.http in parent chain
                // find default index.http ~/.httpx/index.http
                final Path defaultIndexHttp = Path.of(System.getProperty("user.home")).resolve(".httpx").resolve("index.http").toAbsolutePath();
                if (defaultIndexHttp.toFile().exists()) {
                    return defaultIndexHttp;
                }
                return null;
            }
            return resolveIndexHttpFile(parentDir.resolve("index.http"));
        }
        return httpFilePath;
    }

}