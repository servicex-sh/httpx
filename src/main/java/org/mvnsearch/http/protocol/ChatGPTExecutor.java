package org.mvnsearch.http.protocol;

import org.mvnsearch.http.model.HttpHeader;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.utils.JsonUtils;

import java.net.URI;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;


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
            String openAIToken = httpRequest.getHeader("X-OPENAI-API-KEY");
            if (openAIToken == null) {
                openAIToken = httpRequest.getHeader("X-OPENAI_API_KEY");
            }
            if (openAIToken == null) {
                openAIToken = System.getenv("OPENAI_API_KEY");
            }
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
        if (result.size() > 0) {
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
                            System.out.println();
                            System.out.println(text.trim());
                        }
                    }
                }
            } catch (Exception ignore) {

            }
        }
        return result;
    }

    List<Map<String, String>> convertMdToMessages(String mdText) {
        String userMsgContent = mdText;
        List<Map<String, String>> messages = new ArrayList<>();
        // system message
        Pattern systemMsgPattern = Pattern.compile("(\\S.+\\n)*.+\\{\\.system}");
        final Optional<MatchResult> systemMsgFound = systemMsgPattern.matcher(userMsgContent).results().findFirst();
        if (systemMsgFound.isPresent()) {
            String matchedText = systemMsgFound.get().group();
            userMsgContent = userMsgContent.replace(matchedText, "").trim();
            String systemMsgContent = matchedText.replace("{.system}", "").trim();
            messages.add(Map.of("role", "system", "content", systemMsgContent));
        }
        // assistant messages
        List<Map<String, String>> assistantMessages = new ArrayList<>();
        Pattern assistantMsgPattern = Pattern.compile("(\\S.+\\n)*.+\\{\\.assistant}");
        final List<MatchResult> matchResults = assistantMsgPattern.matcher(userMsgContent).results().toList();
        for (MatchResult matchResult : matchResults) {
            String matchedText = matchResult.group();
            userMsgContent = userMsgContent.replace(matchedText, "").trim();
            String assistantMsgContent = matchedText.replace("{.assistant}", "").trim();
            assistantMessages.add(Map.of("role", "assistant", "content", assistantMsgContent));
        }
        // user message
        messages.add(Map.of("role", "user", "content", userMsgContent.trim()));
        // append assistant messages
        if (!assistantMessages.isEmpty()) {
            messages.addAll(assistantMessages);
        }
        return messages;
    }

}
