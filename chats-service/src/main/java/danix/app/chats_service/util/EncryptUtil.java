package danix.app.chats_service.util;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

@Component
public class EncryptUtil {

    private final Key secretKey;

    private static final String ALGORITHM = "AES";

    public EncryptUtil(@Value("${encrypt-secret-key}") String secretKey) {
        this.secretKey = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
    }

    @SneakyThrows
    public String encrypt(String data) {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    @SneakyThrows
    public String decrypt(String encryptedData) {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedBytes);
    }

}
