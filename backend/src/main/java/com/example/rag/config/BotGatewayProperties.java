package com.example.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "bot")
public class BotGatewayProperties {
    private boolean enabled;
    private String defaultMode = "rag";
    private String signingSecret = "";
    private long timestampToleranceSeconds = 300;
    private Map<String, ChannelProperties> channels = new HashMap<>();

    public ChannelProperties channel(String name) {
        return channels.getOrDefault(name.toLowerCase(), new ChannelProperties());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultMode() {
        return defaultMode;
    }

    public void setDefaultMode(String defaultMode) {
        this.defaultMode = defaultMode;
    }

    public String getSigningSecret() {
        return signingSecret;
    }

    public void setSigningSecret(String signingSecret) {
        this.signingSecret = signingSecret;
    }

    public long getTimestampToleranceSeconds() {
        return timestampToleranceSeconds;
    }

    public void setTimestampToleranceSeconds(long timestampToleranceSeconds) {
        this.timestampToleranceSeconds = timestampToleranceSeconds;
    }

    public Map<String, ChannelProperties> getChannels() {
        return channels;
    }

    public void setChannels(Map<String, ChannelProperties> channels) {
        this.channels = channels;
    }

    public static class ChannelProperties {
        private boolean enabled;
        private String token = "";
        private String signingSecret = "";
        private List<String> allowedModes = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getSigningSecret() {
            return signingSecret;
        }

        public void setSigningSecret(String signingSecret) {
            this.signingSecret = signingSecret;
        }

        public List<String> getAllowedModes() {
            return allowedModes;
        }

        public void setAllowedModes(List<String> allowedModes) {
            this.allowedModes = allowedModes;
        }
    }
}
