package org.mvnsearch.http.model;

import io.github.cdimascio.dotenv.Dotenv;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.jetbrains.annotations.Nullable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

public class HttpGlobalFunctions {
    private static HttpGlobalFunctions INSTANCE = null;
    private static Date NOW = null;


    public static HttpGlobalFunctions getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new HttpGlobalFunctions();
        }
        return INSTANCE;
    }

    private final static Map<String, Function<String[], String>> FUNCTIONS = new HashMap<>();

    private HttpGlobalFunctions() {
        FUNCTIONS.put("uuid", HttpGlobalFunctions::uuid); // jetbrains
        FUNCTIONS.put("guid", HttpGlobalFunctions::uuid);
        FUNCTIONS.put("randomInt", HttpGlobalFunctions::randomInt); // jetbrains
        FUNCTIONS.put("timestamp", HttpGlobalFunctions::timestamp); // jetbrains
        FUNCTIONS.put("datetime", HttpGlobalFunctions::datetime);
        FUNCTIONS.put("localDatetime", HttpGlobalFunctions::localDatetime);
        FUNCTIONS.put("dotenv", HttpGlobalFunctions::dotenv);
        FUNCTIONS.put("processEnv", HttpGlobalFunctions::processEnv);
        FUNCTIONS.put("base64", HttpGlobalFunctions::base64);
        FUNCTIONS.put("urlEncode", HttpGlobalFunctions::urlEncode);
        FUNCTIONS.put("escapeHtml", HttpGlobalFunctions::escapeHtml);
        FUNCTIONS.put("escapeXml", HttpGlobalFunctions::escapeXml);
        FUNCTIONS.put("escapeJson", HttpGlobalFunctions::escapeJson);
        FUNCTIONS.put("escapeJavaScript", HttpGlobalFunctions::escapeJavaScript);
        FUNCTIONS.put("hmacMD5", HttpGlobalFunctions::hmacMD5);
        FUNCTIONS.put("hmacSHA1", HttpGlobalFunctions::hmacSHA1);
        FUNCTIONS.put("hmacSHA256", HttpGlobalFunctions::hmacSHA256);
        FUNCTIONS.put("hmacSHA512", HttpGlobalFunctions::hmacSHA512);
        FUNCTIONS.put("md5", HttpGlobalFunctions::md5);
        FUNCTIONS.put("sha1", HttpGlobalFunctions::sha1);
        FUNCTIONS.put("sha256", HttpGlobalFunctions::sha256);
        FUNCTIONS.put("sha512", HttpGlobalFunctions::sha512);
        FUNCTIONS.put("projectRoot", HttpGlobalFunctions::projectRoot);
        FUNCTIONS.put("historyFolder", HttpGlobalFunctions::historyFolder);
        NOW = new Date();
    }

    @Nullable
    public Function<String[], String> findFunction(String name) {
        return FUNCTIONS.get(name);
    }

    public static String uuid(String[] args) {
        return UUID.randomUUID().toString();
    }

    public static String randomInt(String[] args) {
        int min = 0;
        int max = 1000;
        if (args.length > 0) {
            min = Integer.parseInt(args[0]);
            max = Integer.MAX_VALUE;
        }
        if (args.length > 1) {
            max = Integer.parseInt(args[1]);
        }
        return String.valueOf(new Random().nextInt(min, max));
    }

    public static String timestamp(String[] args) {
        long timestamp = NOW.getTime();
        if (args != null && args.length == 2) {
            long offset = Long.parseLong(args[0]);
            // s, m, h, d
            String option = args[1];
            int milliseconds = switch (option) {
                case "s" -> 1000;
                case "m" -> 60 * 1000;
                case "h" -> 60 * 60 * 1000;
                case "d" -> 24 * 60 * 60 * 1000;
                default -> 0;
            };
            timestamp = timestamp + offset * milliseconds;
        }
        return String.valueOf(timestamp);
    }

    public static String datetime(String[] args) {
        return formatDateTime(args, TimeZone.getTimeZone("UTC"));
    }

    public static String localDatetime(String[] args) {
        return formatDateTime(args, TimeZone.getDefault());
    }

    public static String formatDateTime(String[] args, TimeZone timeZone) {
        String pattern = "yyyy-MM-dd'T'HH:mm'Z'";
        if (args.length > 0) {
            String patternArg = args[0];
            if (Objects.equals(patternArg, "rfc1123")) {
                pattern = "EEE, dd MMM yyyy HH:mm:ss z";
            } else if (Objects.equals(patternArg, "iso8601")) {
                pattern = "yyyy-MM-dd'T'HH:mm'Z'";
            } else if (patternArg.startsWith("\"") || patternArg.startsWith("'")) {
                pattern = patternArg.substring(1, patternArg.length() - 1);
            } else {
                pattern = patternArg;
            }
        }
        DateFormat df = new SimpleDateFormat(pattern); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(timeZone);
        return df.format(NOW);
    }


    public static String processEnv(String[] args) {
        if (args.length > 0) {
            return System.getenv(args[0].toUpperCase());
        }
        return "";
    }

    public static String dotenv(String[] args) {
        if (args.length > 0) {
            String name = args[0].toUpperCase();
            try {
                Dotenv dotenv = Dotenv.load();
                if (dotenv != null) {
                    return dotenv.get(name);
                }
            } catch (Exception ignore) {

            }
        }
        return "";
    }

    public static String base64(String[] args) {
        if (args.length > 0) {
            return Base64.getEncoder().encodeToString(args[0].getBytes(StandardCharsets.UTF_8));
        }
        return "";
    }

    public static String urlEncode(String[] args) {
        if (args.length > 0) {
            return URLEncoder.encode(args[0], StandardCharsets.UTF_8);
        }
        return "";
    }

    public static String escapeHtml(String[] args) {
        if (args.length > 0) {
            return StringEscapeUtils.escapeHtml(args[0]);
        }
        return "";
    }

    public static String escapeXml(String[] args) {
        if (args.length > 0) {
            return StringEscapeUtils.escapeXml(args[0]);
        }
        return "";
    }

    public static String escapeJson(String[] args) {
        if (args.length > 0) {
            return StringEscapeUtils.escapeJavaScript(args[0]);
        }
        return "";
    }

    public static String escapeJavaScript(String[] args) {
        if (args.length > 0) {
            return StringEscapeUtils.escapeJavaScript(args[0]);
        }
        return "";
    }

    public static String hmacMD5(String[] args) {
        if (args.length > 1) {
            byte[] key = args[1].getBytes(StandardCharsets.UTF_8);
            final byte[] content = new HmacUtils(HmacAlgorithms.HMAC_MD5, key).hmac(args[1].getBytes(StandardCharsets.UTF_8));
            return formatBytes(content, getBytesFormat(args, 2));
        }
        return "";
    }

    public static String hmacSHA1(String[] args) {
        if (args.length > 1) {
            byte[] key = args[1].getBytes(StandardCharsets.UTF_8);
            final byte[] content = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, key).hmac(args[1].getBytes(StandardCharsets.UTF_8));
            return formatBytes(content, getBytesFormat(args, 2));
        }
        return "";
    }

    public static String hmacSHA256(String[] args) {
        if (args.length > 1) {
            byte[] key = args[1].getBytes(StandardCharsets.UTF_8);
            final byte[] content = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, key).hmac(args[1].getBytes(StandardCharsets.UTF_8));
            return formatBytes(content, getBytesFormat(args, 2));
        }
        return "";
    }

    public static String hmacSHA512(String[] args) {
        if (args.length > 1) {
            byte[] key = args[1].getBytes(StandardCharsets.UTF_8);
            final byte[] content = new HmacUtils(HmacAlgorithms.HMAC_SHA_512, key).hmac(args[1].getBytes(StandardCharsets.UTF_8));
            return formatBytes(content, getBytesFormat(args, 2));
        }
        return "";
    }


    public static String md5(String[] args) {
        if (args.length > 0) {
            return formatBytes(DigestUtils.md5(args[0].getBytes(StandardCharsets.UTF_8)),
                    getBytesFormat(args, 1));
        }
        return "";
    }

    public static String sha1(String[] args) {
        if (args.length > 0) {
            return formatBytes(DigestUtils.sha1(args[0].getBytes(StandardCharsets.UTF_8)),
                    getBytesFormat(args, 1));
        }
        return "";
    }

    public static String sha256(String[] args) {
        if (args.length > 0) {
            return formatBytes(DigestUtils.sha256(args[0].getBytes(StandardCharsets.UTF_8)),
                    getBytesFormat(args, 1));
        }
        return "";
    }

    public static String sha512(String[] args) {
        if (args.length > 0) {
            return formatBytes(DigestUtils.sha512(args[0].getBytes(StandardCharsets.UTF_8)),
                    getBytesFormat(args, 1));
        }
        return "";
    }


    private static String getBytesFormat(String[] args, int offset) {
        String format = "HEX";
        if (args.length > offset) {
            format = args[offset];
        }
        return format;
    }

    private static String formatBytes(byte[] content, String format) {
        if (format.equalsIgnoreCase("base64")) {
            return Base64.getEncoder().encodeToString(content);
        } else {
            return HexFormat.of().formatHex(content);
        }
    }

    public static String projectRoot(String[] args) {
        final String httpFilePath = System.getProperty("http.file");
        if (httpFilePath != null) {
            return Path.of(httpFilePath).getParent().resolve(".idea").toAbsolutePath().toString();
        }
        return ".idea";
    }

    public static String historyFolder(String[] args) {
        final String httpFilePath = System.getProperty("http.file");
        if (httpFilePath != null) {
            return Path.of(httpFilePath).getParent().resolve(".idea").resolve("httpRequests").toAbsolutePath().toString();
        }
        return ".idea/httpRequests/";
    }

}
