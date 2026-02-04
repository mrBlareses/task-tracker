package org.mushroom.config;

import lombok.RequiredArgsConstructor;
import org.mushroom.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserRepository userRepository;

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("UTC+3"));
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}