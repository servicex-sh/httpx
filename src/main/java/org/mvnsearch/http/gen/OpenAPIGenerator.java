package org.mvnsearch.http.gen;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.OpenAPIV3Parser;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public class OpenAPIGenerator {

    public String generateHttpFileFromOpenAPI(String openApiLocation, List<String> targets) throws Exception {
        OpenAPI openAPI = new OpenAPIV3Parser().read(openApiLocation);
        final List<Server> servers = openAPI.getServers();
        final String serverURL;
        if (!servers.isEmpty()) {
            serverURL = servers.get(0).getUrl();
        } else {
            serverURL = "http://localhost:8080";
        }
        List<String> requests = new ArrayList<>();
        openAPI.getPaths().forEach((path, pathItem) -> {
            if (isPathMatched(targets, path)) {
                String method = "POST";
                Operation operation = pathItem.getPost();
                if (operation == null) {
                    operation = pathItem.getPut();
                    method = "PUT";
                }
                if (operation == null) {
                    operation = pathItem.getGet();
                    method = "GET";
                }
                if (operation != null) {
                    String requestUrl = serverURL + path;
                    requestUrl = requestUrl.replaceAll("\\{", "{{").replaceAll("}", "}}");
                    String httpRequest = generateHttpRequest(openAPI, operation, method, path, requestUrl);
                    requests.add(httpRequest);
                }
            }
        });
        if (requests.size() > 0) {
            return String.join(System.lineSeparator(), requests);
        }
        return "";
    }

    private boolean isPathMatched(List<String> targets, String path) {
        if (targets == null || targets.isEmpty()) {
            return true;
        }
        for (String target : targets) {
            if (path.contains(target)) {
                return true;
            }
        }
        return false;
    }

    public String generateHttpRequest(OpenAPI openAPI, Operation operation, String method, String path, String requestUrl) {
        StringBuilder builder = new StringBuilder();
        String comment = operation.getSummary() == null ? "" : operation.getSummary();
        builder.append("### ").append(comment).append(System.lineSeparator());
        builder.append("//@name ").append(operation.getOperationId()).append(System.lineSeparator());
        builder.append(method).append(" ").append(requestUrl).append(System.lineSeparator());
        if (method.equals("POST") || method.equals("PUT")) {
            final Schema<?> schema = getOperationSchema(operation, openAPI);
            if (schema instanceof ObjectSchema) {
                builder.append("Content-Type: application/json").append(System.lineSeparator());
                builder.append("X-JSON-Type: ").append(getJsonType((ObjectSchema) schema)).append(System.lineSeparator());
                Object fakeData = getSchemaFakeValue(schema);
                if (fakeData != null) {
                    builder.append(System.lineSeparator());
                    try {
                        builder.append(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(fakeData));
                    } catch (Exception ignore) {

                    }
                }
            }
        }
        builder.append(System.lineSeparator());
        return builder.toString();
    }

    public Object getSchemaFakeValue(Schema<?> schema) {
        if (schema instanceof NumberSchema || schema instanceof IntegerSchema) {
            return Math.abs(new Random().nextInt(1000));
        } else if (schema instanceof StringSchema) {
            return "";
        } else if (schema instanceof ObjectSchema) {
            Map<String, Object> fakeData = new HashMap<>();
            if (schema.getProperties() != null) {
                schema.getProperties().forEach((name, fieldSchema) -> {
                    fakeData.put(name, getSchemaFakeValue(fieldSchema));
                });
            }
            return fakeData;
        } else if (schema instanceof MapSchema) {
            return new HashMap<>();
        } else if (schema instanceof DateTimeSchema) {
            return Instant.now().toString();
        } else if (schema instanceof DateSchema) {
            return LocalDate.now().toString();
        } else if (schema instanceof BooleanSchema) {
            return true;
        } else if (schema instanceof EmailSchema) {
            return "user@example.com";
        } else if (schema instanceof ArraySchema || schema instanceof ByteArraySchema || schema instanceof ComposedSchema) {
            return new String[0];
        } else if (schema instanceof BinarySchema) {
            return "base64";
        } else if (schema instanceof FileSchema) {
            return "demo.json";
        } else if (schema instanceof PasswordSchema) {
            return "password-1234";
        } else if (schema instanceof UUIDSchema) {
            return UUID.randomUUID().toString();
        } else {
            return "";
        }
    }

    public String getJsonType(ObjectSchema schema) {
        List<String> pairs = new ArrayList<>();
        if (schema.getProperties() != null) {
            schema.getProperties().forEach((name, fieldSchema) -> {
                if (fieldSchema instanceof NumberSchema) {
                    pairs.add(name + ": number");
                } else if (fieldSchema instanceof IntegerSchema) {
                    pairs.add(name + ": integer");
                } else if (fieldSchema instanceof MapSchema) {
                    pairs.add(name + ": object");
                } else if (fieldSchema instanceof DateTimeSchema) {
                    pairs.add(name + ": date-time");
                } else if (fieldSchema instanceof DateSchema) {
                    pairs.add(name + ": date");
                } else if (fieldSchema instanceof BooleanSchema) {
                    pairs.add(name + ": boolean");
                } else if (fieldSchema instanceof EmailSchema) {
                    pairs.add(name + ": email");
                } else if (fieldSchema instanceof ArraySchema || fieldSchema instanceof ByteArraySchema) {
                    pairs.add(name + ": string[]");
                } else if (fieldSchema instanceof BinarySchema) {
                    pairs.add(name + ": string");
                } else if (fieldSchema instanceof FileSchema) {
                    pairs.add(name + ": string");
                } else if (fieldSchema instanceof PasswordSchema) {
                    pairs.add(name + ": string");
                } else if (fieldSchema instanceof UUIDSchema) {
                    pairs.add(name + ": uuid");
                } else if (fieldSchema instanceof ObjectSchema) {
                    pairs.add(name + ": " + getJsonType((ObjectSchema) fieldSchema));
                } else {
                    pairs.add(name + ": string");
                }

            });
        }
        return "{" + String.join(", ", pairs) + " }";
    }

    @Nullable
    private Schema<?> getOperationSchema(Operation operation, OpenAPI openAPI) {
        Schema<?> schema = null;
        final RequestBody requestBody = operation.getRequestBody();
        if (requestBody != null) {
            final Content content = requestBody.getContent();
            for (Map.Entry<String, MediaType> entry : content.entrySet()) {
                String contentType = entry.getKey();
                schema = entry.getValue().getSchema();
                if (contentType.contains("json")) {
                    if (schema != null) {
                        final String schemaRef = schema.get$ref();
                        if (schemaRef != null) {
                            schema = openAPI.getComponents().getSchemas().get(schemaRef.substring(schemaRef.lastIndexOf('/') + 1));
                        }
                    }
                }
            }
        }
        return schema;
    }
}
