package org.mvnsearch.http.vendor;

import org.jetbrains.annotations.Nullable;
import org.mvnsearch.http.utils.JsonUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Aliyun {

    /**
     * you need to run `aliyun configure` first, then read `~/.aliyun/config.json`
     *
     * @param partOfId part of ACCESS ID
     * @return AK with ID and key
     */
    @Nullable
    public static String[] readAccessFromAliyunCli(@Nullable String partOfId) {
        final File aliyunConfigJsonFile = Path.of(System.getProperty("user.home")).resolve(".aliyun").resolve("config.json").toAbsolutePath().toFile();
        if (aliyunConfigJsonFile.exists()) {
            try {
                Map<String, Object> config = JsonUtils.readValue(aliyunConfigJsonFile, Map.class);
                String profileName = (String) config.get("current");
                List<Map<String, Object>> profiles = (List<Map<String, Object>>) config.get("profiles");
                if (profileName != null && profiles != null) {
                    return profiles.stream()
                            .filter(profile -> "AK".equals(profile.get("mode")) && profile.containsKey("access_key_id") && profile.containsKey("access_key_secret"))
                            .filter(profile -> {
                                if (partOfId != null) {
                                    return ((String) profile.get("access_key_id")).contains(partOfId);
                                } else {
                                    return profileName.equals(profile.get("name"));
                                }
                            })
                            .findFirst()
                            .map(profile -> new String[]{(String) profile.get("access_key_id"), (String) profile.get("access_key_secret")})
                            .orElse(null);
                }
            } catch (Exception ignore) {

            }
        }
        return null;
    }
}
