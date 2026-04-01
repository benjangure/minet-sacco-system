package com.minet.sacco.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class to generate BCrypt password hashes
 * Run this class to generate hashes for your passwords
 */
public class PasswordHashGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Generate hash for "password"
        String password = "password";
        String hash = encoder.encode(password);
        
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hash);
        System.out.println("Hash Length: " + hash.length());
        
        // Verify the hash works
        boolean matches = encoder.matches(password, hash);
        System.out.println("Verification: " + (matches ? "SUCCESS" : "FAILED"));
        
        System.out.println("\n--- Testing with known hash ---");
        String knownHash = "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi";
        boolean knownMatches = encoder.matches("password", knownHash);
        System.out.println("Known hash matches 'password': " + (knownMatches ? "YES" : "NO"));
        
        // Generate hash for "admin123" as alternative
        String altPassword = "admin123";
        String altHash = encoder.encode(altPassword);
        System.out.println("\nAlternative password: " + altPassword);
        System.out.println("BCrypt Hash: " + altHash);
    }
}
