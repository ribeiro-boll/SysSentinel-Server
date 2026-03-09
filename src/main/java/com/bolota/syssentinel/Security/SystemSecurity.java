package com.bolota.syssentinel.Security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Scanner;

public class SystemSecurity {

    private static final String filePath = "SysSentinelConfigs/Security/authKey.config";

    public static boolean isAuthFilePresent(){
        File file = new File(filePath);
        return file.exists();
    }

    public static void registerAuthKey() throws IOException {
        try (Scanner scanner = new Scanner(System.in)) {
            File file = new File(filePath);
            File file1 = new File("SysSentinelConfigs/Security");
            file1.mkdirs();
            String jwtToken;
            String registerKey;
            System.out.println("Digite a key do JWT !(NÂO USE ESPAÇOS E NÃO DIVULGUE A KEY)! :");
            jwtToken = scanner.nextLine();
            System.out.println("Digite a chave de registro !(NÃO USE ESPAÇOS E GARANTA QUE A CHAVE SEJA IDÊNTICA COM A DO CLIENTE)! :");
            registerKey = scanner.nextLine();
            try (FileWriter fw = new FileWriter(file)) {
                fw.write("AuthKey=" + jwtToken.trim() + "\n");
                fw.write("RegisterKey=" + registerKey.trim());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public static String getAuthKey(){
        File file = new File(filePath);
        try(Scanner scanner = new Scanner(file)) {
            return scanner.nextLine().substring(8);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static String getRegisterKey(){
        File file = new File(filePath);
        try(Scanner scanner = new Scanner(file)) {
            scanner.nextLine();
            return scanner.nextLine().substring(12);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}