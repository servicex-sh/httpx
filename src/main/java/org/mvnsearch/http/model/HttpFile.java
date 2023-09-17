package org.mvnsearch.http.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpFile {
    private Map<String, String> variables =new HashMap<>();
    private List<HttpRequest> requests;

    public Map<String, String> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, String> variables) {
        this.variables = variables;
    }

    public List<HttpRequest> getRequests() {
        return requests;
    }

    public void setRequests(List<HttpRequest> requests) {
        this.requests = requests;
    }

    public void addVariableLine(String line) {
        String[] parts = line.split("=", 2);
        variables.put(parts[0].substring(1).trim(), parts[1].trim());
    }
}
