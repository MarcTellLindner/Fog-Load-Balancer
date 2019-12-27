package de.unikassel.util.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializerFactory;
import com.esotericsoftware.kryo.serializers.ClosureSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import org.objenesis.strategy.StdInstantiatorStrategy;

public class Serializer {

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
