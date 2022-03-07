package org.mvnsearch.http.protocol;

import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpRequest;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;


public class ZeromqExecutor extends HttpBaseExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(ZeromqExecutor.class);

    public List<byte[]> execute(HttpRequest httpRequest) {
        final URI zeromqURI = httpRequest.getRequestTarget().getUri();
        String methodName = httpRequest.getMethod().getName();
        System.out.println(methodName + " " + zeromqURI);
        System.out.println();
        try (ZContext context = new ZContext()) {
            ZMQ.Socket requester = context.createSocket(SocketType.REQ);
            requester.connect("tcp://localhost:5555");
            requester.send(httpRequest.getBodyBytes());
            final String response = requester.recvStr();
            System.out.println(prettyJsonFormat(response));
            requester.close();
            return List.of(response.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("HTX-103-408", zeromqURI, e);
        }
        return Collections.emptyList();
    }


}
