package de.unikassel.util.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializerFactory;
import com.esotericsoftware.kryo.serializers.ClosureSerializer;
import org.objenesis.strategy.StdInstantiatorStrategy;

/**
 * Class for setting up a new {@link Kryo}-instance.
 */
public class Serializer {

    private Serializer() {
    }

    /**
     * Setup a new {@link Kryo}-instance, that is already configured.
     *
     * @return The new {@link Kryo}.
     */
    public static Kryo setupKryoInstance() {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        SerializerFactory.FieldSerializerFactory fieldSerializerFactory
                = new SerializerFactory.FieldSerializerFactory();
        fieldSerializerFactory.getConfig().setIgnoreSyntheticFields(false);
        kryo.setDefaultSerializer(fieldSerializerFactory);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        kryo.register(java.lang.invoke.SerializedLambda.class);
        kryo.register(ClosureSerializer.Closure.class, new ModifiedClosureSerializer());
        return kryo;
    }

}
