package com.emat.reapi.tally;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class TallySignatureVerifier {
    private final String signingSecret;

    public TallySignatureVerifier(@Value("${tally.signing-secret}") String signingSecret) {
        this.signingSecret = signingSecret;
    }

    public boolean verify(String rawBody, String receivedSignatureBase64) {
        if (receivedSignatureBase64 == null || receivedSignatureBase64.isBlank()) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] calc = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String calcB64 = Base64.getEncoder().encodeToString(calc);
            return MessageDigest.isEqual(
                    calcB64.getBytes(StandardCharsets.UTF_8),
                    receivedSignatureBase64.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }
}
