package com.bolota.syssentinel.Controllers;


import com.bolota.syssentinel.Entities.DTOs.SystemEntityDTO;
import com.bolota.syssentinel.Entities.DTOs.SystemVolatileEntityDTO;
import com.bolota.syssentinel.Entities.UserEntities.UserEntity;
import com.bolota.syssentinel.Resource.SystemEntityResources;
import com.bolota.syssentinel.Resource.SystemVolatileEntityResources;
import com.bolota.syssentinel.Resource.UserEntityResources;
import com.bolota.syssentinel.Service.SysSentinelService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RequestMapping("/api/metrics")
@RestController
public class HostToMetricsFrontendController {

    @Autowired
    SystemEntityResources ser;

    @Autowired
    SystemVolatileEntityResources sver;

    @Autowired
    UserEntityResources uer;

    @GetMapping("/systems")
    public ResponseEntity<Page<SystemEntityDTO>> sendSystems(@AuthenticationPrincipal Jwt jwt, @RequestHeader("login") String login, @PageableDefault(size = 10) Pageable pageable){
        if (jwt == null) return ResponseEntity.status(409).build();
        if (login == null) return ResponseEntity.status(404).build();
        if (!jwt.getSubject().equals(login)) return ResponseEntity.status(401).build();
        System.out.println(login);
        ArrayList<String> systemsUUIDs = uer.getUserEntityByLogin(login).getSystemsInPossession();
        System.out.println(systemsUUIDs);
        ArrayList<SystemEntityDTO> systems = new ArrayList<>();
        for(String uuids: systemsUUIDs){
            systems.add(ser.getByUUID(uuids));
        }
        return ResponseEntity.ok(toPage(systems,pageable));
    }
    @GetMapping("/systemVolatileInfo")
    public ResponseEntity<SystemVolatileEntityDTO> sendSystemVolatileInfo(@AuthenticationPrincipal Jwt jwt, @RequestHeader("login") String login, @RequestParam String uuid){
        System.out.println("teste 1");
        if (jwt == null) return ResponseEntity.status(409).build();
        System.out.println("teste 2");
        if (login == null) return ResponseEntity.status(404).build();
        System.out.println("teste 3");
        if (!jwt.getSubject().equals(login)) return ResponseEntity.status(401).build();
        System.out.println("teste 4");
        System.out.println(uuid);
        if (!sver.existsByUUID(uuid)) return ResponseEntity.status(404).build();
        System.out.println("teste 5");
        return ResponseEntity.ok(sver.getByUUID(uuid));
    }
    @Modifying
    @Transactional
    @PostMapping("/systemRegister")
    public ResponseEntity<Void> registerHandler(@AuthenticationPrincipal Jwt jwt, @RequestHeader("login") String login, @RequestParam String uuid){
        if (login == null) return ResponseEntity.badRequest().build();
        if (login.equals(" ") ||login.isEmpty()) return ResponseEntity.badRequest().build();
        if (!uer.existsByLogin(login)) return ResponseEntity.status(409).build();
        if (!jwt.getSubject().equals(login)) return ResponseEntity.status(401).build();
        System.out.println(uuid);
        UserEntity ue = uer.getUserEntityByLogin(login);
        ue.addSystem(uuid);
        uer.save(ue);
        return ResponseEntity.ok().build();
    }
    public static <T> Page<T> toPage(List<T> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());

        if (start > end) {
            return new PageImpl<>(List.of(), pageable, list.size());
        }
        return new PageImpl<>(list.subList(start, end), pageable, list.size());
    }
}
