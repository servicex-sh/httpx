package org.mvnsearch.http;

import org.junit.jupiter.api.Test;
import org.mvnsearch.http.utils.JsonUtils;

import java.util.List;
import java.util.Map;

public class AliyunProductsTest {

    @Test
    public void testListVersions() throws Exception {
        Map<String, Object> rootObject = JsonUtils.readValue(this.getClass().getResourceAsStream("/aliyun/products.json"), Map.class);
        List<Map<String, Object>> products = (List<Map<String, Object>>) rootObject.get("products");
        for (Map<String, Object> product : products) {
            String code = ((String) product.get("code")).toLowerCase();
            String version = (String) product.get("version");
            System.out.println("API_VERSIONS.put(\"" + code + "\", \"" + version + "\");");
        }
    }
}
