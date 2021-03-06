package org.mvnsearch.http.model;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.broker.common.Id;
import io.rsocket.broker.common.MimeTypes;
import io.rsocket.broker.common.Tags;
import io.rsocket.broker.common.WellKnownKey;
import io.rsocket.broker.frames.Address;
import io.rsocket.broker.frames.AddressFlyweight;
import io.rsocket.broker.frames.RouteSetupFlyweight;
import io.rsocket.metadata.TaggingMetadataCodec;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.util.DefaultPayload;
import org.mvnsearch.http.utils.JsonUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static io.rsocket.metadata.CompositeMetadataCodec.encodeAndAddMetadata;

@SuppressWarnings("FieldMayBeFinal")
public class RSocketRequest {
    private URI uri;
    private String requestType;
    private String dataMimeType;
    private String metadataMimeType;
    private String setupData;
    private String setupMetadata;
    private String acceptMimeType;
    private String authorization;
    private String userAgent;
    private String metadata;
    /**
     * used for converted body, such as GraphQL
     */
    private String newBody;
    private HttpRequest httpRequest;
    private String graphqlOperationName = "request";

    private String appId = UUID.randomUUID().toString();

    public RSocketRequest(HttpRequest httpRequest) {
        this.uri = httpRequest.getRequestTarget().getUri();
        this.requestType = httpRequest.getMethod().getName();
        this.dataMimeType = httpRequest.getHeader("Content-Type");
        if (this.dataMimeType == null) {
            this.dataMimeType = WellKnownMimeType.APPLICATION_JSON.getString();
        }
        this.metadataMimeType = httpRequest.getHeader("Metadata-Type");
        if (this.metadataMimeType == null) {
            this.metadataMimeType = WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.getString();
        }
        this.authorization = httpRequest.getHeader("Authorization");
        this.userAgent = httpRequest.getHeader("User-Agent");
        this.acceptMimeType = httpRequest.getHeader("Accept");
        if (this.acceptMimeType == null) {
            this.acceptMimeType = this.dataMimeType;
        }
        this.setupMetadata = httpRequest.getHeader("Setup-Metadata");
        this.setupData = httpRequest.getHeader("Setup-Data");
        this.metadata = httpRequest.getHeader("Metadata");
        this.httpRequest = httpRequest;
        //graphql convert
        if (Objects.equals(requestType, "GRAPHQL") || Objects.equals(requestType, "GRAPHQLRS")) {
            final String bodyText = httpRequest.bodyText();
            String realContentType = httpRequest.getHeader("Content-Type");
            if (realContentType == null || Objects.equals(realContentType, "application/graphql")) {
                Map<String, Object> jsonRequest = new HashMap<>();
                if (bodyText.startsWith("subscription")) {
                    graphqlOperationName = "subscription";
                }
                jsonRequest.put("query", bodyText);
                // check body + variables json
                int offset1 = bodyText.lastIndexOf('{');
                int offset2 = bodyText.lastIndexOf('}');
                if (offset2 > offset1) {
                    String jsonText = bodyText.substring(offset1, offset2 + 1);
                    if (jsonText.contains("\"")) {
                        try {
                            jsonRequest.put("variables", JsonUtils.readValue(jsonText, Map.class));
                            jsonRequest.put("query", bodyText.subSequence(0, offset1));
                        } catch (Exception ignore) {
                        }
                    }
                }
                String graphqlVariables = httpRequest.getHeader("x-graphql-variables");
                if (graphqlVariables != null && graphqlVariables.startsWith("{")) {
                    try {
                        jsonRequest.put("variables", JsonUtils.readValue(graphqlVariables, Map.class));
                    } catch (Exception ignore) {
                    }
                }
                this.newBody = JsonUtils.writeValueAsString(jsonRequest);
            } else if (dataMimeType.contains("json")) {
                try {
                    final Map<String, Object> document = JsonUtils.readValue(bodyText, Map.class);
                    if (document.containsKey("query")) {
                        if (document.get("query").toString().startsWith("subscription")) {
                            graphqlOperationName = "subscription";
                        }
                    }
                } catch (Exception ignore) {

                }
            }
            this.dataMimeType = "application/json";
            this.acceptMimeType = "application/json";
        } else if (Objects.equals(dataMimeType, "application/graphql+json")) {
            this.dataMimeType = "application/json";
            this.acceptMimeType = "application/json";
        }
    }

