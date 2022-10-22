package org.mvnsearch.http.model;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.vendor.Nodejs;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class HttpRequestParser {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(HttpRequestParser.class);

    public static List<HttpRequest> splitRequests(String httpFileCode) {
        List<HttpRequest> requests = new ArrayList<>();
        try {
            final BufferedReader bufferedReader = new BufferedReader(new StringReader(httpFileCode));
            List<String> lines = bufferedReader.lines().toList();
            int index = 1;
            int lineNumber = 1;
            //remove shebang
            if (lines.get(0).startsWith("#!/usr/bin/env")) {
                lines = lines.subList(1, lines.size());
                lineNumber++;
            }
            HttpRequest httpRequest = new HttpRequest(index);
            for (String rawLine : lines) {
                String line = rawLine.trim();
                //noinspection StatementWithEmptyBody
                if (!httpRequest.isFilled() && line.isEmpty()) {  // ignore empty lines before http request

                } else if (line.startsWith("###")) { // comment for httpRequest or new HttpRequest separator
                    String comment = line.substring(3).trim();
                    if (!httpRequest.isFilled()) { // fill information for current httpRequest
                        httpRequest.setComment(line.substring(3).trim());
                    } else {  // start new httpRequest
                        requests.add(httpRequest);
                        index = index + 1;
                        httpRequest = new HttpRequest(index);
                        httpRequest.setComment(comment);
                    }
                } else if (!httpRequest.isRequestStarted()) {
                    if ((line.startsWith("#") || line.startsWith("//"))) { //comment
                        String comment = (line.startsWith("#") ? line.substring(1) : line.substring(2)).trim();
                        if (comment.startsWith("@")) { // tag for httpRequest
                            String tag = comment.substring(1);
                            String[] parts = tag.split("[=\\s]+", 2);
                            if (parts[0].equals("name") && parts.length > 1) {
                                httpRequest.setName(parts[1].trim());
                            }
                            httpRequest.addTag(tag);
                        }
                    } else if (HttpMethod.isRequestLine(line)) {   // normal comment
                        int position = line.indexOf(' ');
                        final String method = line.substring(0, position);
                        httpRequest.setMethod(HttpMethod.valueOf(method));
                        httpRequest.setRequestLine(line.substring(position + 1));
                        httpRequest.addRequestLine(rawLine);
                    } else {
                        httpRequest.addPreScriptLine(line);
                    }
                } else {
                    httpRequest.addRequestLine(rawLine);
                }
                httpRequest.addLineNumber(lineNumber);
                lineNumber++;
            }
            if (httpRequest.isFilled()) {  //add last httpRequest
                requests.add(httpRequest);
            }
        } catch (Exception e) {
            log.error("HTX-002-500", e);
        }
        return requests;
    }

    public static void parse(HttpRequest httpRequest, Map<String, Object> context) {
        try {
            Map<String, Object> newContext = new HashMap<>(context);
            //clean pre script
            final List<String> preScriptLines = httpRequest.getPreScriptLines();
            if (preScriptLines != null && !preScriptLines.isEmpty()) {
                String scriptCode = StringUtils.join(preScriptLines, "\n");
                int offsetStart = scriptCode.indexOf("< {%");
                int offsetEnd = scriptCode.lastIndexOf("%}");
                if (offsetEnd > offsetStart && offsetStart >= 0) {
                    httpRequest.setPreScriptCode(scriptCode.substring(offsetStart + 4, offsetEnd).trim());
                }
            }
            // execute pre script and inject context variables
            if (httpRequest.getPreScriptCode() != null) {
                final String jsTestOutput = Nodejs.executePreScriptCode(httpRequest.getPreScriptCode());
                if (!jsTestOutput.isEmpty()) {
                    final BufferedReader bufferedReader = new BufferedReader(new StringReader(jsTestOutput));
                    List<String> outputLines = bufferedReader.lines().toList();
                    for (String outputLine : outputLines) {
                        if (outputLine.startsWith("__variable:")) {
                            final String[] variable = outputLine.substring(outputLine.indexOf(":") + 1).split(",", 2);
                            newContext.put(variable[0], variable[1]);
                        }
                    }
                }
            }
            // replace variables and parse request
            final BufferedReader bufferedReader = new BufferedReader(new StringReader(replaceVariables(httpRequest.getRequestCode(), newContext)));
            List<String> lines = bufferedReader.lines().toList();
            int offset = 0;
            String requestLine = null;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (!line.isEmpty() && HttpMethod.isRequestLine(line)) {
                    requestLine = lines.get(i);
                    offset = i;
                }
            }
            // reset request line
            if (requestLine != null) {
                int position = requestLine.indexOf(' ');
                final String method = requestLine.substring(0, position);
                httpRequest.setMethod(HttpMethod.valueOf(method));
                httpRequest.setRequestLine(requestLine.substring(position + 1));
            }
            for (String rawLine : lines.subList(offset + 1, lines.size())) {
                String line = rawLine.trim();
                if (!httpRequest.isBodyStarted()) {
                    if ((rawLine.startsWith("  ") || rawLine.startsWith("\t"))) { // append request line parts in multi lines
                        httpRequest.appendRequestLine(line);
                    } else if (line.indexOf(':') > 0 && !httpRequest.isBodyStarted()) { //http request headers parse: body should be empty
                        int position = line.indexOf(':');
                        final String name = line.substring(0, position).trim();
                        if (name.contains(" ")) {
                            httpRequest.addBodyLine(rawLine);
                            httpRequest.setBodyStarted(true);
                        } else {
                            httpRequest.addHttpHeader(name, line.substring(position + 1).trim());
                        }
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
        } catch (Exception e) {
            log.error("HTX-002-500", e);
        }
    }

    public static List<HttpRequest> parse(String httpFileCode, Map<String, Object> context) {
        final List<HttpRequest> request = splitRequests(httpFileCode);
        for (HttpRequest httpRequest : request) {
            parse(httpRequest, context);
        }
        return request;
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

