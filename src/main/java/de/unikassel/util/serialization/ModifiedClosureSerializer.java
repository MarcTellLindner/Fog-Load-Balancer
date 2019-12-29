package de.unikassel.util.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.ClosureSerializer;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Fixed version of {@link ClosureSerializer}.
 */
public class ModifiedClosureSerializer extends ClosureSerializer {


    private final Method toSerializedLambda;

    /**
     * Create a new {@link ModifiedClosureSerializer}.
     */
    public ModifiedClosureSerializer() {
        try {
            toSerializedLambda = this.getClass().getSuperclass()
                    .getDeclaredMethod("toSerializedLambda", Object.class);
            toSerializedLambda.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new KryoException(e);
        }
    }

    @Override
    public void write(Kryo kryo, Output output, Object object) {
        SerializedLambda serializedLambda;
        try {

            // Serialization needs to be done in the Java-way using parent-classes private method

            serializedLambda = (SerializedLambda) toSerializedLambda.invoke(this, object);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new KryoException(e);
        }
        int count = serializedLambda.getCapturedArgCount();
        output.writeVarInt(count, true);
        for (int i = 0; i < count; i++) {

            // --------------------- This one line is changed ! ---------------------

            kryo.writeClassAndObject(output, serializedLambda.getCapturedArg(i));

            // ----------------------------------------------------------------------

        }
        try {
            kryo.writeClass(output, Class.forName(serializedLambda.getCapturingClass().replace('/', '.')));
        } catch (ClassNotFoundException e) {
            throw new KryoException("Error writing closure.", e);
        }
        output.writeString(serializedLambda.getFunctionalInterfaceClass());
        output.writeString(serializedLambda.getFunctionalInterfaceMethodName());
        output.writeString(serializedLambda.getFunctionalInterfaceMethodSignature());
        output.writeVarInt(serializedLambda.getImplMethodKind(), true);
        output.writeString(serializedLambda.getImplClass());
        output.writeString(serializedLambda.getImplMethodName());
        output.writeString(serializedLambda.getImplMethodSignature());
        output.writeString(serializedLambda.getInstantiatedMethodType());
    }
}