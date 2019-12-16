package de.unikassel.util.metrics;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class MetricsParser {

    public HashMap<MetricType, HashSet<MetricData>> parse(String text) {
        String[] lines = text.split("\n");

        HashMap<MetricType, HashSet<MetricData>> dataMap = new HashMap<>();

        for (String line : lines) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] components = line.split("[{}]");
            if (components.length == 1) {
                components = line.split(" ");
            }

            MetricType type = MetricType.valueOf(components[0].toUpperCase());
            Map<String, String> attributes;
            if (components.length > 2) {
                attributes = new HashMap<>();
                for (String attribute : components[1].split(",")) {
                    String[] attrVal = attribute.split("=", 2);
                    attributes.put(attrVal[0], attrVal[1]);
                }
            } else {
                attributes = Collections.emptyMap();
            }
            double value = Double.parseDouble(components[components.length - 1]);

            MetricData data = new MetricData(type, attributes, value);
            dataMap.putIfAbsent(type, new HashSet<>());
            dataMap.get(type).add(data);
        }

        return dataMap;
    }
}
