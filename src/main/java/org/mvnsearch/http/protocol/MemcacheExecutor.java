package org.mvnsearch.http.protocol;

import com.spotify.folsom.ConnectFuture;
import com.spotify.folsom.MemcacheClient;
import com.spotify.folsom.MemcacheClientBuilder;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpRequest;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;


public class MemcacheExecutor extends HttpBaseExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(MemcacheExecutor.class);

    public List<byte[]> execute(HttpRequest httpRequest) {
        if (!httpRequest.isHostOrUriAvailable()) {
            httpRequest.addHttpHeader("Host", "localhost:11211");
        }
        final URI memcacheURI = httpRequest.getRequestTarget().getUri();
        String key = httpRequest.getRequestLine();
        String methodName = httpRequest.getMethod().getName();
        System.out.println(methodName + " " + memcacheURI);
        System.out.println();
        try {
            byte[] content = null;
            int port = memcacheURI.getPort();
            if (port <= 0) {
                port = 11211;
            }
            final MemcacheClient<byte[]> client = MemcacheClientBuilder.newByteArrayClient()
                    .withAddress(memcacheURI.getHost(), port)
                    .connectAscii();
            ConnectFuture.connectFuture(client).toCompletableFuture().get();
            final byte[] bodyBytes = httpRequest.getBodyBytes();
            if (bodyBytes != null && bodyBytes.length > 0) {  //set
                client.set(key, bodyBytes, 0).toCompletableFuture().get();
                System.out.print("Succeeded to set cache!");
            } else if (key.startsWith("-")) { //delete
                String realKey = key.substring(1);
                client.delete(realKey).toCompletableFuture().get();
                System.out.print("Succeeded to delete cache: " + realKey + "!");
            } else {  //get
                content = client.get(key).toCompletableFuture().get();
                if (content != null && content.length > 0) {
                    System.out.println(prettyJsonFormat(new String(content, StandardCharsets.UTF_8)));
                } else {
                    System.out.println("Failed to find cache: " + key);
                }
            }
            client.shutdown();
            if (content != null) {
                return List.of(content);
            } else {
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("HTX-108-408", memcacheURI, e);
        }
        return Collections.emptyList();
    }


}
