package org.mvnsearch.http.protocol;

import okhttp3.*;
import okio.BufferedSink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.vendor.AWS;
import org.mvnsearch.http.vendor.Aliyun;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AwsExecutor implements BaseExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(AwsExecutor.class);

    public List<byte[]> execute(HttpRequest httpRequest) {
        try {
            String method = httpRequest.getMethod().getName();
            final byte[] bodyBytes = httpRequest.getBodyBytes();
            if (Objects.equals(method, "AWS")) {
                if (bodyBytes == null || bodyBytes.length == 0) {
                    method = "GET";
                } else {
                    method = "POST";
                }
            }
            final URI requestUri = httpRequest.getRequestTarget().getUri();
            String host = requestUri.getHost();
            String serviceName = host.substring(0, host.indexOf('.'));
            //resolve region id from X-Region-Id header or host name
            String regionId = httpRequest.getHeader("X-Region-Id");
            if (regionId == null) { //resolve region id from host
                String tempRegionId = host.replace(".amazonaws.com", "");
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
                regionId = "us-east-1";
            }
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
                    .uri(URI.create("https://iam.amazonaws.com/?Action=ListUsers&Version=2010-05-08"))
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
            String format = "JSON";
            if (!headers.containsKey("Accept")) {
                requestBuilder.putHeader("Accept", "application/json");
            } else {
                if (headers.get("Accept").contains("xml")) {
                    format = "XML";
                }
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
            OkHttpClient client = new OkHttpClient();
            final Request.Builder okhttpRequestBuilder;
            if (Objects.equals(method, "POST") || Objects.equals(method, "PUT")) {
                okhttpRequestBuilder = new Request.Builder().method(method, new RequestBody() {
                    @Nullable
                    @Override
                    public MediaType contentType() {
                        return MediaType.parse(headers.getOrDefault("Content-Type", "txt/plain"));
                    }

                    @Override
                    public void writeTo(@NotNull BufferedSink sink) throws IOException {
                        if (bodyBytes != null && bodyBytes.length > 0) {
                            sink.write(bodyBytes);
                        }
                    }
                });
            } else {
                okhttpRequestBuilder = new Request.Builder().get().url(signedRequest.getUri().toString());
            }
            signedRequest.headers().forEach((name, values) -> {
                okhttpRequestBuilder.header(name, values.get(0));
            });
            final Request request = okhttpRequestBuilder.build();
            try (Response response = client.newCall(request).execute()) {
                @SuppressWarnings("ConstantConditions") final String text = response.body().string();
                if (format.equals("JSON")) {
                    System.out.println(prettyJsonFormatWithJsonPath(text, httpRequest.getHeader("X-JSON-PATH")));
                } else {
                    System.out.println(text);
                }
                return List.of(text.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            log.error("HTX-101-500", e);
        }
        return Collections.emptyList();
    }

}
