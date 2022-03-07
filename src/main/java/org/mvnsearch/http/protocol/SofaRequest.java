package org.mvnsearch.http.protocol;

import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.HessianSerializerOutput;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class SofaRequest implements Serializable {
    private String targetServiceUniqueName;
    private String methodName;
    private String[] methodArgSigs;
    private Map<String, Object> requestProps;
    private transient Object[] stubArguments;

    public SofaRequest(String serviceName, String methodName, @NotNull String[] methodArgSigs, @NotNull Object[] arguments) {
        this.targetServiceUniqueName = serviceName + ":1.0";
        this.methodName = methodName;
        this.methodArgSigs = methodArgSigs;
        this.requestProps = new HashMap<>();
        this.requestProps.put("protocol", "bolt");
        this.requestProps.put("type", "sync");
        this.requestProps.put("generic.revise", "true");
        this.requestProps.put("sofa_head_generic_type", "0");
        this.stubArguments = arguments;
    }

    public String getTargetServiceUniqueName() {
        return targetServiceUniqueName;
    }

    public void setTargetServiceUniqueName(String targetServiceUniqueName) {
        this.targetServiceUniqueName = targetServiceUniqueName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String[] getMethodArgSigs() {
        return methodArgSigs;
    }

    public void setMethodArgSigs(String[] methodArgSigs) {
        this.methodArgSigs = methodArgSigs;
    }

    public Map<String, Object> getRequestProps() {
        return requestProps;
    }

    public void setRequestProps(Map<String, Object> requestProps) {
        this.requestProps = requestProps;
    }

    public byte[] content() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Hessian2Output out = new HessianSerializerOutput(bos);
        out.writeObject(this);
        if (stubArguments != null) {
            for (Object argument : stubArguments) {
                out.writeObject(argument);
            }
        }
        out.flush();
        return bos.toByteArray();
    }

    public byte[] frameHeaderBytes(int contentLength) {
        /*
         * ver: version for protocol
         * type: request/response/request oneway
         * cmdcode: code for remoting command
         * ver2:version for remoting command
         * requestId: id of request
         * codec: code for codec
         * (req)timeout: request timeout.
         * (resp)respStatus: response status
         * classLen: length of request or response class name
         * headerLen: length of header
         * cotentLen: length of content
         * className
         * header
         * content
         */
        byte[] clazz = "com.alipay.sofa.rpc.core.request.SofaRequest".getBytes(StandardCharsets.UTF_8);
        byte[] headers = encode(sofaRpcHeaders());
        ByteBuffer bb = ByteBuffer.allocate(22 + clazz.length + headers.length);
        bb.put((byte) 0x01); //protocol
        bb.put((byte) 0x01); //type - req/resp
        bb.putShort((short) 0x0001); //cmd code - RPC_REQUEST:1
        bb.put((byte) 0x01); //version  0x1
        bb.putInt(1); //requestId
        bb.put((byte) 0x01);  //codec - hessian
        bb.putInt(0x00000BB8);  //request timeout
        bb.putShort((short) clazz.length);  //class Len
        bb.putShort((short) headers.length);  //header Len
        bb.putInt(contentLength);  //content Len
        bb.put(clazz); // className
        bb.put(headers); // headers
        return bb.array();
    }


    public byte[] encode(Map<String, String> map) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && value != null) {
                    writeString(bos, key);
                    writeString(bos, value);
                }
            }
            bos.flush();
            return bos.toByteArray();
        } catch (Exception ignore) {
            return new byte[]{};
        }
    }

    private Map<String, String> sofaRpcHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("service", targetServiceUniqueName);
        headers.put("sofa_head_target_service", targetServiceUniqueName);
        headers.put("sofa_head_method_name", methodName);
        headers.put("protocol", "bolt");
        headers.put("type", "sync");
        headers.put("generic.revise", "true");
        headers.put("sofa_head_generic_type", "0");
        return headers;
    }

    protected void writeString(OutputStream out, String str) throws IOException {
        if (str.isEmpty()) {
            writeInt(out, 0);
        } else {
            byte[] bs = str.getBytes(StandardCharsets.UTF_8);
            writeInt(out, bs.length);
            out.write(bs);
        }
    }

    private void writeInt(OutputStream out, int i) throws IOException {
        out.write((byte) (i >> 24));
        out.write((byte) (i >> 16));
        out.write((byte) (i >> 8));
        out.write(i);
    }
}
