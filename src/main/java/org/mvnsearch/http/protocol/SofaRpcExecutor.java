package org.mvnsearch.http.protocol;

import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.HessianSerializerInput;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.utils.JsonUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class SofaRpcExecutor extends HttpBaseExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(SofaRpcExecutor.class);

    @SuppressWarnings("unused")
    public List<byte[]> execute(HttpRequest httpRequest) {
        final URI sofaUri = httpRequest.getRequestTarget().getUri();
        final Map<String, String> params = queryToMap(sofaUri);
        String methodSignature = params.get("method");
        String serviceName = sofaUri.getPath().substring(1);
        if (serviceName.contains("/")) {
            final String[] parts = serviceName.split("/", 2);
            serviceName = parts[0];
            methodSignature = parts[1];
        }
        String methodName = methodSignature;
        String[] paramsTypeArray = new String[]{};
        Object[] arguments = new Object[]{};
        if (methodSignature.contains("(")) {
            methodName = methodSignature.substring(0, methodSignature.indexOf('('));
            final String parts = methodSignature.substring(methodSignature.indexOf('(') + 1, methodSignature.indexOf(')'));
            if (!parts.isEmpty()) {
                paramsTypeArray = parts.split(",");
            }
        }
        if (paramsTypeArray.length > 0) {
            final byte[] body = httpRequest.getBodyBytes();
            if (body == null || body.length == 0) {
                arguments = new Object[]{null};
            } else {
                String text = new String(body, StandardCharsets.UTF_8);
                if (!text.startsWith("[")) { // one object  and convert to array
                    text = "[" + text + "]";
                }
                try {
                    final List<?> items = JsonUtils.readValue(text, List.class);
                    arguments = new Object[items.size()];
                    for (int i = 0; i < items.size(); i++) {
                        final Object item = items.get(i);
                        arguments[i] = item;
                    }
                } catch (Exception e) {
                    log.error("HTX-103-500", text);
                }
            }
        }
        int port = sofaUri.getPort();
        if (port <= 0) {
            port = 12200;
        }
        System.out.println("SOFA " + sofaUri);
        System.out.println();
        try (SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(sofaUri.getHost(), port))) {
            SofaRpcInvocation invocation = new SofaRpcInvocation(serviceName, methodName, paramsTypeArray, arguments);
            final byte[] contentBytes = invocation.content();
            final byte[] frameBytes = invocation.frameBytes(contentBytes);
            socketChannel.write(ByteBuffer.wrap(frameBytes));
            final byte[] receivedBytes = extractData(socketChannel);
            final ByteBuf in = Unpooled.wrappedBuffer(receivedBytes);
            final byte protocol = in.readByte();
            byte type = in.readByte();
            short cmdCode = in.readShort();
            byte ver2 = in.readByte();
            int requestId = in.readInt();
            byte serializer = in.readByte();
            short status = in.readShort();
            short classLen = in.readShort();
            short headerLen = in.readShort();
            int contentLen = in.readInt();
            byte[] clazz = new byte[classLen];
            byte[] header = new byte[headerLen];
            byte[] content = new byte[contentLen];
            in.readBytes(clazz);
            in.readBytes(header);
            in.readBytes(content);
            Hessian2Input input = new HessianSerializerInput(new ByteArrayInputStream(content));
            final SofaResponse response = (SofaResponse) input.readObject();
            if (!response.isError()) {
                final Object appResponse = response.getAppResponse();
                if (appResponse == null) {
                    System.out.print("===No return value===");
                } else {
                    String text = JsonUtils.writeValueAsPrettyString(appResponse);
                    System.out.print(prettyJsonFormat(text));
                    return List.of(text.getBytes(StandardCharsets.UTF_8));
                }
            } else {
                System.out.println("Error:" + response.getErrorMsg());
            }
        } catch (Exception e) {
            log.error("HTX-103-408", sofaUri, e);
        }
        return Collections.emptyList();
    }


    public byte[] extractData(SocketChannel socketChannel) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteBuffer buf = ByteBuffer.allocate(1024);
        int readCount;
        do {
            readCount = socketChannel.read(buf);
            bos.write(buf.array(), 0, readCount);
        } while (readCount == 1024);
        return bos.toByteArray();
    }

}
