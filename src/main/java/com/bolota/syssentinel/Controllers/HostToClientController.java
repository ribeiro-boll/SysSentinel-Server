package com.bolota.syssentinel.Controllers;

import com.bolota.syssentinel.Entities.DTOs.SystemEntityDTO;
import com.bolota.syssentinel.Entities.DTOs.SystemVolatileEntityDTO;
import com.bolota.syssentinel.Entities.SystemEntities.SystemEntity;
import com.bolota.syssentinel.Entities.SystemEntities.SystemVolatileEntity;
import com.bolota.syssentinel.Resource.SystemEntityResources;
import com.bolota.syssentinel.Resource.SystemVolatileEntityResources;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;

import static com.bolota.syssentinel.Security.SystemSecurity.getRegisterKey;

@RequestMapping("/api/systems")
@RestController
public class HostToClientController {
    @Autowired
    SystemEntityResources ser;

    @Autowired
    SystemVolatileEntityResources sver;

    @Autowired
    JwtEncoder jwtEncoder;

    @Autowired
    JwtDecoder jwtDecoder;

    @PostMapping(value="/sysinfo", consumes="application/json")
    public ResponseEntity<HashMap> SysEntityHandler(@RequestHeader("JwtToken") String jwttoken,@RequestHeader("RegisterToken") String rgstrtoken, @RequestBody SystemEntity seNew){
        if(seNew.getUUID().equals("null") && jwttoken.equals("null") && rgstrtoken.equals(getRegisterKey())){
            String UUID = genUUID();
            HashMap<String,String> map = new HashMap<>();
            map.put("UUID",UUID);
            map.put("token",issueAgentToken(UUID));
            seNew.setUUID(UUID);
            ser.save(new SystemEntityDTO(seNew));
            return new ResponseEntity<>(map, HttpStatusCode.valueOf(200));
        }
        if(!seNew.getUUID().equals("null") && jwttoken.equals("null") && rgstrtoken.equals(getRegisterKey())){
            if (ser.existsByUUID(seNew.getUUID())){
                HashMap<String,String> map = new HashMap<>();
                map.put("UUID",seNew.getUUID());
                map.put("token",issueAgentToken(seNew.getUUID()));
                ser.save(new SystemEntityDTO(seNew));
                return new ResponseEntity<>(map, HttpStatusCode.valueOf(200));
            }
            else {
                HashMap<String,String> map = new HashMap<>();
                map.put("UUID",null);
                map.put("token",null);
                return new ResponseEntity<>(map, HttpStatusCode.valueOf(401));
            }
        }
        if (seNew.getUUID().equals("null") && !jwttoken.equals("null") && rgstrtoken.equals("null")) {
            HashMap<String, String> map = new HashMap<>();
            map.put("UUID", null);
            map.put("token", null);
            return new ResponseEntity<>(map, HttpStatusCode.valueOf(404));
        }

        if (seNew.getUUID().equals(jwtDecoder.decode(jwttoken).getSubject())){
            if (ser.existsByUUID(seNew.getUUID())){
                ser.deleteByUUID(seNew.getUUID());
            }
            ser.save(new SystemEntityDTO(seNew));
            HashMap<String,String> map = new HashMap<>();
            map.put("UUID",seNew.getUUID());
            map.put("token",jwttoken);
            ser.save(new SystemEntityDTO(seNew));
            return new ResponseEntity<>(map, HttpStatusCode.valueOf(200));
        }
        else {
            HashMap<String,String> map = new HashMap<>();
            map.put("UUID",null);
            map.put("token",null);
            return new ResponseEntity<>(map, HttpStatusCode.valueOf(401));
        }
    }

    @Transactional
    @PostMapping(value="/sysinfovolatile", consumes="application/json")
    public ResponseEntity<Void> SysVolatileHandler(@AuthenticationPrincipal Jwt jwt, @RequestBody SystemVolatileEntity sveNew){
        if (jwt == null){
            return ResponseEntity.status(409).build();
        }
        String jwtSub = jwt.getSubject();
        if (sveNew.getUUID() == null) return  ResponseEntity.status(403).build();

        if (!sveNew.getUUID().equals(jwtSub)){
            return  ResponseEntity.status(403).build();
        }
        if (!ser.existsByUUID(sveNew.getUUID())){
            return ResponseEntity.status(404).build();
        }
        if (sver.existsByUUID(sveNew.getUUID())){
            sver.deleteByUUID(sveNew.getUUID());
            sver.save(new SystemVolatileEntityDTO(sveNew));
            return ResponseEntity.status(200).build();
        }
        sver.save(new SystemVolatileEntityDTO(sveNew));
        return ResponseEntity.status(200).build();
    }

    public String issueAgentToken(String uuid) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("SysSentinelHost")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60L * 60L * 24L * 7L))
                .subject(uuid)
                .claim("roles", List.of("AGENT"))
                .build();
        JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
    }
    public String genUUID(){
        char[] UUID_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        StringBuilder s;
        do{
            s = new StringBuilder();
            for (int i = 0; i < 15;i++){
                s.append(UUID_CHARS[(int)((Math.random() * 100) % UUID_CHARS.length)]);
            }
            System.out.println(s);
        }while (ser.existsByUUID(s.toString()));
        return s.toString();
    }

}
