package org.example.celjavasample.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CelJsonUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Map<String, Object> jsonToCelInput(String json) throws JsonProcessingException {
        Object raw = mapper.readValue(json, Object.class);
        return (Map<String, Object>) convertNumbers(raw);
    }

    public static String toJson(Object result) throws JsonProcessingException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }

    // Your existing convertNumbers method can go here
    public static Object convertNumbers(Object v) {
        if (v instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (var e : map.entrySet()) {
                result.put(String.valueOf(e.getKey()), convertNumbers(e.getValue()));
            }
            return result;
        }
        if (v instanceof List<?> list) {
            List<Object> result = new ArrayList<>();
            for (var x : list) {
                result.add(convertNumbers(x));
            }
            return result;
        }
        if (v instanceof Integer i) {
            return Long.valueOf(i);
        }
        return v;
    }
}