package org.mvnsearch.http.model;

import java.text.SimpleDateFormat;
import java.util.Date;

public class HttpCookie {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss");
    private String domain;
    private String path;
    private String name;
    private String value;
    private Date expired;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Date getExpired() {
        return expired;
    }

    public void setExpired(Date expired) {
        this.expired = expired;
    }

    public static HttpCookie valueOf(String line) throws Exception {
        final String[] parts = line.split("\\t");
        HttpCookie cookie = new HttpCookie();
        cookie.setDomain(parts[0]);
        cookie.setPath(parts[1]);
        cookie.setName(parts[2]);
        cookie.setValue(parts[3]);
        cookie.setExpired(DATE_FORMAT.parse(parts[4]));
        return cookie;
    }
}
