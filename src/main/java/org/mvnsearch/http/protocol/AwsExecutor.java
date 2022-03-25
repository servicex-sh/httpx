package org.mvnsearch.http.protocol;

import org.jetbrains.annotations.Nullable;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpHeader;
import org.mvnsearch.http.model.HttpMethod;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.vendor.AWS;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.*;

public class AwsExecutor extends HttpExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(AwsExecutor.class);

    public List<byte[]> execute(HttpRequest httpRequest) {
        try {
            String method = httpRequest.getMethod().getName();
            final byte[] bodyBytes = httpRequest.getBodyBytes();
            if (HttpMethod.AWS_METHODS.contains(method)) {
                if (Objects.equals(method, "AWS")) {
                    method = "GET";
                } else {
                    method = method.substring(3);
                }
            }
            final URI requestUri = httpRequest.getRequestTarget().getUri();
            String host = requestUri.getHost();
            String serviceName = host.substring(0, host.indexOf('.'));
            //resolve region id from X-Region-Id header or host name
            String regionId = AWS.readRegionId(httpRequest);
            @Nullable String[] credential = AWS.readAwsAccessToken(httpRequest);
            if (credential == null || credential.length < 2) {
                log.error("HTX-301-401");
                return Collections.emptyList();
            }
            for (String item : credential) {
                if (item != null) {
                    // compatible VS Code humao.rest-client
                    if (item.startsWith("region:")) {
                        regionId = item.replace("region:", "");
                    } else if (item.startsWith("service:")) {
                        serviceName = item.replace("service:", "");
                    }
                }
            }
            final SdkHttpFullRequest.Builder requestBuilder = SdkHttpFullRequest.builder()
                    .uri(requestUri)
                    .method(SdkHttpMethod.valueOf(method));
            final Map<String, String> headers = httpRequest.getHeadersMap();
            headers.forEach((name, value) -> {
                if (!name.equalsIgnoreCase("Authorization")) {
                    requestBuilder.putHeader(name, value);
                }
            });
            if (!headers.containsKey("Host")) {
                requestBuilder.putHeader("Host", host);
            }
            // set default format: json
            if (!headers.containsKey("Accept")) {
                requestBuilder.putHeader("Accept", "application/json");
            }
            // set body
            if (bodyBytes != null && bodyBytes.length > 0) {
                requestBuilder.contentStreamProvider(() -> new ByteArrayInputStream(bodyBytes));
            }
            final Aws4SignerParams aws4SignerParams = Aws4SignerParams.builder()
                    .awsCredentials(AwsBasicCredentials.create(credential[0], credential[1]))
                    .signingRegion(Region.of(regionId))
                    .signingName(serviceName)
                    .build();
            final SdkHttpFullRequest awsRequest = requestBuilder.build();
            final SdkHttpFullRequest signedRequest = Aws4Signer.create().sign(awsRequest, aws4SignerParams);
            List<HttpHeader> newHeaders = new ArrayList<>();
            signedRequest.headers().forEach((name, values) -> {
                newHeaders.add(new HttpHeader(name, values.get(0)));
            });
            httpRequest.setHeaders(newHeaders);
            super.execute(httpRequest);
        } catch (Exception e) {
            log.error("HTX-101-500", e);
        }
        return Collections.emptyList();
    }

}