    public Payload setupPayload() {
        if (isAliBroker()) {
            return createSetupPayloadForAliBroker();
        } else if (isSpringBroker()) {
            return createSetupPayloadForSpringBroker(Id.from(appId));
        } else {
            var metadata = setupMetadata == null ? Unpooled.EMPTY_BUFFER : Unpooled.wrappedBuffer(textToBytes(setupMetadata));
            var data = setupData == null ? Unpooled.EMPTY_BUFFER : Unpooled.wrappedBuffer(textToBytes(setupData));
            return DefaultPayload.create(data, metadata);
        }
    }

    public Payload createPayload() throws Exception {
        if (metadataMimeType.equals(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.getString())) {
            var compositeMetadataBuffer = compositeMetadata();
            if (isSpringBroker()) {
                encodeAddressMetadata(Id.from(appId), compositeMetadataBuffer);
            }
            return DefaultPayload.create(Unpooled.wrappedBuffer(getBodyBytes()), compositeMetadataBuffer);
        } else { //json
            var metadata = jsonMetadata();
            return DefaultPayload.create(getBodyBytes(), metadata.getBytes(StandardCharsets.UTF_8));
        }
    }

    public byte[] getBodyBytes() throws Exception {
        if (this.newBody != null) {
            return newBody.getBytes(StandardCharsets.UTF_8);
        }
        return httpRequest.getBodyBytes();
    }

    private String jsonMetadata() {
        var compositeMetadata = new HashMap<String, Object>();
        final List<String> routingMetadata = routingMetadata();
        if (!routingMetadata.get(0).isEmpty()) {
            compositeMetadata.put(WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.getString(), routingMetadata());
            compositeMetadata.put(WellKnownMimeType.MESSAGE_RSOCKET_MIMETYPE.getString(), dataMimeType);
        }
        if (metadata != null) {
            try {
                var metadataJson = JsonUtils.readValue(metadata, Map.class);
                compositeMetadata.putAll(metadataJson);
            } catch (Exception ignore) {

            }
        }
        return JsonUtils.writeValueAsString(compositeMetadata);
    }

    private void encodeAddressMetadata(Id routeId, CompositeByteBuf metadataHolder) {
        var builder = Address.from(routeId);
        httpRequest.getHeaders().forEach(httpHeader -> {
            String key = httpHeader.getName();
            String value = httpHeader.getValue();
            if (key.startsWith("X-")) {
                var keyName = key.substring(2);
                var knownKey = WellKnownKey.fromMimeType("io.rsocket.broker.$keyName");
                if (knownKey != WellKnownKey.UNPARSEABLE_KEY) {
                    builder.with(knownKey, value.trim());
                } else {
                    builder.with(keyName, value.trim());
                }
            }
        });
        var address = builder.build();
        var byteBuf = AddressFlyweight.encode(ByteBufAllocator.DEFAULT, address.getOriginRouteId(), address.getMetadata(), address.getTags(), address.getFlags());
        encodeAndAddMetadata(metadataHolder, ByteBufAllocator.DEFAULT, MimeTypes.BROKER_FRAME_MIME_TYPE, byteBuf);
    }

    private CompositeByteBuf compositeMetadata() {
        var compositeMetadataBuffer = ByteBufAllocator.DEFAULT.compositeBuffer();
        var routingMetadata = routingMetadata();
        if (!routingMetadata.get(0).isEmpty()) {
            var routingMetaData = TaggingMetadataCodec.createTaggingContent(ByteBufAllocator.DEFAULT, routingMetadata);
            encodeAndAddMetadata(
                    compositeMetadataBuffer, ByteBufAllocator.DEFAULT,
                    WellKnownMimeType.MESSAGE_RSOCKET_ROUTING,
                    routingMetaData
            );
            var dataType = WellKnownMimeType.fromString(dataMimeType);
            final byte data = (byte) (dataType.getIdentifier() | 0x80);
            encodeAndAddMetadata(
                    compositeMetadataBuffer, ByteBufAllocator.DEFAULT,
                    WellKnownMimeType.MESSAGE_RSOCKET_MIMETYPE,
                    Unpooled.wrappedBuffer(new byte[]{data}));
        }
        return compositeMetadataBuffer;
    }

