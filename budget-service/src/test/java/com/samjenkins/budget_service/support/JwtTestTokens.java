package com.samjenkins.budget_service.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public final class JwtTestTokens {
    private JwtTestTokens() {}

    public static String valid(UUID subject) {
        return token(IntegrationTestSupport.TEST_ISSUER, subject.toString(), Instant.now().plusSeconds(900));
    }

    public static String wrongIssuer(UUID subject) {
        return token("wrong-issuer", subject.toString(), Instant.now().plusSeconds(900));
    }

    public static String malformedSubject() {
        return token(IntegrationTestSupport.TEST_ISSUER, "not-a-uuid", Instant.now().plusSeconds(900));
    }

    private static String token(String issuer, String subject, Instant expiresAt) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(subject)
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(expiresAt))
                .jwtID(UUID.randomUUID().toString())
                .build();

            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(IntegrationTestSupport.TEST_SECRET.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (JOSEException ex) {
            throw new IllegalStateException("Failed generating test JWT", ex);
        }
    }
}
