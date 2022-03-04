package org.mvnsearch.http.gen;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.mvnsearch.http.model.HttpMethod;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.utils.JsonUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CodeGenerator {
    public static final List<String> HTTP_FRAMEWORKS = List.of("curl", "webclient", "fetch", "okhttp3", "apache-http-client5");

    public String generate(HttpRequest httpRequest, @NotNull String gen) {
        final HttpMethod requestMethod = httpRequest.getMethod();
        String result;
        if (requestMethod.isHttpMethod()) {
            result = generateHttp(httpRequest, gen);
        } else {
            result = "";
            System.out.print("Not support: " + requestMethod.getName());
        }
        return result;
    }

    public String generateHttp(HttpRequest httpRequest, String gen) {
        if (gen.isEmpty() || !HTTP_FRAMEWORKS.contains(gen)) {
            System.out.print("Please use " + String.join(", ", HTTP_FRAMEWORKS));
            return "";
        }
        String httpMethod = httpRequest.getMethod().getName();
        String url = httpRequest.getRequestTarget().getUri().toString();
        if (gen.contains("curl")) {
            String headers = httpRequest.getHeaders().stream()
                    .map(httpHeader -> "--header '" + httpHeader.getName() + ": " + httpHeader.getValue() + "'")
                    .collect(Collectors.joining(" "));
            String cliWithBody = "curl --request %s %s %s --data-binary @- <<BODY\n%s\nBODY\n";
            String cliWithoutBody = "curl --request %s %s %s";
            if (Objects.equals(httpMethod, "PUT") || Objects.equals(httpMethod, "POST")) {
                String body = new String(httpRequest.getBodyBytes(), StandardCharsets.UTF_8);
                return cliWithBody.formatted(httpMethod, url, headers, body);
            } else {
                return cliWithoutBody.formatted(httpMethod, url, headers);
            }
        } else if (gen.contains("fetch")) {
            @Language("JavaScript")
            String fetchCodeWithBody = """
                    async function doHttp() {
                        const headers = %s;
                        const body = `
                         %s
                        `;
                        const response = await fetch("%s", {
                            method: '%s',
                            headers,
                            body
                        });
                        return response.text();
                    }

                    const result = await doHttp();
                    console.log(result)
                    """;
            @Language("JavaScript")
            String fetchCodeWithoutBody = """
                    async function doHttp() {
                        const headers = %s;
                        const response = await fetch("%s", {
                            method: '%s',
                            headers
                        });
                        return response.text();
                    }
                               
                    const result = await doHttp();
                    console.log(result)
                    """;
            String headersJson = JsonUtils.writeValueAsString(httpRequest.getHeadersMap());
            if (Objects.equals(httpMethod, "PUT") || Objects.equals(httpMethod, "POST")) {
                String body = new String(httpRequest.getBodyBytes(), StandardCharsets.UTF_8);
                return fetchCodeWithBody.formatted(headersJson, body, url, httpMethod);
            } else {
                return fetchCodeWithoutBody.formatted(headersJson, url, httpMethod);
            }
        } else if (gen.contains("okhttp")) {
            String headers = httpRequest.getHeaders().stream()
                    .map(httpHeader -> ".set(\"" + httpHeader.getName() + "\",\"" + httpHeader.getValue() + "\")")
                    .collect(Collectors.joining());
            String headerBuilder = "new Headers.Builder()" + headers + ".build();";
            @Language("JAVA")
            String okhttp3CodeWithBody = """
                    ///usr/bin/env jbang "$0" "$@" ; exit $?
                    //JAVA 17
                    //DEPS com.squareup.okhttp3:okhttp:3.14.9
                                       
                    package http;
                                       
                    import okhttp3.*;
                                       
                    public class HelloOkHttp3 {
                        public static void main(String... args) throws Exception {
                            doHttp();
                        }
                                       
                        /**
                         * For more please visit https://square.github.io/okhttp/recipes/
                         */
                        public static void doHttp() throws Exception {
                            OkHttpClient httpClient = new OkHttpClient();
                            Headers headers = %s;
                            String body = ""\"
                                    %s
                                    ""\";
                            RequestBody requestBody = RequestBody.create(MediaType.get("application/json; charset=utf-8"), body);
                            Request request = new Request.Builder()
                                    .url("%s")
                                    .headers(headers)
                                    .%s(requestBody)
                                    .build();
                            try (Response response = httpClient.newCall(request).execute()) {
                                System.out.println(response.body().string());
                            }
                        }
                    }
                    """;
            @Language("JAVA")
            String okhttp3CodeWithoutBody = """
                    ///usr/bin/env jbang "$0" "$@" ; exit $?
                    //JAVA 17
                    //DEPS com.squareup.okhttp3:okhttp:3.14.9
                                       
                    package http;
                                       
                    import okhttp3.*;
                                       
                    public class HelloOkHttp3 {
                        public static void main(String... args) throws Exception {
                            doHttp();
                        }
                                       
                        /**
                         * For more please visit https://square.github.io/okhttp/recipes/
                         */
                        public static void doHttp() throws Exception {
                            OkHttpClient httpClient = new OkHttpClient();
                            Headers headers = %s;
                            Request request = new Request.Builder()
                                    .url("%s")
                                    .headers(headers)
                                    .%s()
                                    .build();
                            try (Response response = httpClient.newCall(request).execute()) {
                                System.out.println(response.body().string());
                            }
                        }
                    }
                    """;
            if (Objects.equals(httpMethod, "PUT") || Objects.equals(httpMethod, "POST")) {
                String body = new String(httpRequest.getBodyBytes(), StandardCharsets.UTF_8);
                return okhttp3CodeWithBody.formatted(headerBuilder, body, url, httpMethod.toLowerCase());
            } else {
                return okhttp3CodeWithoutBody.formatted(headerBuilder, url, httpMethod.toLowerCase());
            }
        } else if (gen.contains("apache")) {
            return "https://hc.apache.org/httpcomponents-client-5.1.x/quickstart.html";
        } else if (gen.contains("webclient")) {
            return "https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-client";
        }
        return "";
    }
}
