package lexek.wschat.security;

import com.google.common.io.BaseEncoding;

import java.security.SecureRandom;

public class SecureTokenGenerator {
    private final SecureRandom secureRandom = new SecureRandom();

    String generateVerificationCode() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return BaseEncoding.base32Hex().encode(bytes);
    }

    String generateSessionId() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return BaseEncoding.base16().encode(bytes);
    }

    public String generateRandomToken(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return BaseEncoding.base64Url().encode(bytes);
    }

    public void nextBytes(byte[] bytes) {
        secureRandom.nextBytes(bytes);
    }
}
