package com.bolota.syssentinel.Controllers;

import com.bolota.syssentinel.Entities.SystemEntities.SystemEntityPersistent;
import com.bolota.syssentinel.Entities.SystemEntities.SystemVolatileEntityPersistent;
import com.bolota.syssentinel.Entities.SystemEntitiesDTOs.SystemEntityDTO;
import com.bolota.syssentinel.Entities.SystemEntitiesDTOs.SystemVolatileEntityDTO;
import com.bolota.syssentinel.Resource.SystemEntityResources;
import com.bolota.syssentinel.Resource.SystemVolatileEntityResources;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
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
    public ResponseEntity<HashMap> SysEntityHandler(@RequestHeader("JwtToken") String jwttoken,@RequestHeader("RegisterToken") String rgstrtoken, @RequestBody SystemEntityDTO seNew){
        if(seNew.getUUID().equals("null") && jwttoken.equals("null") && rgstrtoken.equals(getRegisterKey())){
            String UUID = genUUID();
            HashMap<String,String> map = new HashMap<>();
            map.put("UUID",UUID);
            map.put("token",issueAgentToken(UUID));
            seNew.setUUID(UUID);
            ser.save(new SystemEntityPersistent(seNew));
            return new ResponseEntity<>(map, HttpStatusCode.valueOf(200));
        }
        if(!seNew.getUUID().equals("null") && jwttoken.equals("null") && rgstrtoken.equals(getRegisterKey())){
            if (ser.existsByUUID(seNew.getUUID())){
                HashMap<String,String> map = new HashMap<>();
                map.put("UUID",seNew.getUUID());
                map.put("token",issueAgentToken(seNew.getUUID()));
                ser.save(new SystemEntityPersistent(seNew));
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
        try{
            if (seNew.getUUID().equals(jwtDecoder.decode(jwttoken).getSubject())){
                HashMap<String,String> map = new HashMap<>();
                map.put("UUID",seNew.getUUID());
                map.put("token",jwttoken);
                ser.save(new SystemEntityPersistent(seNew));
                return new ResponseEntity<>(map, HttpStatusCode.valueOf(200));
            }
            else {
                HashMap<String,String> map = new HashMap<>();
                map.put("UUID",null);
                map.put("token",null);
                return new ResponseEntity<>(map, HttpStatusCode.valueOf(401));
            }
        } catch (JwtValidationException e) {
            HashMap<String,String> map = new HashMap<>();
            map.put("UUID",null);
            map.put("token",null);
            return new ResponseEntity<>(map, HttpStatusCode.valueOf(401));
        }
    }
    @Modifying
    @Transactional
    @PostMapping(value="/sysinfovolatile", consumes="application/json")
    public ResponseEntity<Void> SysVolatileHandler(@AuthenticationPrincipal Jwt jwt, @RequestBody SystemVolatileEntityDTO sveNew){
        if (jwt == null){
            return ResponseEntity.status(401).build();
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
            sver.save(new SystemVolatileEntityPersistent(sveNew));
            return ResponseEntity.status(200).build();
        }
        sver.save(new SystemVolatileEntityPersistent(sveNew));
        return ResponseEntity.status(200).build();
    }
    @GetMapping("/updateAuth")
    public ResponseEntity<HashMap> updateAuth(@RequestHeader ("JwtToken") String jwtTkn, @RequestHeader ("RegisterToken") String regTkn, @RequestHeader ("sysUUID") String uuid){
        if ((jwtTkn.equals("null")) && regTkn.equals(getRegisterKey())){
            HashMap<String,String> map = new HashMap<>();
            map.put("UUID",uuid);
            map.put("token",issueAgentToken(uuid));
            return new ResponseEntity<>(map,HttpStatusCode.valueOf(200));
        }
        HashMap<String,String> map = new HashMap<>();
        map.put("UUID",uuid);
        map.put("token",null);
        return new ResponseEntity<>(map,HttpStatusCode.valueOf(200));
    }
    public String issueAgentToken(String uuid) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("SysSentinelHost")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60L /* * 60L * 3L*/))
                .subject(uuid)
                .claim("roles", List.of("AGENT"))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header,claims)).getTokenValue();
    }
    public String genUUID(){
        char[] UUID_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        StringBuilder s;
        do{
            s = new StringBuilder();
            for (int i = 0; i < 15;i++){
                s.append(UUID_CHARS[(int)((Math.random() * 100) % UUID_CHARS.length)]);
            }
        }while (ser.existsByUUID(s.toString()));
        return s.toString();
    }
}
