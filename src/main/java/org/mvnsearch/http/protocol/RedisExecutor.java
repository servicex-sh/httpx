package org.mvnsearch.http.protocol;

import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.utils.JsonUtils;
import redis.clients.jedis.Jedis;

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
            if (methodName.equals("SET")) {
                jedis.set(key, httpRequest.bodyText());
                System.out.print("Succeeded to set value!");
            } else if (methodName.equals("HMSET")) {
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
        } catch (Exception e) {
            log.error("HTX-105-500", redisUriAndKey.uri(), e);
        }
        return Collections.emptyList();
    }

}
