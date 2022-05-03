package org.mvnsearch.http.protocol;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.jetbrains.annotations.Nullable;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpRequest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class SSHExecutor implements BaseExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(SSHExecutor.class);

    @Override
    public List<byte[]> execute(HttpRequest httpRequest) {
        var sshURI = httpRequest.getRequestTarget().getUri();
        System.out.println("SSH " + sshURI);
        System.out.println();
        Session session = null;
        ChannelExec channel = null;
        try {
            final JSch jsch = new JSch();
            int sshPort = sshURI.getPort();
            if (sshPort <= 0) {
                sshPort = 22;
            }
            String userName = null;
            String password = null;
            String userInfo = sshURI.getUserInfo();
            if (userInfo != null && !userInfo.isEmpty()) {
                if (userInfo.contains(":")) {
                    final String[] parts = userInfo.split(":", 2);
                    userName = parts[0];
                    password = parts[1];
                } else {
                    userName = userInfo;
                }
            }
            final @Nullable String[] basicAuthorization = httpRequest.getBasicAuthorization();
            if (basicAuthorization != null) { //username and password login
                userName = basicAuthorization[0];
                password = basicAuthorization[1];
            }
            if (userName != null && password != null) { //login by username and password
                session = jsch.getSession(userName, sshURI.getHost(), sshPort);
                session.setPassword(password);
            } else { //login by private key
                String privateKey = httpRequest.getHeader("X-SSH-Private-Key");
                if (privateKey != null) {
                    if (!new File(privateKey).exists()) {
                        System.out.println("Failed to load SSH private key: " + privateKey);
                        return Collections.emptyList();
                    } else {
                        jsch.addIdentity(privateKey);
                    }
                } else { // load private keys from $HOME/.ssh/
                    final File sshDir = new File(System.getProperty("user.home"), ".ssh");
                    if (sshDir.exists()) {
                        final File[] privateFiles = sshDir.listFiles((dir, name) -> name.startsWith("id_") && !name.contains("."));
                        if (privateFiles != null && privateFiles.length > 0) {
                            for (File privateFile : privateFiles) {
                                jsch.addIdentity(privateFile.getAbsolutePath());
                            }
                        }
                    }
                }
                session = jsch.getSession(userName, sshURI.getHost(), sshPort);
            }
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(cleanUpCommands(httpRequest.bodyText()));
            channel.setErrStream(System.err);
            ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
            channel.setOutputStream(responseStream);
            channel.connect();
            while (channel.isConnected()) {
                Thread.sleep(100);
            }
            final byte[] content = responseStream.toByteArray();
            String output = responseStream.toString(StandardCharsets.UTF_8);
            if (httpRequest.getHeader("Accept", "text/plain").contains("json")) {
                System.out.println(prettyJsonFormat(output));
            } else {
                System.out.println(output);
            }
            return Collections.singletonList(content);
        } catch (Exception e) {
            log.error("HTX-109-500", sshURI, e);
        } finally {
            if (session != null) {
                session.disconnect();
            }
            if (channel != null) {
                channel.disconnect();
            }
        }
        return Collections.emptyList();
    }

    private String cleanUpCommands(String body) {
        StringBuilder builder = new StringBuilder();
        body.lines().forEach(rawLine -> {
            String line = rawLine.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                if (line.endsWith("\\")) { //concat next line
                    builder.append(rawLine, 0, rawLine.lastIndexOf('\\'));
                } else {
                    builder.append(rawLine).append("; ");
                }
            }
        });
        final String script = builder.toString();
        return script.substring(0, script.length() - 2);
    }

}
