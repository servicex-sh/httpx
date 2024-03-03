package org.mvnsearch.http;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.*;
import org.mvnsearch.http.model.extension.HttpxExtensionRequest;
import org.mvnsearch.http.protocol.*;
import org.mvnsearch.http.utils.JsonUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;

@SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "unused"})
@Component
@Command(name = "httpx", version = "0.42.4", description = "CLI to run http file", mixinStandardHelpOptions = true)
public class HttpxCommand implements Callable<Integer> {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(HttpxCommand.class);
    @CommandLine.Unmatched
    private List<String> unmatchedOptions;
    @Option(names = {"--completions"}, description = "Shell Completion, such as zsh, bash")
    private String completions;
    @Option(names = {"-g"}, description = "Display global variables")
    private boolean displayGlobalVariables;
    @Option(names = {"-p"}, description = "Profile")
    private String[] profile;
    @Option(names = {"-x"}, description = "HTTP proxy")
    private String httpProxy;
    @Option(names = {"-f", "--httpfile"}, description = "Http file, and default is index.http")
    private String httpFile;
    @Option(names = {"-t"}, description = "Targets to run")
    private String target;
    @Option(names = {"-d", "--data"}, description = "Body data from text, @file or HTTP url")
    private String bodyData;
    @Option(names = {"-l", "--list"}, description = "List all targets in http file")
    private boolean listRequests;
    @Option(names = {"-s", "--summary"}, description = "Display summary")
    private boolean summary;
    @Option(names = {"-a"}, description = "Run all requests")
    private boolean runAllRequests = false;
    @Parameters(description = "targets to run")
    private List<String> targets;
    private boolean requestFromStdin = false;
    /**
     * body from input - stdin, data text, @file or http url
     */
    private byte[] bodyFromInput = null;

