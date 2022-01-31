package org.mvnsearch.http.protocol;

import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.HessianSerializerOutput;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class DubboRpcInvocation {
    private String serviceName;
    private String methodName;
    private final String methodStub = "$invoke";
    private final String parameterTypesDesc = "Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;";
    private Object[] arguments = new Object[3];
    private Map<String, String> attachments = new HashMap<>();

    public DubboRpcInvocation(String serviceName, String methodName) {
        this.serviceName = serviceName;
        this.methodName = methodName;
        this.arguments[0] = methodName;
        this.arguments[1] = new String[]{"java.lang.String"};  // method signature
        this.arguments[2] = new Object[]{"Jackie"}; // real arguments
        this.attachments.put("path", serviceName);
        this.attachments.put("remote.application", "ConsumerTest");
        this.attachments.put("interface", serviceName);
        this.attachments.put("version", "0.0.0");
        this.attachments.put("generic", "gson");
    }


    public byte[] toBytes() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Hessian2Output out = new HessianSerializerOutput(bos);
        out.writeString("2.0.2");
        out.writeString(serviceName);
        out.writeString("0.0.0");
        out.writeString(methodStub);
        out.writeString(parameterTypesDesc);
        for (Object argument : arguments) {
            out.writeObject(argument);
        }
        out.writeObject(attachments);
        out.flush();
        return bos.toByteArray();
    }
}
