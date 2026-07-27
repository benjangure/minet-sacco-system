package com.minet.sacco.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        try {
            final String requestTokenHeader = request.getHeader("Authorization");
            final String requestMethod = request.getMethod();
            final String requestPath = request.getRequestURI();

            String username = null;
            String jwtToken = null;

            if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
                jwtToken = requestTokenHeader.substring(7);
                try {
                    username = jwtUtil.extractUsername(jwtToken);
                    logger.info("DEBUG: " + requestMethod + " " + requestPath + " - JWT extracted for user: " + username);
                } catch (Exception e) {
                    logger.warn("DEBUG: " + requestMethod + " " + requestPath + " - Unable to extract JWT Token: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                }
            } else if (requestTokenHeader != null) {
                logger.warn("DEBUG: " + requestMethod + " " + requestPath + " - Authorization header present but doesn't start with 'Bearer '");
            } else {
                logger.debug("DEBUG: " + requestMethod + " " + requestPath + " - No Authorization header found");
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                    logger.info("DEBUG: Loaded user: " + username + " with authorities: " + userDetails.getAuthorities());

                    boolean isValid = jwtUtil.validateToken(jwtToken, userDetails);
                    logger.info("DEBUG: Token validation result for " + username + ": " + isValid);
                    
                    if (isValid) {
                        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                        logger.info("DEBUG: Authentication set successfully for " + requestMethod + " " + requestPath + " with user: " + username);
                    } else {
                        logger.warn("DEBUG: Token validation FAILED for user: " + username + " on " + requestMethod + " " + requestPath);
                    }
                } catch (UsernameNotFoundException e) {
                    logger.warn("DEBUG: User not found during authentication: " + e.getMessage());
                } catch (Exception e) {
                    logger.error("DEBUG: Error during authentication for " + requestMethod + " " + requestPath + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("DEBUG: Unexpected error in JwtRequestFilter: " + e.getMessage(), e);
        }
        
        // Always continue filter chain, even if authentication failed
        chain.doFilter(request, response);
    }
}