    @Override
    public Integer call() {
        // display global variables
        if (displayGlobalVariables) {
            printGlobalVariables();
            return 0;
        }
        if (httpFile == null) {
            httpFile = "index.http";
        }
        if (completions != null) {
            printShellCompletion();
            return 0;
        }
        String httpCode = null;
        //read input from stdin
        String httpCodeOrBody = readHttpCodeOrBodyFromStdin();
        if (httpCodeOrBody != null) {
            if (HttpMethod.isRequestLine(httpCodeOrBody)) {
                httpCode = httpCodeOrBody;
                requestFromStdin = true;
            } else {
                final String stdinText = httpCodeOrBody.trim();
                // check is httpx extension request or not
                if (stdinText.startsWith("{") && stdinText.contains("\"method\"") && stdinText.contains("\"uri\"")) {
                    return executeExtensionRequest(stdinText);
                } else {
                    bodyFromInput = stdinText.getBytes(StandardCharsets.UTF_8); //trim end line
                }
            }
        }
        Path httpFilePath;
        if (Objects.equals(httpFile, "index.http")) {  //resolve index.http with parent directory support
            httpFilePath = resolveIndexHttpFile(Path.of(httpFile).toAbsolutePath());
        } else { //resolve normal http file
            httpFilePath = Path.of(httpFile);
            if (!httpFilePath.toFile().exists()) {
                httpFilePath = null;
            }
        }
        if (httpCode == null) {
            if (httpFilePath == null) {
                System.out.println("http file not found: " + httpFile);
                return -1;
            } else {
                try {
                    this.httpFile = httpFilePath.toAbsolutePath().toString();
                    System.setProperty("http.file", this.httpFile);
                    httpCode = Files.readString(httpFilePath, StandardCharsets.UTF_8);
                } catch (Exception ignore) {
                    log.error("HTX-001-501", httpFile);
                }
            }
        }
        // resolve body data from data option
        try {
            resolveBodyData(httpFilePath);
        } catch (Exception e) {
            log.error("HTX-001-502", bodyData, e);
            return -1;
        }
        try {
            Map<String, Object> context = requestFromStdin ? new HashMap<>() : constructHttpClientContext(Path.of(httpFile));
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
            // profile variables overwrite by unmatched options: `-e user=xxx`
            if (unmatchedOptions != null && !unmatchedOptions.isEmpty()) {
                for (String variable : unmatchedOptions) {
                    final String[] parts = variable.substring(2).split("=", 2);
                    if (parts.length == 2) {
                        context.put(parts[0], parts[1]);
                    } else {
                        context.put(parts[0], "true");
                    }
                }
            }
            // load global variables into context
            injectGlobalVariables(context);
            // parse http requests
            final HttpFile httpFile = HttpRequestParser.parseHttpFile(httpCode);
            final List<HttpRequest> requests = httpFile.getRequests();
            // list summary for code completion
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
            // list all requests
            if (listRequests) {
                for (HttpRequest request : requests) {
                    HttpRequestParser.parse(request, context);
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
            //http proxy configuration
            if (httpProxy == null) {
                httpProxy = System.getenv("HTTP_PROXY");
            }
            if (httpProxy != null && !httpProxy.isEmpty()) {
                if (!httpProxy.contains("://")) {
                    httpProxy = "http://" + httpProxy;
                }
                var proxyUri = URI.create(httpProxy);
                System.setProperty("http.proxyHost", proxyUri.getHost());
                System.setProperty("http.proxyPort", "" + proxyUri.getPort());
            }
            //run all requests
            if (runAllRequests) {
                for (HttpRequest request : requests) {
                    System.out.println("=============" + request.getName() + "==================");
                    HttpRequestParser.parse(request, context);
                    execute(request, httpFilePath);
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
                        if (targetFound) { // seperate line for multi targets
                            System.out.println("=========================================");
                        }
                        targetFound = true;
                        HttpRequestParser.parse(request, context);
                        execute(request, httpFilePath);
                    }
                }
            }
            if (!targetFound) {
                System.err.println("Target not found in http file: " + String.join(",", targets));
            }
        } catch (Exception e) {
            log.error("HTX-002-500", httpFile, e);
            return -1;
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> constructHttpClientContext(Path httpFilePath) throws Exception {
        Map<String, Object> context = new HashMap<>();
        final Path httpFileDir = httpFilePath.toAbsolutePath().getParent();
        File envJsonFile = httpFileDir.resolve("http-client.env.json").toFile();
        File envPrivateJsonFile = httpFileDir.resolve("http-client.private.env.json").toFile();
        if (!envJsonFile.exists()) { // resolve http-client.env.json from current directory if not found
            envJsonFile = Paths.get("http-client.env.json").toFile();
        }
        if (!envPrivateJsonFile.exists()) { // resolve http-client.private.env.json from current directory if not found
            envPrivateJsonFile = Paths.get("http-client.private.env.json").toFile();
        }
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
        //read rest-client.environmentVariables from .vscode/settings.json
        final File vsCodeSettingsJsonFile = httpFileDir.resolve(".vscode/settings.json").toFile();
        if (vsCodeSettingsJsonFile.exists()) {
            final Map<String, Object> settings = JsonUtils.readValue(envJsonFile, Map.class);
            if (settings.containsKey("rest-client.environmentVariables")) {
                Map<String, Map<String, Object>> profiles = new HashMap<>((Map<String, Map<String, Object>>) settings.get("rest-client.environmentVariables"));
                final Map<String, Object> sharedProfile = profiles.remove("$shared");
                for (Map.Entry<String, Map<String, Object>> entry : profiles.entrySet()) {
                    String profile = entry.getKey();
                    final Map<String, Object> pairs = entry.getValue();
                    if (sharedProfile != null) {
                        for (Map.Entry<String, Object> sharedEntry : sharedProfile.entrySet()) {
                            pairs.putIfAbsent(sharedEntry.getKey(), sharedEntry.getValue());
                        }
                    }
                    context.put(profile, pairs);
                }
            }
        }
        return context;
    }

    private void injectGlobalVariables(Map<String, Object> context) {
        Map<String, Object> globalVariables = loadGlobalVariables();
        for (Map.Entry<String, Object> entry : globalVariables.entrySet()) {
            if (!context.containsKey(entry.getKey())) {
                context.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private Map<String, Object> loadGlobalVariables() {
        // read $HOME/.servicex/global_variables.json
        final Path globalVariablesFilePath = Path.of(System.getProperty("user.home")).resolve(".servicex").resolve("global_variables.json").toAbsolutePath();
        if (globalVariablesFilePath.toFile().exists()) {
            File globalVariablesFile = globalVariablesFilePath.toFile();
            try {
                //noinspection unchecked
                return JsonUtils.OBJECT_MAPPER.readValue(globalVariablesFile, Map.class);
            } catch (Exception e) {
                log.error("HTX-002-504", globalVariablesFile, e);
            }
        }
        return Map.of();
    }

    public void execute(HttpRequest httpRequest, @Nullable Path httpFilePath) throws Exception {
        httpRequest.cleanBody(httpFilePath);
        //reset body from input
        if (bodyFromInput != null && bodyFromInput.length > 0) {
            httpRequest.setBodyBytes(bodyFromInput);
        }
        final HttpMethod requestMethod = httpRequest.getMethod();
        String host = httpRequest.getRequestTarget().getHost();
        List<byte[]> result;
        if (requestMethod.isHttpMethod()) {
            result = new HttpExecutor().execute(httpRequest);
        } else if (requestMethod.isWebSocketMethod()) {
            result = new WebSocketExecutor().execute(httpRequest);
        } else if (requestMethod.isRestMethod()) {
            result = new JsonRestExecutor().execute(httpRequest);
        } else if (requestMethod.isRSocketMethod()) {
            result = new RSocketExecutor().execute(httpRequest);
        } else if (requestMethod.isGrpcMethod()) {
            result = new GrpcExecutor().execute(httpRequest);
        } else if (requestMethod.isGraphQLMethod()) {
            final String uri = httpRequest.getRequestTarget().getUri().toString();
            if (uri.startsWith("rsocket")) { // GraphQL over RSocket
                result = new RSocketExecutor().execute(httpRequest);
            } else {
                result = new GraphqlExecutor().execute(httpRequest);
            }
        } else if (requestMethod.isDubboMethod()) {
            result = new DubboExecutor().execute(httpRequest);
        } else if (requestMethod.isSofaMethod()) {
            result = new SofaRpcExecutor().execute(httpRequest);
        } else if (requestMethod.isTarpcMethod()) {
            result = new TarpcExecutor().execute(httpRequest);
        } else if (requestMethod.isMsgpackMethod()) {
            result = new MsgpackRpcExecutor().execute(httpRequest);
        } else if (requestMethod.isNvimMethod()) {
            result = new NeovimExecutor().execute(httpRequest);
        } else if (requestMethod.isJsonRPCMethod()) {
            result = new JsonRpcExecutor().execute(httpRequest);
        } else if (requestMethod.isTRPCMethod()) {
            result = new TrpcExecutor().execute(httpRequest);
        } else if (requestMethod.isThriftMethod()) {
            result = new ThriftExecutor().execute(httpRequest);
        } else if (requestMethod.isZeromqMethod()) {
            result = new ZeromqExecutor().execute(httpRequest);
        } else if (requestMethod.isMailMethod()) {
            result = new MailExecutor().execute(httpRequest);
        } else if (requestMethod.isPubMethod()) {
            result = new MessagePublishExecutor().execute(httpRequest);
        } else if (requestMethod.isSubMethod()) {
            result = new MessageSubscribeExecutor().execute(httpRequest);
        } else if (requestMethod.isMemcacheMethod()) {
            result = new MemcacheExecutor().execute(httpRequest);
        } else if (requestMethod.isRedisMethod()) {
            result = new RedisExecutor().execute(httpRequest);
        } else if (requestMethod.isSSHMethod()) {
            result = new SSHExecutor().execute(httpRequest);
        } else if (requestMethod.isAwsMethod() || (host.endsWith(".amazonaws.com") && requestMethod.isHttpMethod())) {
            result = new AwsExecutor().execute(httpRequest);
        } else if (requestMethod.isAliyunMethod()) {
            result = new AliyunExecutor().execute(httpRequest);
        } else if (requestMethod.isChatGPTMethod()) {
            result = new ChatGPTExecutor().execute(httpRequest);
        } else {
            result = Collections.emptyList();
            System.out.print("Not support: " + requestMethod.getName());
        }
        System.out.println();
        //disable response written to file because of security
        /*if (!requestFromStdin) { // if request from stdin, ignore to write response to file
            if (httpRequest.getRedirectResponse() != null && !result.isEmpty()) {
                writeResponse(httpRequest.getRedirectResponse(), result);
            }
        }*/
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
                done < <(httpx -s)
                                    
                _describe 'command' subcmds
                """;
        System.out.println(zshCompletion);
    }

    private void printGlobalVariables() {
        final Map<String, Object> globalVariables = loadGlobalVariables();
        System.out.println(JsonUtils.writeValueAsPrettyColorString(globalVariables));
    }

    @Nullable
    private String readHttpCodeOrBodyFromStdin() {
        try {
            if (System.in.available() > 0) {
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
                // find default index.http ~/.servicex/index.http
                final Path defaultIndexHttp = Path.of(System.getProperty("user.home")).resolve(".servicex").resolve("index.http").toAbsolutePath();
                if (defaultIndexHttp.toFile().exists()) {
                    return defaultIndexHttp;
                }
                return null;
            }
            return resolveIndexHttpFile(parentDir.resolve("index.http"));
        }
        return httpFilePath;
    }

    private void resolveBodyData(@Nullable Path httpFilePath) throws Exception {
        if (bodyData != null) {
            if (bodyData.startsWith("@")) {  //read data from file
                String dataFilePath = bodyData.substring(1);
                if (dataFilePath.startsWith("/") || dataFilePath.contains(":\\")) { // linux/windows absolute path
                    bodyFromInput = Files.readAllBytes(Path.of(dataFilePath));
                } else { //relative path
                    if (httpFilePath != null) { // read file relative to http file path
                        bodyFromInput = Files.readAllBytes(httpFilePath.toAbsolutePath().getParent().resolve(dataFilePath));
                    } else { // read file relative to current directory
                        bodyFromInput = Files.readAllBytes(Path.of(dataFilePath));
                    }
                }
            } else if (bodyData.startsWith("https://") || bodyData.startsWith("http://")) {
                bodyFromInput = IOUtils.toByteArray(new URL(bodyData));
            } else {
                bodyFromInput = bodyData.getBytes(StandardCharsets.UTF_8);
            }
        }
    }

    public int executeExtensionRequest(String extensionRequestJson) {
        try {
            final HttpxExtensionRequest extensionRequest = JsonUtils.readValue(extensionRequestJson, HttpxExtensionRequest.class);
            HttpRequest httpRequest = new HttpRequest();
            httpRequest.setMethod(HttpMethod.valueOf(extensionRequest.getMethod()));
            HttpRequestTarget requestTarget = new HttpRequestTarget();
            final URI uri = URI.create(extensionRequest.getUri());
            requestTarget.setUri(uri);
            requestTarget.setHost(uri.getHost());
            httpRequest.setRequestTarget(requestTarget);
            final Map<String, String> headers = extensionRequest.getHeaders();
            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    httpRequest.addHttpHeader(entry.getKey(), entry.getValue());
                }
            }
            final byte[] body = extensionRequest.getBody();
            if (body != null && body.length > 0) {
                httpRequest.setBodyBytes(body);
            }
            execute(httpRequest, null);
        } catch (Exception e) {
            log.error("HTX-401-500", e);
            return -1;
        }
        return 0;
    }
}