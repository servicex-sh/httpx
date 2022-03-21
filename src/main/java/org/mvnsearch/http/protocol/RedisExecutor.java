package org.mvnsearch.http.protocol;

import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.utils.JsonUtils;
import redis.clients.jedis.Jedis;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RedisExecutor implements BasePubSubExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(RedisExecutor.class);

    public List<byte[]> execute(HttpRequest httpRequest) {
        String methodName = httpRequest.getMethod().getName();
        final UriAndSubject redisUriAndKey = getRedisUriAndChannel(httpRequest.getRequestTarget().getUri(), httpRequest);
        try (Jedis jedis = new Jedis(redisUriAndKey.uri())) {
            String key = redisUriAndKey.subject();
            switch (methodName) {
                case "SET" -> {
                    jedis.set(key, httpRequest.bodyText());
                    System.out.print("Succeeded to set value!");
                }
                case "EVAL" -> {
                    String luaScript = httpRequest.bodyText();
                    if (!luaScript.isEmpty()) {
                        final String requestLine = httpRequest.getRequestLine();
                        Object result;
                        if (requestLine.isEmpty() || requestLine.equals("0")) {
                            result = jedis.eval(luaScript);
                        } else {
                            final String[] parts = requestLine.split("\\s+");
                            int paramCount = parts.length / 2;
                            String[] params = new String[paramCount];
                            for (int i = 0; i < paramCount; i++) {
                                params[i] = parts[i * 2 + 1];
                            }
                            result = jedis.eval(luaScript, paramCount - 1, params);
                        }
                        colorOutput("bold,green", "Succeeded to eval Lua script:");
                        if (result == null) {
                            System.out.println("Failed to execute Lua script, please check your script.");
                        } else if (result instanceof byte[]) {
                            System.out.println(new String((byte[]) result, StandardCharsets.UTF_8));
                        } else {
                            System.out.println(result);
                        }
                    } else {
                        System.out.println("No Lua script supplied!");
                    }
                }
                case "HMSET" -> {
                    String contentType = httpRequest.getHeader("Content-Type", "application/json");
                    if (contentType.equals("application/json")) {
                        final Map<String, ?> draftData = JsonUtils.readValue(httpRequest.getBodyBytes(), Map.class);
                        Map<String, String> hashData = new HashMap<>();
                        for (Map.Entry<String, ?> entry : draftData.entrySet()) {
                            final Object value = entry.getValue();
                            if (value instanceof String) {
                                hashData.put(entry.getKey(), (String) value);
                            } else {
                                hashData.put(entry.getKey(), value.toString());
                            }
                        }
                        jedis.hmset(key, hashData);
                        System.out.print("Succeeded to set Hash fields!");
                    } else {
                        System.out.print("Please set Content-Type to application/json for HSET!");
                    }
                }
            }
        } catch (Exception e) {
            log.error("HTX-105-500", redisUriAndKey.uri(), e);
        }
        return Collections.emptyList();
    }

}
