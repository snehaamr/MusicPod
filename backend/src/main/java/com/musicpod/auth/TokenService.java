package com.musicpod.auth;

import java.time.Instant;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.musicpod.user.UserAccount;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public TokenService(
            JwtEncoder jwtEncoder,
            JwtProperties jwtProperties) {

        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
    }

    public String createAccessToken(
            UserAccount user) {

        Instant now = Instant.now();

        Instant expiresAt =
                now.plus(
                        jwtProperties.accessTokenTtl()
                );

        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(
                                jwtProperties.issuer()
                        )
                        .issuedAt(now)
                        .expiresAt(expiresAt)
                        .subject(
                                user.getId().toString()
                        )
                        .claim(
                                "email",
                                user.getEmail()
                        )
                        .build();

        return jwtEncoder
                .encode(
                        JwtEncoderParameters.from(
                                claims
                        )
                )
                .getTokenValue();
    }

    public long getAccessTokenTtlSeconds() {

        return jwtProperties
                .accessTokenTtl()
                .toSeconds();
    }
}