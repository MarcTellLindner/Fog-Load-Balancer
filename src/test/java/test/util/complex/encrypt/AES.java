package test.util.complex.encrypt;

import test.util.complex.SomethingComplex;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

public class AES implements SomethingComplex<byte[]> {

    @Override
    public byte[] doSomethingComplex(int intVal, long longVal) {
        try {
            if(longVal > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("longVal was too big");
            }

            SecretKeySpec key = generateKey();
            IvParameterSpec inVec = generateInVec();
            Cipher cipher = setupCipher(key, inVec);


            byte[] rand = new byte[(int) longVal];
            new Random().nextBytes(rand);

            for(int i = 0; i < intVal; ++i) {
                rand = cipher.doFinal(rand);
            }

            return rand;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SecretKeySpec generateKey() throws NoSuchAlgorithmException {
        byte[] passwordBytes = "SuperSecretPassword".getBytes(StandardCharsets.UTF_8);
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = Arrays.copyOf(messageDigest.digest(passwordBytes), 16);
        return new SecretKeySpec(keyBytes, "AES");
    }

    private IvParameterSpec generateInVec() {
        byte[] inVecBytes = new byte[16];
        new SecureRandom().nextBytes(inVecBytes);
        return new IvParameterSpec(inVecBytes);
    }

    private Cipher setupCipher(SecretKeySpec key, IvParameterSpec inVec) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, inVec);
        return cipher;
    }
}
