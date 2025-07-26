package org.mvnsearch.http.aot;

import com.sun.mail.handlers.*;
import com.sun.mail.smtp.SMTPProvider;
import com.sun.mail.smtp.SMTPTransport;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class JavamailHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection().registerType(SMTPProvider.class);
        hints.reflection().registerType(SMTPTransport.class);
        hints.reflection().registerType(multipart_mixed.class);
        hints.reflection().registerType(text_plain.class);
        hints.reflection().registerType(text_html.class);
        hints.reflection().registerType(text_xml.class);
        hints.reflection().registerType(message_rfc822.class);
        hints.resources().registerPattern("org/springframework/mail/javamail/mime.types");
        hints.resources().registerPattern("META-INF/mailcap");
        hints.resources().registerPattern("META-INF/javamail.*");
    }
}
