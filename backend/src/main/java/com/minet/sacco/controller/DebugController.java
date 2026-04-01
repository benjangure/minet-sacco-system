package com.minet.sacco.controller;

import com.minet.sacco.entity.User;
import com.minet.sacco.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@CrossOrigin
public class DebugController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/check-password/{username}")
    public Map<String, Object> checkPassword(@PathVariable String username, @RequestParam String password) {
        Map<String, Object> result = new HashMap<>();
        
        User user = userRepository.findByUsername(username).orElse(null);
        
        if (user == null) {
            result.put("error", "User not found");
            return result;
        }
        
        result.put("username", user.getUsername());
        result.put("storedHash", user.getPassword());
        result.put("hashLength", user.getPassword().length());
        result.put("role", user.getRole());
        result.put("enabled", user.getEnabled());
        result.put("passwordMatches", passwordEncoder.matches(password, user.getPassword()));
        
        // Generate a fresh hash for comparison
        String freshHash = passwordEncoder.encode(password);
        result.put("freshHashForSamePassword", freshHash);
        result.put("freshHashMatches", passwordEncoder.matches(password, freshHash));
        
        return result;
    }

    @GetMapping("/generate-hash")
    public Map<String, String> generateHash(@RequestParam String password) {
        Map<String, String> result = new HashMap<>();
        String hash = passwordEncoder.encode(password);
        result.put("password", password);
        result.put("hash", hash);
        result.put("length", String.valueOf(hash.length()));
        result.put("verified", String.valueOf(passwordEncoder.matches(password, hash)));
        return result;
    }
}




