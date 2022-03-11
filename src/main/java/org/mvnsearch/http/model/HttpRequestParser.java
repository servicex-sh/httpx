package org.mvnsearch.http.model;

import org.jetbrains.annotations.Nullable;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class HttpRequestParser {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(HttpRequestParser.class);

    public static List<HttpRequest> parse(String httpFileCode, Map<String, Object> context) {
        List<HttpRequest> requests = new ArrayList<>();
        try {
            final BufferedReader bufferedReader = new BufferedReader(new StringReader(replaceVariables(httpFileCode, context)));
            List<String> lines = bufferedReader.lines().toList();
            //remove shebang
            if (lines.get(0).startsWith("#!/usr/bin/env")) {
                lines = lines.subList(1, lines.size());
            }
            int index = 1;
            HttpRequest httpRequest = new HttpRequest(index);
            for (String rawLine : lines) {
                String line = rawLine.trim();
                //noinspection StatementWithEmptyBody
                if (!httpRequest.isFilled() && line.isEmpty()) {  // ignore empty lines before http request

                } else if (line.startsWith("###")) { // comment for httpRequest or new HttpRequest
                    String comment = line.substring(3).trim();
                    if (!httpRequest.isFilled()) { // fill information for current httpRequest
                        httpRequest.setComment(line.substring(3).trim());
                    } else {  // start new httpRequest
                        requests.add(httpRequest);
                        index = index + 1;
                        httpRequest = new HttpRequest(index);
                        httpRequest.setComment(comment);
                    }
                } else if (!httpRequest.isBodyStarted()) {
                    if ((line.startsWith("#") || line.startsWith("//")) && !httpRequest.isBodyStarted()) { //comment
                        if (line.contains("@")) { // tag for httpRequest
                            String tag = line.substring(line.indexOf("@") + 1);
                            String[] parts = tag.split("[=\\s]+", 2);
                            if (parts[0].equals("name")) {
                                httpRequest.setName(parts[1].trim());
                            }
                            httpRequest.addTag(tag);
                        } else {   // normal comment
                            if (httpRequest.getComment() == null) {
                                httpRequest.setComment(line.substring(2).trim());
                            }
                        }
                    } else if (HttpMethod.isRequestLine(line) && !httpRequest.isBodyStarted()) {  // request line parse
                        int position = line.indexOf(' ');
                        final String method = line.substring(0, position);
                        httpRequest.setMethod(HttpMethod.valueOf(method));
                        httpRequest.setRequestLine(line.substring(position + 1));
                    } else if ((rawLine.startsWith("  ") || rawLine.startsWith("\t")) && !httpRequest.isBodyStarted()) { // append request parts in multi lines
                        httpRequest.appendRequestLine(line);
                    } else if (line.indexOf(':') > 0 && !httpRequest.isBodyStarted()) { //http request headers parse: body should be empty
                        int position = line.indexOf(':');
                        httpRequest.addHttpHeader(line.substring(0, position), line.substring(position + 1).trim());
                    } else {
                        if (!line.isEmpty()) { // ignore lines between headers and body
                            httpRequest.addBodyLine(rawLine);
                        } else {
                            httpRequest.setBodyStarted(true);
                        }
                    }
                } else {  // parse httpRequest body
                    httpRequest.addBodyLine(rawLine);
                }
            }
            if (httpRequest.isFilled()) {  //add last httpRequest
                requests.add(httpRequest);
            }
        } catch (Exception e) {
            log.error("HTX-002-500", e);
        }
        return requests;
    }

    public static String replaceVariables(String httpFile, Map<String, Object> context) {
        int offset = httpFile.indexOf("{{");
        if (offset < 0) {
            return httpFile;
        }
        int length = httpFile.length();
        StringBuilder builder = new StringBuilder();
        if (offset > 0) {
            builder.append(httpFile, 0, offset);
        }
        offset = offset + 2;
        while (offset >= 0 && offset < length) {
            final int temp = httpFile.indexOf("}}", offset);
            // closure not found
            if (temp == -1) {
                break;
            }
            String name = httpFile.substring(offset, temp).trim();
            Object value;
            if (name.startsWith("$")) { // function
                value = evaluateFunction(name, context);
            } else {  // env variable
                value = context.get(name);
            }

            //append value from context
            builder.append(value == null ? "" : value);
            // find next variable
            offset = httpFile.indexOf("{{", temp);
            // have next variable
            if (offset > temp) {
                builder.append(httpFile, temp + 2, offset);
                offset = offset + 2;
            } else {  // no more variable
                builder.append(httpFile.substring(temp + 2));
                break;
            }
        }
        return builder.toString();
    }

    @Nullable
    public static String evaluateFunction(String functionExpression, Map<String, Object> context) {
        String functionName = functionExpression.substring(1);
        List<String> functionParams = new ArrayList<>();
        if (functionName.contains(" ")) { //contains functionParams
            functionName = functionName.substring(0, functionName.indexOf(' '));
            String paramsText = functionExpression.substring(functionExpression.indexOf(' ') + 1).trim();
            // fun1 %name text    fun1 `hello %{name}` %demo demo
            while (!paramsText.isEmpty()) {
                if (paramsText.startsWith("`") || paramsText.startsWith("\"") || paramsText.startsWith("'")) {
                    String quotation = paramsText.substring(0, 1);
                    final int offset = paramsText.indexOf(quotation, 1);
                    if (offset < 0) {
                        log.error("HTX-002-502", paramsText);
                        System.exit(-1);
                    }
                    String arg = paramsText.substring(0, offset + 1);
                    functionParams.add(arg);
                    paramsText = paramsText.substring(offset + 1).trim();
                } else {
                    final int offset = paramsText.indexOf(' ', 1);
                    if (offset > 0) {
                        String arg = paramsText.substring(0, offset);
                        functionParams.add(arg);
                        paramsText = paramsText.substring(offset + 1).trim();
                    } else {
                        functionParams.add(paramsText);
                        paramsText = "";
                    }
                }
            }
        }
        // clean function params
        String[] args = new String[functionParams.size()];
        for (int i = 0; i < functionParams.size(); i++) {
            String param = functionParams.get(i);
            if (param.startsWith("%")) {
                args[i] = context.getOrDefault(param.substring(1), "").toString();
            } else if (param.startsWith("`")) {
                final String template = param.substring(1, param.length() - 1);
                args[i] = evaluateTemplate(template, context);
            } else if (param.startsWith("\"") || param.startsWith("'")) {
                args[i] = param.substring(1, param.length() - 1);
            } else {
                args[i] = param;
            }
        }
        final Function<String[], String> httpFunction = HttpGlobalFunctions.getInstance().findFunction(functionName);
        if (httpFunction != null) {
            return httpFunction.apply(args);
        }
        return null;
    }

    public static String evaluateTemplate(String template, Map<String, Object> context) {
        if (!template.contains("%{")) {
            return template;
        }
        StringBuilder builder = new StringBuilder();
        int offset = template.indexOf("%{");
        if (offset > 0) {
            builder.append(template, 0, offset);
        }
        while (offset >= 0) {
            int index = template.indexOf("}", offset);
            if (index < 0) {
                log.error("HTX-002-503", template);
                System.exit(-1);
            }
            String name = template.substring(offset + 2, index);
            builder.append(context.getOrDefault(name, "").toString());
            offset = template.indexOf("%{", index);
            if (offset < 0) {
                builder.append(template.substring(index + 1));
            } else {
                builder.append(template, index + 1, offset);
            }
        }
        return builder.toString();
    }

}

