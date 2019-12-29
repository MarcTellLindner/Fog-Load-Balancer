package de.unikassel.prediction.metrics;

import java.util.Collections;
import java.util.Map;

/**
 * Information about one measured metric.
 */
public class MetricData {

    public final MetricType type;
    public final Map<String, String> attributes;
    public final double value;

    /**
     * Constructor setting type, attributes and value of data.
     *
     * @param type The type of the data.
     * @param attributes The attributes of the data.
     * @param value The value of the data.
     */
    public MetricData(MetricType type, Map<String, String> attributes, double value) {
        this.type = type;
        this.attributes = Collections.unmodifiableMap(attributes);
        this.value = value;
    }
}
