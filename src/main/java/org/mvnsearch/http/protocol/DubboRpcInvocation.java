package org.mvnsearch.http.protocol;

import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.HessianSerializerOutput;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class DubboRpcInvocation {
    private String serviceName;
    private String methodName;
    private final String methodStub = "$invoke";
    private final String parameterTypesDesc = "Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;";
    private Object[] stubArguments = new Object[3];
    private Map<String, String> attachments = new HashMap<>();

    public DubboRpcInvocation(String serviceName, String methodName, @NotNull String[] paramsTypeArray, @NotNull Object[] arguments) {
        this.serviceName = serviceName;
        this.methodName = methodName;
        this.stubArguments[0] = methodName;
        this.stubArguments[1] = paramsTypeArray;  // method signature
        this.stubArguments[2] = arguments; // real arguments
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
        for (Object argument : stubArguments) {
            out.writeObject(argument);
        }
        out.writeObject(attachments);
        out.flush();
        return bos.toByteArray();
    }

    public byte[] frameHeaderBytes(long messageId, int length) {
        //2byte magic:类似java字节码文件里的魔数，用来判断是不是dubbo协议的数据包。魔数是常量0xdabb
        //1byte 消息标志位:16-20序列id,21 event,22 two way,23请求或响应标识
        // 1byte 状态，当消息类型为响应时，设置响应状态。24-31位。状态位, 设置请求响应状态，dubbo定义了一些响应的类型。具体类型见com.alibaba.dubbo.remoting.exchange.Response
        //8byte 消息ID,long类型，32-95位。每一个请求的唯一识别id（由于采用异步通讯的方式，用来把请求request和返回的response对应上）
        //4byte 消息长度，96-127位。消息体 body 长度, int 类型，即记录Body Content有多少个字节。
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putShort((short) 0xDABB); //magic
        bb.put((byte) 0xC2); //mark
        bb.put((byte) 0x00);  //status
        bb.putLong(messageId);
        bb.putInt(length);
        return bb.array();
    }
}
