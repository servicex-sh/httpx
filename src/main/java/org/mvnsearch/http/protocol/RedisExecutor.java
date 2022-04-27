package org.mvnsearch.http.protocol;

import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.utils.JsonUtils;
import redis.clients.jedis.Jedis;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class RedisExecutor implements BasePubSubExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(RedisExecutor.class);

    public List<byte[]> execute(HttpRequest httpRequest) {
        String methodName = httpRequest.getMethod().getName();
        if (!httpRequest.isHostOrUriAvailable()) {
            httpRequest.addHttpHeader("Host", "127.0.0.1:6379");
        }
        URI redisURI = httpRequest.getRequestTarget().getUri();
        final UriAndSubject redisUriAndKey = getRedisUriAndChannel(redisURI, httpRequest);
        try (Jedis jedis = new Jedis(redisUriAndKey.uri())) {
            String key = redisUriAndKey.subject();
            switch (methodName) {
                case "RSET" -> {
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
                            final List<String> parts = Arrays.asList(requestLine.split("\\s+"));
                            int paramCount = Integer.parseInt(parts.get(0));
                            final List<String> keys = parts.subList(1, paramCount + 1);
                            final List<String> args = parts.subList(paramCount + 1, parts.size());
                            result = jedis.eval(luaScript, keys, args);
                        }
                        System.out.println(colorOutput("bold,green", "Succeeded to eval Lua script:"));
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
                case "LOAD" -> {
                    String luaScript = httpRequest.bodyText();
                    if (!luaScript.isEmpty()) {
                        String libName = httpRequest.getRequestLine();
                        if (libName.contains("/")) {
                            libName = libName.substring(libName.indexOf("/") + 1);
                        }
                        if (!luaScript.startsWith("#!lua")) {
                            luaScript = "#!lua name=" + libName + "\n" + luaScript;
                        }
                        String result = jedis.functionLoadReplace(luaScript);
                        if (result == null || !result.equals(libName)) {
                            System.out.println(colorOutput("bold,red", "Load Redis functions:"));
                            System.out.println("Failed to load Redis functions, please check your script.");
                        } else {
                            System.out.println(colorOutput("bold,green", "Succeed to load Redis functions:"));
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
