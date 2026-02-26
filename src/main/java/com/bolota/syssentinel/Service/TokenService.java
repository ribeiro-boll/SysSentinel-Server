package com.bolota.syssentinel.Service;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;

    public TokenService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String issueAgentToken(String uuid) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("SysSentinelHost")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60L * 60L * 24L * 7L)) // 7 dias
                .subject(uuid) // sub = uuid do agente
                .claim("roles", List.of("AGENT"))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public String issueLoginToken(String login) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("SysSentinelHost")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60L * 60L * 3L))
                .subject(login)
                .claim("roles", List.of("USER"))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}