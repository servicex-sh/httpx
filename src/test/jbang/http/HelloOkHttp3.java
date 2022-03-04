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
        Headers headers = new Headers.Builder().set("Content-Type","application/json").build();
        String body = """
                {}
                """;
        RequestBody requestBody = RequestBody.create(MediaType.get("application/json; charset=utf-8"), body);
        Request request = new Request.Builder()
                .url("https://httpbin.org/post")
                .headers(headers)
                .post(requestBody)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            System.out.println(response.body().string());
        }
    }
}
