package org.mvnsearch.http.utils;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.github.skjolber.jackson.jsh.AnsiSyntaxHighlight;
import com.github.skjolber.jackson.jsh.DefaultSyntaxHighlighter;
import com.github.skjolber.jackson.jsh.SyntaxHighlighter;
import com.github.skjolber.jackson.jsh.SyntaxHighlightingJsonGenerator;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;


public class JsonColorFactory extends JsonFactory {

    public JsonGenerator createGenerator(Writer w) throws IOException {
        final JsonGenerator delegate = super.createGenerator(w);
        return new SyntaxHighlightingJsonGenerator(delegate, highlighter(), true);
    }

    public JsonGenerator createGenerator(OutputStream out, JsonEncoding enc) throws IOException {
        final JsonGenerator delegate = super.createGenerator(out, enc);
        return new SyntaxHighlightingJsonGenerator(delegate, highlighter(), true);
    }

    public static SyntaxHighlighter highlighter() {
        return DefaultSyntaxHighlighter.newBuilder()
                .withField(AnsiSyntaxHighlight.GREEN)
                .withString(AnsiSyntaxHighlight.YELLOW)
                .build();
    }
}
