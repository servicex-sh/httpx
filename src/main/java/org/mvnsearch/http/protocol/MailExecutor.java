package org.mvnsearch.http.protocol;

import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.SmtpRequest;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Properties;


public class MailExecutor extends HttpBaseExecutor {
    public List<byte[]> execute(HttpRequest httpRequest) {
        SmtpRequest smtpRequest = new SmtpRequest(httpRequest);
        if (!smtpRequest.isLegal()) {
            System.err.print("Format is not correct, and failed to send email!");
            return Collections.emptyList();
        }
        Properties prop = new Properties();
        prop.put("mail.smtp.host", smtpRequest.getHost());
        prop.put("mail.smtp.port", smtpRequest.getPort());
        prop.put("mail.smtp.socketFactory.port", smtpRequest.getPort());
        if (smtpRequest.isSsl()) {
            prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }
        Authenticator authenticator = null;
        final String authorization = smtpRequest.getAuthorization();
        if (authorization != null) {
            prop.put("mail.smtp.auth", "true");
            if (authorization.startsWith("Basic ")) {
                String pair = authorization.substring(authorization.indexOf(' ')).trim();
                if (!pair.contains(":")) { // base64 decode
                    pair = new String(Base64.getDecoder().decode(pair), StandardCharsets.UTF_8);
                }
                final String[] parts = pair.split(":");
                authenticator = new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(parts[0], parts[1]);
                    }
                };
            }
        }
        System.out.println("MAIL " + smtpRequest.getUrl());
        System.out.println();
        try {
            Session session = Session.getInstance(prop, authenticator);
            MimeMessage message = new MimeMessage(session);
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
            helper.setFrom(new InternetAddress(smtpRequest.getFrom()));
            helper.setTo(InternetAddress.parse(smtpRequest.getTo()));
            if (smtpRequest.getCc() != null) {
                helper.setCc(InternetAddress.parse(smtpRequest.getCc()));
            }
            if (smtpRequest.getBcc() != null) {
                helper.setBcc(InternetAddress.parse(smtpRequest.getBcc()));
            }
            helper.setSubject(smtpRequest.getSubject());
            String body = smtpRequest.body();
            if (smtpRequest.getContentType().equals("text/html")) {
                helper.setText(body, true);
            } else {
                message.setText(body);
            }
            Transport.send(message);
            System.out.print("Succeeded to send email!");
        } catch (MessagingException e) {
            System.err.print("Failed to send email: " + e.getMessage());
        }
        return Collections.emptyList();
    }

}