    public List<String> routingMetadata() {
        var path = uri.getRawPath();
        if (path == null) {
            path = "";
        }
        if (uri.getScheme().contains("ws")) {
            String endpointPath = getWebSocketEndpointPath(uri.toString());
            if (!endpointPath.isEmpty()) {
                path = path.substring(endpointPath.length());
            }
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        var routing = new ArrayList<String>();
        routing.add(path);
        var query = uri.getQuery();
        if (query != null && !query.isEmpty()) {
            routing.addAll(Arrays.asList(query.split("&")));

        }
        return routing;
    }


    private Payload createSetupPayloadForAliBroker() {
        var allocator = ByteBufAllocator.DEFAULT;
        var setupMetadata = allocator.compositeBuffer();
        var appInfo = """
                {"name": "rsocket-http-cli"}""";
        encodeAndAddMetadata(setupMetadata, allocator, "message/x.rsocket.application+json",
                Unpooled.wrappedBuffer(appInfo.getBytes(StandardCharsets.UTF_8)));
        return DefaultPayload.create(Unpooled.EMPTY_BUFFER, setupMetadata);
    }

    private Payload createSetupPayloadForSpringBroker(Id routeId) {
        var appId = UUID.randomUUID().toString();
        var allocator = ByteBufAllocator.DEFAULT;
        var routeSetup = RouteSetupFlyweight.encode(allocator, routeId, "rsocket-http-cli", Tags.empty(), 0);
        var setupMetadata = allocator.compositeBuffer();
        encodeAndAddMetadata(setupMetadata, allocator, MimeTypes.BROKER_FRAME_MIME_TYPE, routeSetup);
        return DefaultPayload.create(Unpooled.EMPTY_BUFFER, setupMetadata);
    }

    public boolean isAliBroker() {
        return httpRequest.getHeader("X-AliBroker") != null;
    }

    public boolean isSpringBroker() {
        return httpRequest.getHeader("X-ServiceName") != null;
    }

    private byte[] textToBytes(String text) {
        if (text == null) {
            return new byte[]{};
        } else if (text.startsWith("data:")) {
            var base64Text = text.substring(text.indexOf(",") + 1).trim();
            return Base64.getDecoder().decode(base64Text);
        } else {
            return text.getBytes(StandardCharsets.UTF_8);
        }
    }

    public URI getUri() {
        return uri;
    }

    public String getGraphqlOperationName() {
        return graphqlOperationName;
    }

    public URI getWebsocketRequestURI() {
        var connectionURL = uri.toString();
        if (connectionURL.startsWith("rsocketws") || connectionURL.contains("+ws")) { //rsocket as ws path
            connectionURL = connectionURL.substring(connectionURL.indexOf("ws"));
        }
        String endpointPath = getWebSocketEndpointPath(connectionURL);
        if (!endpointPath.isEmpty()) {
            connectionURL = connectionURL.substring(0, connectionURL.indexOf(endpointPath) + endpointPath.length());
        }
        return URI.create(connectionURL);
    }

    private String getWebSocketEndpointPath(String url) {
        int offset1;
        if (url.startsWith("rsocket")) {
            offset1 = url.indexOf('/', 15);
        } else {
            offset1 = url.indexOf('/', 7);
        }
        if (offset1 > 0) {
            int offset2 = url.indexOf('/', offset1 + 1);
            if (offset2 < 0) {
                return url.substring(offset1);
            } else {
                return url.substring(offset1, offset2);
            }
        }
        return "";
    }

    public String getRequestType() {
        return requestType;
    }

    public String getDataMimeType() {
        return dataMimeType;
    }

    public String getMetadataMimeType() {
        return metadataMimeType;
    }

    public String getAcceptMimeType() {
        return acceptMimeType;
    }

    public HttpRequest getHttpRequest() {
        return httpRequest;
    }
}
