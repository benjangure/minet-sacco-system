package com.minet.sacco.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time updates across the SACCO system.
 * Enables instant notifications when:
 * - Loans are created, updated, approved, or disbursed
 * - Guarantor requests need attention
 * - Transactions are processed
 * - Member data changes
 * - Top-up requests are submitted or processed
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker for broadcasting to subscribers
        config.enableSimpleBroker(
            "/topic",    // Public broadcasts (e.g., system-wide notifications)
            "/queue"     // Private messages (e.g., user-specific notifications)
        );
        
        // Prefix for messages bound for @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register WebSocket endpoint that clients will connect to
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*", "http://192.168.*:*")
                .withSockJS();  // Enable SockJS fallback for browsers that don't support WebSocket
    }
}
