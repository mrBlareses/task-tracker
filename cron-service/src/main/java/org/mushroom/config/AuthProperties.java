package org.mushroom.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {
    private String baseUrl;
    private String loginEndpoint;
    private String email;
    private String password;
}