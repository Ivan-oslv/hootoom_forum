package com.hootoom.forum.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class JwtKeyConfig {
    private static final Logger log = LoggerFactory.getLogger(JwtKeyConfig.class);

    @Bean
    public KeyPair jwtKeyPair(Environment environment,
                              @Value("${app.auth.private-key:}") String privateKey,
                              @Value("${app.auth.public-key:}") String publicKey) throws Exception {
        if (!privateKey.isBlank() && !publicKey.isBlank()) {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return new KeyPair(
                    factory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey))),
                    factory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey))));
        }
        if (!environment.acceptsProfiles(Profiles.of("local", "test"))) {
            throw new IllegalStateException("AUTH_TOKEN_PRIVATE_KEY and AUTH_TOKEN_PUBLIC_KEY are required outside local/test");
        }
        // 本地允许进程级临时密钥；重启会使旧 Access Token 失效，不影响 Refresh Token 换取新令牌。
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        log.warn("ephemeral JWT signing key generated for local/test profile");
        return keyPair;
    }

    @Bean
    public JwtEncoder jwtEncoder(KeyPair keyPair, @Value("${app.auth.key-id}") String keyId) {
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate()).keyID(keyId).build();
        JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(source);
    }

    @Bean
    public JwtDecoder jwtDecoder(KeyPair keyPair, UserAccessTokenValidator userValidator,
                                 @Value("${app.auth.issuer}") String issuer,
                                 @Value("${app.auth.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(audience)
                ? org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success()
                : org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(
                new org.springframework.security.oauth2.core.OAuth2Error("invalid_token", "Invalid audience", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), audienceValidator, userValidator));
        return decoder;
    }
}
