package com.samjenkins.auth_service.service;

import com.samjenkins.auth_service.config.JwtProperties;
import com.samjenkins.auth_service.entity.UserEntity;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final JwtProperties props;

    public JwtService(JwtEncoder encoder, JwtProperties props) {
        this.encoder = encoder;
        this.props = props;
    }

    public String issueAccessToken(UserEntity user) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.accessTokenMinutes(), ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(props.issuer())
            .issuedAt(now)
            .expiresAt(exp)
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("name", user.getDisplayName())
            .build();

        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
