package com.example.rag.controller;

import com.example.rag.config.BotGatewayProperties;
import com.example.rag.dto.BotMessageRequest;
import com.example.rag.dto.BotMessageResponse;
import com.example.rag.service.BotGatewayService;
import com.example.rag.service.BotSignatureVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/bot")
public class BotGatewayController {
    private final ObjectMapper objectMapper;
    private final BotGatewayProperties properties;
    private final BotSignatureVerifier signatureVerifier;
    private final BotGatewayService botGatewayService;
    private final Validator validator;

    public BotGatewayController(ObjectMapper objectMapper,
                                BotGatewayProperties properties,
                                BotSignatureVerifier signatureVerifier,
                                BotGatewayService botGatewayService,
                                Validator validator) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.signatureVerifier = signatureVerifier;
        this.botGatewayService = botGatewayService;
        this.validator = validator;
    }

    @PostMapping(value = "/{channel}/callback", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> callback(@PathVariable String channel,
                                      @RequestHeader HttpHeaders headers,
                                      @RequestBody String body) throws Exception {
        JsonNode payload = objectMapper.readTree(body);
        if (isFeishuChallenge(channel, payload)) {
            verifyFeishuChallenge(channel, payload);
            return ResponseEntity.ok(Map.of("challenge", payload.path("challenge").asText()));
        }

        signatureVerifier.verify(channel, headers, body);
        BotMessageRequest request = objectMapper.treeToValue(payload, BotMessageRequest.class);
        if (!StringUtils.hasText(request.getChannel())) {
            request.setChannel(channel);
        }
        validate(request);
        BotMessageResponse response = botGatewayService.handle(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/wechat/callback", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verifyWechat(@RequestParam String signature,
                                               @RequestParam String timestamp,
                                               @RequestParam String nonce,
                                               @RequestParam String echostr) {
        var channelProperties = properties.channel("wechat");
        if (!properties.isEnabled() || !channelProperties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "WeChat bot channel is disabled");
        }
        String token = channelProperties.getToken();
        if (!StringUtils.hasText(token)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "WeChat verifier is not configured");
        }
        String expected = sha1(Stream.of(token, timestamp, nonce).sorted().reduce("", String::concat));
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid WeChat signature");
        }
        return ResponseEntity.ok(echostr);
    }

    private boolean isFeishuChallenge(String channel, JsonNode payload) {
        return "feishu".equalsIgnoreCase(channel)
                && payload.hasNonNull("challenge")
                && "url_verification".equals(payload.path("type").asText());
    }

    private void verifyFeishuChallenge(String channel, JsonNode payload) {
        var channelProperties = properties.channel(channel);
        if (!properties.isEnabled() || !channelProperties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Feishu bot channel is disabled");
        }
        String token = channelProperties.getToken();
        if (StringUtils.hasText(token) && !token.equals(payload.path("token").asText())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Feishu verification token");
        }
    }

    private void validate(BotMessageRequest request) {
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.iterator().next().getPropertyPath() + " " + violations.iterator().next().getMessage();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private String sha1(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to verify WeChat signature");
        }
    }
}
