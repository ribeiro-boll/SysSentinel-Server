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

    private static final String filePath = "src/main/java/com/bolota/syssentinel/Security/authKey.config";

    public static boolean isAuthFilePresent(){
        File file = new File(filePath);
        return file.exists();
    }

    public static void registerAuthKey() throws IOException {
        try (Scanner scanner = new Scanner(System.in)) {
            File file = new File(filePath);
            String jwtToken;
            String registerKey;
            System.out.println("Enter the Jwt Key !(spaces will be trimmed)! :");
            jwtToken = scanner.nextLine();
            System.out.println("Enter the Register Key !(spaces will be trimmed)! :");
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