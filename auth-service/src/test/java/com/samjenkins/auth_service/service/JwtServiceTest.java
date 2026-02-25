package com.samjenkins.auth_service.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jwt.SignedJWT;
import com.samjenkins.auth_service.config.JwtProperties;
import com.samjenkins.auth_service.entity.UserEntity;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void issueAccessTokenSetsExpectedHeaderAndClaims() throws Exception {
        JwtProperties props = new JwtProperties(
            "budgeting-auth-test",
            "budgeting-api-test",
            "0123456789abcdef0123456789abcdef",
            15,
            14
        );

        SecretKey key = new SecretKeySpec(props.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        JwtService jwtService = new JwtService(encoder, props);

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setDisplayName("Test User");

        String token = jwtService.issueAccessToken(user);
        SignedJWT parsed = SignedJWT.parse(token);

        assertThat(parsed.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.HS256);
        assertThat(parsed.getJWTClaimsSet().getIssuer()).isEqualTo(props.issuer());
        assertThat(parsed.getJWTClaimsSet().getAudience()).isEqualTo(List.of(props.audience()));
        assertThat(parsed.getJWTClaimsSet().getSubject()).isEqualTo(user.getId().toString());
        assertThat(parsed.getJWTClaimsSet().getStringClaim("email")).isEqualTo(user.getEmail());
        assertThat(parsed.getJWTClaimsSet().getStringClaim("name")).isEqualTo(user.getDisplayName());
        assertThat(parsed.getJWTClaimsSet().getExpirationTime()).isAfter(parsed.getJWTClaimsSet().getIssueTime());
    }
}
