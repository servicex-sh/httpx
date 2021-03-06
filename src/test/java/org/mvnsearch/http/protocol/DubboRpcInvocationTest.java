package org.mvnsearch.http.protocol;

import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.HessianSerializerOutput;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

public class DubboRpcInvocationTest {

    @Test
    public void testBytesOutput() throws Exception {
        DubboRpcInvocation invocation = new DubboRpcInvocation("GreetingsService", "sayHi",
                new String[]{"java.lang.String"}, new Object[]{"Jackie"});
        final byte[] contentBytes = invocation.toBytes();
        final byte[] headerBytes = invocation.frameHeaderBytes(0L, contentBytes.length);
        for (int i = 0; i < headerBytes.length; i++) {
            System.out.printf("%02X ", headerBytes[i]);
        }
        for (int i = 0; i < contentBytes.length; i++) {
            if (i % 16 == 0) {
                System.out.println();
            }
            System.out.printf("%02X ", contentBytes[i]);
        }
    }

    @Test
    public void testHessian() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Hessian2Output out = new HessianSerializerOutput(bos);
        out.writeString("hi, Jackie");
        out.flush();
        byte[] bytes = bos.toByteArray();
        for (int i = 0; i < bytes.length; i++) {
            System.out.printf("%02X ", bytes[i]);
        }
    }

}
