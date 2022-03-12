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
import java.util.Objects;

public class AliyunExecutor implements BaseExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(AliyunExecutor.class);

    public List<byte[]> execute(HttpRequest httpRequest) {
        try {
            final URI requestUri = httpRequest.getRequestTarget().getUri();
            String host = requestUri.getHost();
            //resolve region id from X-Region-Id header or host name
            String regionId = httpRequest.getHeader("X-Region-Id");
            if (regionId == null) { //resolve region id from host
                String tempRegionId = host.replace(".aliyuncs.com", "");
                if (tempRegionId.contains(".")) {
                    tempRegionId = tempRegionId.substring(tempRegionId.indexOf(".") + 1);
                } else if (tempRegionId.contains("-")) {
                    tempRegionId = tempRegionId.substring(tempRegionId.indexOf("-") + 1);
                }
                if (Aliyun.regions().contains(tempRegionId)) {
                    regionId = tempRegionId;
                }
            }
            if (regionId == null) {
                regionId = "cn-hangzhou";
            }
            String[] keyIdAndSecret = Aliyun.readAliyunAccessToken(httpRequest);
            if (keyIdAndSecret == null) {
                log.error("HTX-300-401");
                return Collections.emptyList();
            }
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
            String format = "JSON";
            if (queries.containsKey("Format")) {
                format = queries.get("Format");
            } else {
                final String acceptHeader = httpRequest.getHeader("Accept");
                if (acceptHeader != null && acceptHeader.contains("xml")) {
                    format = "XML";
                }
            }
            request.putQueryParameter("Format", format);
            CommonResponse response = client.getCommonResponse(request);
            final Map<String, String> sysHeaders = response.getHttpResponse().getSysHeaders();
            String contentType = Objects.equals(format, "JSON") ? "application/json" : "application/xml";
            String text = response.getData();
            System.out.print(prettyJsonFormatWithJsonPath(text, httpRequest.getHeader("X-JSON-PATH")));
            runJsTest(httpRequest, response.getHttpStatus(), sysHeaders, contentType, text);
            return List.of(text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("HTX-101-500", e);
        }
        return Collections.emptyList();
    }

}
