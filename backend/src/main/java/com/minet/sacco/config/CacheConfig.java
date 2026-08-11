package com.minet.sacco.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Cache Configuration for Performance Optimization
 * 
 * Defines in-memory caches for frequently accessed data:
 * - users: User lookup cache (TTL: 5 minutes in production)
 * - members: Member data cache
 * - loans: Loan data cache
 * - accounts: Account balance cache
 * 
 * Uses simple in-memory caching (ConcurrentHashMap) for development.
 * For production, consider using Redis or Caffeine with TTL support.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        
        // Define all cache names used in @Cacheable annotations across the application
        cacheManager.setCaches(Arrays.asList(
            // User caches
            new ConcurrentMapCache("users"),
            
            // Member caches
            new ConcurrentMapCache("members"),
            new ConcurrentMapCache("memberById"),
            new ConcurrentMapCache("memberByNumber"),
            new ConcurrentMapCache("membersByStatus"),
            
            // Loan caches
            new ConcurrentMapCache("loans"),
            new ConcurrentMapCache("allLoans"),
            new ConcurrentMapCache("loanById"),
            new ConcurrentMapCache("loansByMember"),
            new ConcurrentMapCache("loansByStatus"),
            
            // Account caches
            new ConcurrentMapCache("accounts"),
            
            // Other entity caches
            new ConcurrentMapCache("loanProducts"),
            new ConcurrentMapCache("guarantors"),
            
            // Notification caches
            new ConcurrentMapCache("unreadCount"),
            
            // Report caches
            new ConcurrentMapCache("cashbookReport"),
            new ConcurrentMapCache("trialBalanceReport"),
            new ConcurrentMapCache("balanceSheetReport"),
            new ConcurrentMapCache("loanRegisterReport"),
            
            // System settings cache
            new ConcurrentMapCache("systemSettings")
        ));
        
        return cacheManager;
    }
}
