package com.haiintel.authservice.adapter.rest;

import com.haiintel.authservice.domain.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * REST controller for JWKS (JSON Web Key Set) endpoint.
 * 
 * ✅ V2.1.1 FIX: Correct modulus encoding (Base64 URL-safe, no padding)
 * ✅ P1 FIX: kid = timestamp-based (supports multiple rotations per day)
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "JWKS", description = "JSON Web Key Set endpoint")
public class JwksController {
    
    private final JwtService jwtService;
    
    @GetMapping("/.well-known/jwks.json")
    @Operation(summary = "Get JWKS", 
               description = "Get JSON Web Key Set for JWT signature verification")
    public ResponseEntity<Map<String, Object>> getJwks() {
        PublicKey publicKey = jwtService.getPublicKey();
        RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
        
        // ✅ V2.1.1 FIX: Correct Base64 URL-safe encoding without padding
        String modulus = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(rsaPublicKey.getModulus().toByteArray());
        
        String exponent = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(rsaPublicKey.getPublicExponent().toByteArray());
        
        // ✅ P1 FIX: Timestamp-based kid
        String kid = String.valueOf(Instant.now().getEpochSecond());
        
        Map<String, Object> jwk = Map.of(
            "kty", "RSA",
            "use", "sig",
            "alg", "RS256",
            "kid", kid,
            "n", modulus,
            "e", exponent
        );
        
        Map<String, Object> jwks = Map.of("keys", List.of(jwk));
        
        log.debug("JWKS requested, kid={}", kid);
        
        return ResponseEntity.ok(jwks);
    }
}

