package com.bolota.syssentinel.Controllers;

import com.bolota.syssentinel.Entities.UserEntities.UserEntity;
import com.bolota.syssentinel.Resource.UserEntityResources;
import org.aspectj.weaver.IClassFileProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;


@RequestMapping("/api/user")
@RestController
public class HostToUserFrontendController {
    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    UserEntityResources uer;

    @Autowired
    JwtEncoder jwtEncoder;

    @PostMapping("/login")
    public ResponseEntity<String> loginHandler(@RequestParam HashMap<String,String> loginInfo){
        if (loginInfo==null) return ResponseEntity.badRequest().build();
        if (loginInfo.get("login") == null || loginInfo.get("password") == null) return ResponseEntity.badRequest().build();
        if (!uer.existsByLogin(loginInfo.get("login"))) return ResponseEntity.notFound().build();
        UserEntity ue = uer.getUserEntityByLogin(loginInfo.get("login"));
        if (!passwordEncoder.matches(loginInfo.get("password"),ue.getPasswordHash())) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(issueLoginToken(loginInfo.get("login")));
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerHandler(@RequestParam HashMap<String,String> loginInfo){
        if (loginInfo==null) return ResponseEntity.badRequest().build();
        if (loginInfo.get("login") == null || loginInfo.get("password") == null) return ResponseEntity.badRequest().build();
        if (loginInfo.get("login").trim().isEmpty()||loginInfo.get("password").trim().isEmpty()) return ResponseEntity.badRequest().build();
        if (uer.existsByLogin(loginInfo.get("login"))) return ResponseEntity.status(409).build();
        uer.save(new UserEntity(loginInfo.get("login"), passwordEncoder.encode(loginInfo.get("password"))));
        return ResponseEntity.ok(issueLoginToken(loginInfo.get("login")));
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
