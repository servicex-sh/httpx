package org.mvnsearch.http.protocol;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.profile.DefaultProfile;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.vendor.Aliyun;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AliyunExecutor implements BaseExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(AliyunExecutor.class);

    public List<byte[]> execute(HttpRequest httpRequest) {
        try {
            final URI requestUri = httpRequest.getRequestTarget().getUri();
            String host = requestUri.getHost();
            String regionId = host.replace(".aliyuncs.com", "");
            if (regionId.contains(".")) {
                regionId = regionId.substring(regionId.indexOf(".") + 1);
            }
            String[] keyIdAndSecret = Aliyun.readAliyunAccessToken(httpRequest);
            DefaultProfile profile = DefaultProfile.getProfile(
                    regionId,
                    keyIdAndSecret[0],
                    keyIdAndSecret[1]);
            System.out.println("ALIYUN" + " " + requestUri);
            System.out.println();
            final Map<String, String> queries = queryToMap(requestUri);
            IAcsClient client = new DefaultAcsClient(profile);
            CommonRequest request = new CommonRequest();
            request.setSysDomain(host);
            request.setSysVersion(queries.get("Version"));
            request.setSysAction(queries.get("Action"));
            request.putQueryParameter("Format", queries.getOrDefault("Format", "JSON"));
            CommonResponse response = client.getCommonResponse(request);
            String text = response.getData();
            System.out.print(prettyJsonFormat(text));
            return List.of(text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("HTX-101-500", e);
        }
        return Collections.emptyList();
    }

}
