package org.mvnsearch.http.protocol;

import org.mvnsearch.http.model.HttpHeader;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.utils.JsonUtils;

import java.net.URI;
import java.util.*;


public class ChatGPTExecutor extends HttpExecutor {
    public List<byte[]> execute(HttpRequest httpRequest) {
        // url
        String url = httpRequest.getRequestTarget().getUri().toString();
        if (url.isEmpty()) {
            url = "https://api.openai.com/v1/chat/completions";
            httpRequest.getRequestTarget().setUri(URI.create(url));
        }
        // body
        String mdBody = httpRequest.bodyText();
        Map<String, Object> chatRequest = new HashMap<>();
        chatRequest.put("model", httpRequest.getHeader("X-Model", "gpt-3.5-turbo"));
        chatRequest.put("temperature", Double.parseDouble(httpRequest.getHeader("X-Temperature", "1")));
        chatRequest.put("messages", convertMdToMessages(mdBody));
        httpRequest.setBodyBytes(JsonUtils.writeValueAsBytes(chatRequest));
        // clean headers
        final List<HttpHeader> headers = new ArrayList<>(httpRequest.getHeaders());
        headers.removeIf(header -> header.getName().startsWith("X-") || header.getName().startsWith("Content-Type"));
        if (httpRequest.getHeader("Authorization") == null) {
            final String openAIToken = System.getenv("OPENAI_API_KEY");
            if (openAIToken == null) {
                System.err.println("Please set OPENAI_API_KEY environment variable");
                return Collections.emptyList();
            } else {
                headers.add(new HttpHeader("Authorization", "Bearer " + openAIToken));
            }
        }
        headers.add(new HttpHeader("Content-Type", "application/json"));
        httpRequest.setHeaders(headers);
        //execute http chatRequest
        final List<byte[]> result = super.execute(httpRequest);
        if (result.size() > 0 && httpRequest.getHeader("Accept") == null) {
            String json = new String(result.get(0));
            try {
                Map<String, Object> response = JsonUtils.readValue(json, Map.class);
                if (response.containsKey("choices")) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                    if (choices.size() > 0) {
                        Map<String, Object> choice = choices.get(0);
                        if (choice.containsKey("message")) {
                            String text = ((Map<String, String>) choice.get("message")).get("content");
                            System.out.println("\n");
                            System.out.println("=================ChatGPT Answer==================");
                            System.out.println("\n");
                            System.out.println(text);
                        }
                    }
                }
            } catch (Exception ignore) {

            }
        }
        return result;
    }

    List<Map<String, String>> convertMdToMessages(String mdText) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", mdText));
        return messages;
    }

}
