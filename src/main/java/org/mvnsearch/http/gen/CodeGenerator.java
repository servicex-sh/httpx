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
        if (gen.contains("curl")) {
            String url = httpRequest.getRequestTarget().getUri().toString();
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
            String url = httpRequest.getRequestTarget().getUri().toString();
            String headersJson = JsonUtils.writeValueAsString(httpRequest.getHeadersMap());
            if (Objects.equals(httpMethod, "PUT") || Objects.equals(httpMethod, "POST")) {
                String body = new String(httpRequest.getBodyBytes(), StandardCharsets.UTF_8);
                return fetchCodeWithBody.formatted(headersJson, body, url, httpMethod);
            } else {
                return fetchCodeWithoutBody.formatted(headersJson, url, httpMethod);
            }
        } else if (gen.contains("okhttp")) {
            @Language("JAVA")
            String okhttp3Code = """
                    ///usr/bin/env jbang "$0" "$@" ; exit $?
                    //DEPS com.squareup.okhttp3:okhttp:3.14.9
                                    
                    import okhttp3.*;
                                    
                    public class HelloOkHttp3 {
                        public static void main(String... args) throws Exception {
                            httpPost();
                        }
                                    
                        /**
                         * For more please visit https://square.github.io/okhttp/recipes/
                         */
                        public static void httpPost() throws Exception {
                            OkHttpClient httpClient = new OkHttpClient();
                            RequestBody body = RequestBody.create(MediaType.get("application/json; charset=utf-8"), "{}");
                            Request request = new Request.Builder()
                                    .url("https://httpbin.org/post")
                                    .header("demo", "xxx")
                                    .post(body)
                                    .build();
                            try (Response response = httpClient.newCall(request).execute()) {
                                System.out.println(response.body().string());
                            }
                        }
                    }
                    """;
            return okhttp3Code;
        }
        return "";
    }
}
