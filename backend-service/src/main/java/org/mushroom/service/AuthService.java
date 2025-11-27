package org.mushroom.service;

import lombok.RequiredArgsConstructor;
import org.mushroom.entity.User;
import org.mushroom.model.AuthenticationResponse;
import org.mushroom.model.LoginRequest;
import org.mushroom.model.LogoutResponse;
import org.mushroom.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse authenticate(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return generateTokenResponse(user);
    }

    public AuthenticationResponse generateTokenResponse(User user) {
        String jwtToken = jwtService.generateToken(user);
        return new AuthenticationResponse()
                .token(jwtToken)
                .type("Bearer")
                .expiresIn(86_400L);
    }

    public LogoutResponse logout() {
        SecurityContextHolder.clearContext();
        LogoutResponse response = new LogoutResponse();
        response.setMessage("Successfully logged out");
        return response;
    }
}