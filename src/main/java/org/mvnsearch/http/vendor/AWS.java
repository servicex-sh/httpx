package org.mvnsearch.http.vendor;

import org.ini4j.Ini;
import org.ini4j.Profile;
import org.jetbrains.annotations.Nullable;
import org.mvnsearch.http.model.HttpRequest;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AWS {

    @Nullable
    public static AwsBasicCredentials awsBasicCredentials(HttpRequest httpRequest) {
        String[] credential = AWS.readAwsAccessToken(httpRequest);
        if (credential != null && credential.length > 1) {
            return AwsBasicCredentials.create(credential[0], credential[1]);
        }
        return null;
    }

    @Nullable
    public static String[] readAwsAccessToken(HttpRequest httpRequest) {
        final String authHeader = httpRequest.getHeader("Authorization");
        String[] awsCredential = null;
        if (authHeader != null) {
            if (authHeader.startsWith("AWS ")) { // VS Code REST Client plugin
                awsCredential = authHeader.substring(4).trim().split("\\s+");
            } else if (authHeader.startsWith("Basic")) {
                return httpRequest.getBasicAuthorization();
            }
        }
        if (awsCredential == null) { // read default profile
            awsCredential = readAccessFromAwsCli(null);
        } else if (awsCredential.length > 1 && awsCredential[1].length() <= 4) { // id match
            awsCredential = readAccessFromAwsCli(awsCredential[0]);
        }
        return awsCredential;
    }

    @Nullable
    public static String[] readAccessFromAwsCli(@Nullable String partOfId) {
        final Path awsCredentialsFile = Path.of(System.getProperty("user.home")).resolve(".aws").resolve("credentials").toAbsolutePath();
        if (awsCredentialsFile.toFile().exists()) {
            try {
                final List<String> lines = Files.readAllLines(awsCredentialsFile);
                Map<String, Map<String, String>> store = new HashMap<>();
                String profileName = "default";
                for (String line : lines) {
                    if (line.startsWith("[")) {
                        profileName = line.substring(1, line.indexOf(']'));
                    } else if (line.contains("=")) {
                        final String[] parts = line.split("=", 2);
                        Map<String, String> profile = store.computeIfAbsent(profileName, k -> new HashMap<>());
                        profile.put(parts[0].trim(), parts[1].trim());
                    }
                }
                if (partOfId == null && store.containsKey("default")) {
                    return extractAccessToken(store.get("default"));
                } else if (partOfId != null && !store.isEmpty()) {
                    for (Map.Entry<String, Map<String, String>> entry : store.entrySet()) {
                        String keyId = entry.getValue().get("aws_access_key_id");
                        if (keyId != null && keyId.contains(partOfId)) {
                            return extractAccessToken(store.get(entry.getKey()));
                        }
                    }
                }
            } catch (Exception ignore) {

            }
        }
        return null;
    }

    private static String[] extractAccessToken(Map<String, String> profile) {
        if (profile.containsKey("aws_access_key_id") && profile.containsKey("aws_access_key_id")) {
            return new String[]{profile.get("aws_access_key_id"), profile.get("aws_secret_access_key")};
        }
        return null;
    }

    public static String readDefaultRegionFromCLI() {
        final Path awsConfigFile = Path.of(System.getProperty("user.home")).resolve(".aws").resolve("config").toAbsolutePath();
        if (awsConfigFile.toFile().exists()) {
            try {
                final Profile.Section profile = new Ini(awsConfigFile.toFile()).get("default");
                if (profile != null) {
                    return profile.get("region");
                }
            } catch (Exception ignore) {

            }
        }
        return null;
    }

    public static String readRegionId(HttpRequest httpRequest) {
        String regionId = httpRequest.getHeader("X-Region-Id");
        if (regionId == null) { //resolve region id from host
            String host = httpRequest.getRequestTarget().getUri().getHost();
            String tempRegionId = host.replace(".amazonaws.com", "");
            if (tempRegionId.contains(".")) {
                tempRegionId = tempRegionId.substring(tempRegionId.indexOf(".") + 1);
            }
            if (tempRegionId.contains("-")) {
                regionId = tempRegionId;
            }
        }
        if (regionId == null) {
            regionId = readDefaultRegionFromCLI();
        }
        return regionId == null ? "us-east-1" : regionId;
    }
}
