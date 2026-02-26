package com.bolota.syssentinel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.bolota.syssentinel.Security.SystemSecurity.*;

@SpringBootApplication
public class SysSentinelApplication {
    public static String getLocalIpFast() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            return localHost.getHostAddress();  // ex: 192.168.0.50
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }
    public static void assertAuthKey(){
        if (!isAuthFilePresent()){
            registerAuthKey();
        }
    }

    public static void main(String[] args) {
        System.out.println(getLocalIpFast()+":"+"8080");
        assertAuthKey();
        SpringApplication.run(SysSentinelApplication.class, args);
    }
}
