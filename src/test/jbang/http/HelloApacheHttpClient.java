///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17
//DEPS org.apache.httpcomponents.client5:httpclient5-fluent:5.1.3

package http;

import org.apache.hc.client5.http.fluent.Content;
import org.apache.hc.client5.http.fluent.Request;

import java.io.IOException;

public class HelloApacheHttpClient {
    public static void main(String... args) throws Exception {
        doHttp();
    }

    /**
     * For more please visit https://hc.apache.org/httpcomponents-client-5.1.x/quickstart.html
     */
    public static void doHttp() throws IOException {
        final Content result = Request.get("https://httpbin.org/ip")
                .execute()
                .returnContent();
        System.out.println(result.asString());
    }
}
