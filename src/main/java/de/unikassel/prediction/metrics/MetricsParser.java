package de.unikassel.prediction.metrics;

import java.util.*;

/**
 * Class for creating {@link MetricData}-objects from Strings.
 */
public class MetricsParser {

    /**
     * Read the metrics in the provided text.
     *
     * @param lines The text containing information about the metrics.
     * @return The information represented as a {@link MetricData}-objects.
     */
    public HashMap<MetricType, HashSet<MetricData>> parse(String[] lines) {

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

    /**
     * Get the maximum value of the specified {@link MetricType} in the given structure.
     *
     * @param type The {@link MetricType} to fin the maximum value of.
     * @param data The {@link MetricData}-objects to search in.
     * @param sum Add multiple values, if present in the same map, treat as individual values otherwise.
     * @return An {@link Optional} containing the maximum value, if present.
     */
    public Optional<MetricData> getMax(MetricType type,
                                       List<HashMap<MetricType, HashSet<MetricData>>> data, boolean sum) {
        if(sum) {
            OptionalDouble max
                    = data.stream().mapToDouble(map -> map.get(type).stream().mapToDouble(md -> md.value).sum()).max();
            if (!max.isPresent()) {
                return Optional.empty();
            }
            return Optional.of(new MetricData(type, Collections.singletonMap("max", "summed"), max.getAsDouble()));
        } else {
            return data.stream().flatMap(map -> map.get(type).stream()).max(Comparator.comparingDouble(a -> a.value));
        }
    }
}
