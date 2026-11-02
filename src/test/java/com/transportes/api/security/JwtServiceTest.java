package com.transportes.api.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class JwtServiceTest {

    private static final String SECRET = "un-secreto-de-pruebas-con-mas-de-32-caracteres";

    @Test
    void generatedTokenCarriesTheUsername() {
        JwtService jwtService = new JwtService(SECRET, 60);

        String token = jwtService.generateToken("carlos");

        assertEquals("carlos", jwtService.extractUsername(token));
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwtService = new JwtService(SECRET, 60);
        String token = jwtService.generateToken("carlos");
        String tampered = token.substring(0, token.length() - 3) + "abc";

        try {
            jwtService.extractUsername(tampered);
            fail("Se esperaba JwtException");
        } catch (JwtException e) {
            assertTrue(true);
        }
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService issuer = new JwtService("otro-secreto-distinto-con-mas-de-32-caracteres", 60);
        JwtService validator = new JwtService(SECRET, 60);
        String token = issuer.generateToken("carlos");

        try {
            validator.extractUsername(token);
            fail("Se esperaba JwtException");
        } catch (JwtException e) {
            assertTrue(true);
        }
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwtService = new JwtService(SECRET, -1);
        String token = jwtService.generateToken("carlos");

        try {
            jwtService.extractUsername(token);
            fail("Se esperaba JwtException");
        } catch (JwtException e) {
            assertTrue(true);
        }
    }

    @Test
    void shortSecretIsNotAccepted() {
        try {
            new JwtService("corto", 60);
            fail("Se esperaba IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("32"));
        }
    }
}
