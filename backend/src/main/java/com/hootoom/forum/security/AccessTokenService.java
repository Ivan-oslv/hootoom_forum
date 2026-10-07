package com.hootoom.forum.security;

import com.hootoom.forum.auth.entity.ForumUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class AccessTokenService {
    private final JwtEncoder encoder;
    private final String issuer;
    private final String audience;
    private final String keyId;
    private final Duration lifetime;
    private final Clock clock = Clock.systemUTC();

    public AccessTokenService(JwtEncoder encoder,
                              @Value("${app.auth.issuer}") String issuer,
                              @Value("${app.auth.audience}") String audience,
                              @Value("${app.auth.key-id}") String keyId,
                              @Value("${app.auth.access-token-minutes:15}") long minutes) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.audience = audience;
        this.keyId = keyId;
        this.lifetime = Duration.ofMinutes(minutes);
    }

    public String issue(ForumUser user) {
        Instant issuedAt = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer).audience(List.of(audience)).subject(String.valueOf(user.getId()))
                .id(UUID.randomUUID().toString()).issuedAt(issuedAt).expiresAt(issuedAt.plus(lifetime))
                .claim("tokenVersion", user.getTokenVersion()).claim("subjectType", "USER").build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(keyId).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long expiresInSeconds() {
        return lifetime.toSeconds();
    }
}
