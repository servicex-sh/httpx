package org.mvnsearch.http.model;

import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;

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
            Object value = switch (name) {
                case "$uuid" -> UUID.randomUUID().toString();
                case "$timestamp" -> System.currentTimeMillis();
                case "$randomInt" -> new Random().nextInt(1000);
                case "$projectRoot" -> ".idea";
                case "$historyFolder" -> ".idea/httpRequests";
                default -> context.get(name);
            };
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

}

