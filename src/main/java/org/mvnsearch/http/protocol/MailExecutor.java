package org.mvnsearch.http.protocol;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.SmtpRequest;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;


public class MailExecutor extends HttpBaseExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(MailExecutor.class);

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
        prop.put("mail.smtp.starttls.enable", "true");
        if (smtpRequest.getSchema() != null) {
            if (Objects.equals(smtpRequest.getSchema(), "ssl")) {
                prop.put("mail.smtp.socketFactory.class", "com.sun.mail.util.MailSSLSocketFactory");
                prop.put("mail.smtp.socketFactory.fallback", "false");
            }
        }
        final String authorization = smtpRequest.getAuthorization();
        if (authorization != null) {
            prop.put("mail.smtp.auth", "true");
            String[] parts = httpRequest.getBasicAuthorization();
            if (parts != null) {
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
            if (smtpRequest.getReplyTo() != null) {
                helper.setReplyTo(smtpRequest.getReplyTo());
            }
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
            log.error("HTX-104-500", e);
        }
        return Collections.emptyList();
    }

}
