package com.example.rag.service;

import com.example.rag.config.BotGatewayProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class BotSignatureVerifier {
    private static final String SIGNATURE_HEADER = "X-Bot-Signature";
    private static final String TIMESTAMP_HEADER = "X-Bot-Timestamp";
    private static final String TOKEN_HEADER = "X-Bot-Token";

    private final BotGatewayProperties properties;

    public BotSignatureVerifier(BotGatewayProperties properties) {
        this.properties = properties;
    }

    public void verify(String channel, HttpHeaders headers, String body) {
        if (!properties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bot gateway is disabled");
        }

        BotGatewayProperties.ChannelProperties channelProperties = properties.channel(channel);
        if (!channelProperties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bot channel is disabled");
        }

        String token = channelProperties.getToken();
        String providedToken = headers.getFirst(TOKEN_HEADER);
        if (StringUtils.hasText(token) && secureEquals(token, providedToken)) {
            return;
        }

        String secret = StringUtils.hasText(channelProperties.getSigningSecret())
                ? channelProperties.getSigningSecret()
                : properties.getSigningSecret();
        if (!StringUtils.hasText(secret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bot channel has no verifier configured");
        }

        String timestamp = headers.getFirst(TIMESTAMP_HEADER);
        String signature = headers.getFirst(SIGNATURE_HEADER);
        if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(signature)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bot signature headers");
        }
        assertTimestampFresh(timestamp);

        String expected = hmacSha256Hex(secret, timestamp + "." + body);
        String normalized = signature.startsWith("sha256=") ? signature.substring("sha256=".length()) : signature;
        if (!secureEquals(expected, normalized)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid bot signature");
        }
    }

    private void assertTimestampFresh(String timestampValue) {
        try {
            long raw = Long.parseLong(timestampValue);
            long seconds = raw > 10_000_000_000L ? raw / 1000 : raw;
            long age = Math.abs(Instant.now().getEpochSecond() - seconds);
            if (age > properties.getTimestampToleranceSeconds()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Expired bot signature");
            }
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid bot timestamp");
        }
    }

    private String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to verify bot signature");
        }
    }

    private boolean secureEquals(String expected, String actual) {
        if (!StringUtils.hasText(expected) || !StringUtils.hasText(actual)) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }
}
