package org.mvnsearch.http.protocol;

import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.HessianSerializerOutput;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

public class SofaRpcInvocationTest {

    @Test
    public void testBytesOutput() throws Exception {
        SofaRpcInvocation invocation = new SofaRpcInvocation("org.mvnsearch.HelloService", "sayHello",
                new String[]{"java.lang.String"}, new Object[]{"world"});
        final byte[] contentBytes = invocation.content();
        final byte[] headerBytes = invocation.frameBytes(contentBytes);
        for (int i = 0; i < headerBytes.length; i++) {
            if (i % 16 == 0) {
                System.out.println();
            }
            if (i % 8 == 0 && i % 16 != 0) {
                System.out.print("- ");
            }
            System.out.printf("%02X ", headerBytes[i]);
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
