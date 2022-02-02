package org.mvnsearch.http.protocol;

import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.SmtpRequest;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.net.ssl.SSLSocketFactory;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class MailExecutor extends HttpBaseExecutor {
    public List<byte[]> execute(HttpRequest httpRequest) {
        SmtpRequest smtpRequest = new SmtpRequest(httpRequest);
        if (!smtpRequest.isLegal()) {
            System.err.print("Format is not correct, and failed to send email!");
            return Collections.emptyList();
        }
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        Properties prop = new Properties();
        prop.put("mail.transport.protocol", "smtp");
        prop.put("mail.smtp.host", smtpRequest.getHost());
        prop.put("mail.smtp.port", smtpRequest.getPort());
        prop.put("mail.smtp.socketFactory.port", smtpRequest.getPort());
        if (smtpRequest.getSchema() != null) {
            if (Objects.equals(smtpRequest.getSchema(), "ssl")) {
                SSLSocketFactory.getDefault();
                prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            } else if (Objects.equals(smtpRequest.getSchema(), "tls")) {
                prop.put("mail.smtp.starttls.enable", "true");
            }
        }
        final String authorization = smtpRequest.getAuthorization();
        if (authorization != null) {
            prop.put("mail.smtp.auth", "true");
            if (authorization.startsWith("Basic ")) {
                String pair = authorization.substring(authorization.indexOf(' ')).trim();
                if (!pair.contains(":")) { // base64 decode
                    pair = new String(Base64.getDecoder().decode(pair), StandardCharsets.UTF_8);
                }
                final String[] parts = pair.split(":");
                mailSender.setUsername(parts[0]);
                mailSender.setPassword(parts[1]);
            }
        }
        System.out.println("MAIL " + smtpRequest.getUrl());
        System.out.println();
        try {
            mailSender.setJavaMailProperties(prop);
            final MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
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
                helper.setText(body);
            }
            mailSender.send(mimeMessage);
            System.out.print("Succeeded to send email!");
        } catch (MessagingException e) {
            e.printStackTrace();
            System.err.print("Failed to send email: " + e.getMessage());
        }
        return Collections.emptyList();
    }

}
