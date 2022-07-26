package org.mvnsearch.http.protocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

public class MessageStore {
    private final Map<Integer, List<String>> store = new HashMap<>();

    public boolean containsKey(int id) {
        return store.containsKey(id);
    }

    public List<String> get(int id) {
        return store.getOrDefault(id, emptyList());
    }

    public void addMessage(int id, String message) {
        if (!store.containsKey(id)) {
            store.put(id, new ArrayList<>());
        }
        store.get(id).add(message);
    }
}
