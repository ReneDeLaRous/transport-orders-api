package com.transportes.api.auth;

import com.transportes.api.auth.dto.AuthResponse;
import com.transportes.api.auth.dto.LoginRequest;
import com.transportes.api.auth.dto.RegisterRequest;
import com.transportes.api.common.exception.DuplicateResourceException;
import com.transportes.api.security.JwtService;
import com.transportes.api.user.AppUser;
import com.transportes.api.user.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String TOKEN_TYPE = "Bearer";

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AppUserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("El usuario '" + username + "' ya existe");
        }

        userRepository.save(new AppUser(username, passwordEncoder.encode(request.password())));
        log.atInfo()
                .addKeyValue("username", username)
                .log("user registered");
        return buildResponse(username);
    }

    public AuthResponse login(LoginRequest request) {
        String username = request.username().trim();

        // Si las credenciales no son correctas, esto lanza una AuthenticationException (se responde 401)
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, request.password()));

        log.atInfo()
                .addKeyValue("username", username)
                .log("user logged in");
        return buildResponse(username);
    }

    private AuthResponse buildResponse(String username) {
        return new AuthResponse(jwtService.generateToken(username), TOKEN_TYPE, jwtService.getExpirationMinutes());
    }
}
