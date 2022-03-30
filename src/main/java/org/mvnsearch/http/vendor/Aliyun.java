package org.mvnsearch.http.vendor;

import org.ini4j.Ini;
import org.ini4j.Profile;
import org.jetbrains.annotations.Nullable;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.utils.JsonUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Aliyun {
    /**
     * endpoints info from endpoint.json
     */
    private static Map<String, Object> ENDPOINTS = null;
    private static Map<String, String> API_VERSIONS = new HashMap<>();

    public static String getApiVersion(String productCode) {
        if (API_VERSIONS.isEmpty()) {
            API_VERSIONS.put("domain", "2018-01-29");
            API_VERSIONS.put("cdn", "2018-05-10");
            API_VERSIONS.put("ram", "2015-05-01");
            API_VERSIONS.put("cbn", "2017-09-12");
            API_VERSIONS.put("drds", "2019-01-23");
            API_VERSIONS.put("emr", "2016-04-08");
            API_VERSIONS.put("sts", "2015-04-01");
            API_VERSIONS.put("cs", "2015-12-15");
            API_VERSIONS.put("cr", "2018-12-01");
            API_VERSIONS.put("hbase", "2019-01-01");
            API_VERSIONS.put("ros", "2019-09-10");
            API_VERSIONS.put("ess", "2018-08-28");
            API_VERSIONS.put("gpdb", "2016-05-03");
            API_VERSIONS.put("dds", "2015-12-01");
            API_VERSIONS.put("mongodb", "2015-12-01");
            API_VERSIONS.put("cloudauth", "2020-06-18");
            API_VERSIONS.put("live", "2016-11-01");
            API_VERSIONS.put("hpc", "2018-04-12");
            API_VERSIONS.put("ehpc", "2018-04-12");
            API_VERSIONS.put("ddos", "2017-05-18");
            API_VERSIONS.put("ddosbasic", "2017-05-18");
            API_VERSIONS.put("ddospro", "2017-05-18");
            API_VERSIONS.put("antiddos", "2017-05-18");
            API_VERSIONS.put("ddosbgp", "2018-07-20");
            API_VERSIONS.put("dm", "2015-11-23");
            API_VERSIONS.put("domain-intl", "2018-01-29");
            API_VERSIONS.put("cloudwf", "2017-12-07");
            API_VERSIONS.put("ecs", "2014-05-26");
            API_VERSIONS.put("ecs-cn-hangzhou", "2014-05-26");
            API_VERSIONS.put("vpc", "2016-04-28");
            API_VERSIONS.put("redisa", "2015-01-01");
            API_VERSIONS.put("r-kvstore", "2015-01-01");
            API_VERSIONS.put("codepipeline", "2021-06-25");
            API_VERSIONS.put("cds", "2021-06-25");
            API_VERSIONS.put("rds", "2014-08-15");
            API_VERSIONS.put("httpdns", "2016-02-01");
            API_VERSIONS.put("httpdns-api", "2016-02-01");
            API_VERSIONS.put("green", "2018-05-09");
            API_VERSIONS.put("alidns", "2014-05-15");
            API_VERSIONS.put("push", "2016-08-01");
            API_VERSIONS.put("cloudpush", "2016-08-01");
            API_VERSIONS.put("cms", "2019-01-01");
            API_VERSIONS.put("metrics", "2019-01-01");
            API_VERSIONS.put("slb", "2014-05-15");
            API_VERSIONS.put("apigateway", "2016-07-14");
            API_VERSIONS.put("cloudapi", "2016-07-14");
            API_VERSIONS.put("sas", "2018-12-03");
            API_VERSIONS.put("sas-api", "2018-12-03");
            API_VERSIONS.put("beebot", "2017-10-11");
            API_VERSIONS.put("chatbot", "2017-10-11");
            API_VERSIONS.put("iot", "2018-01-20");
            API_VERSIONS.put("arms", "2019-08-08");
            API_VERSIONS.put("polardb", "2017-08-01");
            API_VERSIONS.put("ccc", "2017-07-05");
            API_VERSIONS.put("bastionhost", "2019-12-09");
            API_VERSIONS.put("yundun-bastionhost", "2019-12-09");
            API_VERSIONS.put("rtc", "2018-01-01");
            API_VERSIONS.put("nlp", "2019-11-11");
            API_VERSIONS.put("nlp-automl", "2019-11-11");
            API_VERSIONS.put("trademark", "2019-12-09");
            API_VERSIONS.put("sca", "2019-01-15");
            API_VERSIONS.put("qualitycheck", "2019-01-15");
            API_VERSIONS.put("iovcc", "2018-05-01");
            API_VERSIONS.put("ons", "2019-02-14");
            API_VERSIONS.put("onsvip", "2019-02-14");
            API_VERSIONS.put("pts", "2020-10-20");
            API_VERSIONS.put("waf", "2019-09-10");
            API_VERSIONS.put("wafopenapi", "2019-09-10");
            API_VERSIONS.put("cloudfirewall", "2017-12-07");
            API_VERSIONS.put("cloudfw", "2017-12-07");
            API_VERSIONS.put("baas", "2018-12-21");
            API_VERSIONS.put("imm", "2017-09-06");
            API_VERSIONS.put("ims", "2019-08-15");
            API_VERSIONS.put("oss", "2019-05-17");
            API_VERSIONS.put("ddoscoo", "2020-01-01");
            API_VERSIONS.put("smartag", "2018-03-13");
            API_VERSIONS.put("actiontrail", "2020-07-06");
            API_VERSIONS.put("ots", "2016-06-20");
            API_VERSIONS.put("cas", "2020-04-07");
            API_VERSIONS.put("mts", "2014-06-18");
            API_VERSIONS.put("pvtz", "2018-01-01");
            API_VERSIONS.put("ensdisk", "2017-11-10");
            API_VERSIONS.put("ens", "2017-11-10");
            API_VERSIONS.put("vod", "2017-03-21");
            API_VERSIONS.put("imagesearch", "2020-12-14");
            API_VERSIONS.put("market", "2015-11-01");
            API_VERSIONS.put("pcdn", "2017-04-11");
            API_VERSIONS.put("nas", "2017-06-26");
            API_VERSIONS.put("kms", "2016-01-20");
            API_VERSIONS.put("eci", "2018-08-08");
            API_VERSIONS.put("fc", "2021-04-06");
            API_VERSIONS.put("openanalytics", "2018-06-19");
            API_VERSIONS.put("dcdn", "2018-01-15");
            API_VERSIONS.put("elasticsearch", "2017-06-13");
            API_VERSIONS.put("dts", "2020-01-01");
            API_VERSIONS.put("dysmsapi", "2017-05-25");
            API_VERSIONS.put("dybaseapi", "2017-05-25");
            API_VERSIONS.put("bssopenapi", "2017-12-14");
            API_VERSIONS.put("business", "2017-12-14");
            API_VERSIONS.put("dmsenterprise", "2018-11-01");
            API_VERSIONS.put("dms-enterprise", "2018-11-01");
            API_VERSIONS.put("alikafka", "2019-09-16");
            API_VERSIONS.put("foas", "2018-11-11");
            API_VERSIONS.put("alidfs", "2018-06-20");
            API_VERSIONS.put("dfs", "2018-06-20");
            API_VERSIONS.put("airec", "2020-11-26");
            API_VERSIONS.put("scdn", "2017-11-15");
            API_VERSIONS.put("saf", "2017-03-31");
            API_VERSIONS.put("linkwan", "2019-03-01");
            API_VERSIONS.put("linkedmall", "2018-01-16");
            API_VERSIONS.put("vs", "2018-12-12");
            API_VERSIONS.put("aiccs", "2018-10-15");
            API_VERSIONS.put("ccs", "2018-10-15");
            API_VERSIONS.put("hitsdb", "2020-06-15");
            API_VERSIONS.put("alimt", "2018-10-12");
            API_VERSIONS.put("mt", "2018-10-12");
            API_VERSIONS.put("dbs", "2019-03-06");
            API_VERSIONS.put("xxx", "2019-12-09");
        }
        return API_VERSIONS.get(productCode);
    }

    @Nullable
    public static String getRegionId(String host) {
        if (host.contains(".fc.aliyuncs.com")) {
            return host.substring(0, host.indexOf('.'));
        }
        String regionId = null;
        String tempRegionId = host.replace(".aliyuncs.com", "");
        if (tempRegionId.contains(".")) {
            regionId = tempRegionId.substring(tempRegionId.indexOf(".") + 1);
        } else if (tempRegionId.contains("-")) {
            if (!tempRegionId.contains("r-kvstore")) {
                regionId = tempRegionId.substring(tempRegionId.indexOf("-") + 1);
            }
        }
        return regionId;
    }

    public static String getServiceName(String host) {
        if (host.endsWith(".fc.aliyuncs.com")) {
            return "fc";
        }
        if (host.endsWith(".oas.aliyuncs.com")) {
            return "oas";
        }
        String serviceName = host.substring(0, host.indexOf("."));
        if (serviceName.contains("-")) {
            if (serviceName.contains("r-kvstore")) {
                return "redisa";
            } else if (serviceName.contains("domain-intl")) {
                return "domain-intl";
            } else if (serviceName.contains("yundun-bastionhost")) {
                return "yundun-bastionhost";
            } else if (serviceName.contains("httpdns-api")) {
                return "httpdns";
            } else if (serviceName.contains("dms-enterprise")) {
                return "dms-enterprise";
            } else {
                return serviceName.substring(0, serviceName.indexOf('-'));
            }
        }
        return serviceName;
    }

    @Nullable
    public static CloudAccount readAliyunAccessToken(HttpRequest httpRequest) {
        CloudAccount account;
        String[] keyIdAndSecret = httpRequest.getBasicAuthorization();
        if (keyIdAndSecret == null) { // read default profile
            account = Aliyun.readAccessFromAliyunCli(null);
            if (account == null) {
                account = Aliyun.readAccessFromCredentials(null);
            }
        } else if (keyIdAndSecret.length == 2 && keyIdAndSecret[1].length() <= 4) { // id match
            account = Aliyun.readAccessFromAliyunCli(keyIdAndSecret[0]);
            if (account == null) {
                account = Aliyun.readAccessFromCredentials(keyIdAndSecret[0]);
            }
        } else {
            account = new CloudAccount();
            account.setAccessKeyId(keyIdAndSecret[0]);
            account.setAccessKeySecret(keyIdAndSecret[1]);
        }
        return account;
    }

    /**
     * you need to run `aliyun configure` first, then read `~/.aliyun/config.json`
     *
     * @param partOfId part of ACCESS ID
     * @return AK with ID and key
     */
    @Nullable
    public static CloudAccount readAccessFromAliyunCli(@Nullable String partOfId) {
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
                            .map(profile -> new CloudAccount((String) profile.get("access_key_id"), (String) profile.get("access_key_secret"), (String) profile.get("region_id")))
                            .orElse(null);
                }
            } catch (Exception ignore) {

            }
        }
        return null;
    }

    /**
     * read AK from $HOME/.alibabacloud/credentials.ini
     *
     * @param partOfId part of id
     * @return ak
     */
    public static CloudAccount readAccessFromCredentials(@Nullable String partOfId) {
        final Path configPath = Path.of(System.getProperty("user.home"), ".alibabacloud", "credentials.ini");
        if (configPath.toFile().exists()) {
            try {
                final Ini config = new Ini(configPath.toFile());
                Profile.Section profile = null;
                if (partOfId != null) {
                    for (Profile.Section tempProfile : config.values()) {
                        if (tempProfile.containsKey("access_key_id")) {
                            if (tempProfile.get("access_key_id").contains(partOfId)) {
                                profile = tempProfile;
                                break;
                            }
                        }
                    }
                } else {
                    profile = config.get("default");
                }
                if (profile != null) {
                    return new CloudAccount(profile.get("access_key_id"), profile.get("access_key_secret"), profile.get("region_id"));
                }
            } catch (Exception ignore) {

            }
        }
        return null;
    }

    public static List<String> regions() {
        if (ENDPOINTS == null) {
            initEndpoints();
        }
        return (List<String>) ENDPOINTS.get("regions");
    }

    private static void initEndpoints() {
        try {
            ENDPOINTS = JsonUtils.readValue(Aliyun.class.getResourceAsStream("/endpoints.json"), Map.class);
        } catch (Exception ignore) {

        }
    }
}
