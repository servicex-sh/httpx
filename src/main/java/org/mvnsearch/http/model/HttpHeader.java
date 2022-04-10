package org.mvnsearch.http.model;


import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class HttpHeader implements Comparable<HttpHeader> {
    private String name;
    private String value;

    public HttpHeader() {
    }

    public HttpHeader(String name, String value) {
        this.name = name;
        this.value = value;
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

    @Override
    public int compareTo(@NotNull HttpHeader o) {
        return this.name.compareTo(o.name);
    }
}

