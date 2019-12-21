package de.unikassel.prediction.metrics;

import java.util.Collections;
import java.util.Map;

public class MetricData {

    public final MetricType type;
    public final Map<String, String> attributes;
    public final double value;

    public MetricData(MetricType type, Map<String, String> attributes, double value) {
        this.type = type;
        this.attributes = Collections.unmodifiableMap(attributes);
        this.value = value;
    }
}
