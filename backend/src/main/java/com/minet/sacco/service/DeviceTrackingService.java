package com.minet.sacco.service;

import com.minet.sacco.entity.LoginHistory;
import com.minet.sacco.entity.User;
import com.minet.sacco.entity.UserDevice;
import com.minet.sacco.repository.LoginHistoryRepository;
import com.minet.sacco.repository.UserDeviceRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class DeviceTrackingService {

    @Autowired
    private UserDeviceRepository userDeviceRepository;

    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

    @Autowired(required = false)
    private NotificationService notificationService;

    @Autowired(required = false)
    private EmailNotificationService emailNotificationService;

    @Autowired(required = false)
    private RealtimeNotificationService realtimeNotificationService;

    /**
     * Track user login and send notification for new devices
     */
    @Transactional
    public void trackLogin(User user, HttpServletRequest request, boolean loginSuccess, String failureReason) {
        String deviceFingerprint = generateDeviceFingerprint(request);
        String ipAddress = getClientIpAddress(request);
        String deviceInfo = extractDeviceInfo(request);
        String location = extractLocation(ipAddress);

        if (loginSuccess) {
            handleSuccessfulLogin(user, deviceFingerprint, ipAddress, deviceInfo, location);
        }

        // Log login attempt
        logLoginAttempt(user, deviceFingerprint, ipAddress, deviceInfo, location, 
                       loginSuccess ? "SUCCESS" : "FAILED", failureReason);
    }

    /**
     * Handle successful login - track device and send notifications
     */
    private void handleSuccessfulLogin(User user, String deviceFingerprint, 
                                       String ipAddress, String deviceInfo, String location) {
        Optional<UserDevice> existingDevice = userDeviceRepository
            .findByUserAndDeviceFingerprint(user, deviceFingerprint);

        if (existingDevice.isPresent()) {
            // Known device - update last login
            UserDevice device = existingDevice.get();
            device.setLastLoginAt(LocalDateTime.now());
            device.setLoginCount(device.getLoginCount() + 1);
            device.setIpAddress(ipAddress);
            device.setLocation(location);
            userDeviceRepository.save(device);
        } else {
            // New device - create record and send notification
            UserDevice newDevice = createNewDevice(user, deviceFingerprint, ipAddress, deviceInfo, location);
            userDeviceRepository.save(newDevice);
            
            // Send new device login notification
            sendNewDeviceNotification(user, newDevice);
        }
    }

    /**
     * Create a new device record
     */
    private UserDevice createNewDevice(User user, String deviceFingerprint, 
                                      String ipAddress, String deviceInfo, String location) {
        UserDevice device = new UserDevice();
        device.setUser(user);
        device.setDeviceFingerprint(deviceFingerprint);
        device.setIpAddress(ipAddress);
        device.setLocation(location);
        
        // Parse device info
        DeviceDetails details = parseDeviceInfo(deviceInfo);
        device.setDeviceName(details.deviceName);
        device.setDeviceType(details.deviceType);
        device.setBrowser(details.browser);
        device.setOperatingSystem(details.operatingSystem);
        
        device.setFirstLoginAt(LocalDateTime.now());
        device.setLastLoginAt(LocalDateTime.now());
        device.setLoginCount(1);
        device.setIsTrusted(false);
        
        return device;
    }

    /**
     * Send notification for new device login
     */
    private void sendNewDeviceNotification(User user, UserDevice device) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"));

        // Build the rich HTML email body used by both paths below
        String emailSubject = "🔐 New Device Login Alert - Minet SACCO";
        String emailMessage = String.format(
            "<p><strong>A new device just logged into your Minet SACCO account.</strong></p>" +
            "<div style='background: #f9fafb; border-left: 4px solid #dc2626; padding: 16px; margin: 16px 0;'>" +
            "<p><strong>Device Details:</strong></p>" +
            "<ul style='margin: 8px 0; padding-left: 20px;'>" +
            "<li><strong>Device:</strong> %s</li>" +
            "<li><strong>Browser:</strong> %s</li>" +
            "<li><strong>Operating System:</strong> %s</li>" +
            "<li><strong>IP Address:</strong> %s</li>" +
            "<li><strong>Location:</strong> %s</li>" +
            "<li><strong>Time:</strong> %s</li>" +
            "</ul>" +
            "</div>" +
            "<p><strong>Was this you?</strong></p>" +
            "<p>If you recognize this activity, you can safely ignore this email.</p>" +
            "<p><strong style='color: #dc2626;'>If you did NOT log in:</strong></p>" +
            "<ol style='margin: 8px 0; padding-left: 20px;'>" +
            "<li>Change your password immediately</li>" +
            "<li>Contact our support team</li>" +
            "<li>Review your account activity</li>" +
            "</ol>" +
            "<p style='margin-top: 16px;'><em>This is an automated security notification from Minet SACCO.</em></p>",
            device.getDeviceName() != null ? device.getDeviceName() : "Unknown Device",
            device.getBrowser() != null ? device.getBrowser() : "Unknown Browser",
            device.getOperatingSystem() != null ? device.getOperatingSystem() : "Unknown OS",
            device.getIpAddress() != null ? device.getIpAddress() : "Unknown",
            device.getLocation() != null ? device.getLocation() : "Unknown Location",
            timestamp
        );

        if (notificationService != null) {
            // notificationService.notifyUser() saves the in-app notification AND sends the email
            // internally — so we only call it once to avoid duplicate emails.
            notificationService.notifyUser(
                user.getId(),
                emailMessage,
                "SECURITY_ALERT",
                null,
                null,
                "NEW_DEVICE_LOGIN"
            );
        } else if (emailNotificationService != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
            // Fallback: notificationService is unavailable, send email directly
            try {
                emailNotificationService.sendNotificationEmail(
                    user.getEmail(),
                    emailSubject,
                    emailMessage,
                    "SECURITY_ALERT"
                );
            } catch (Exception e) {
                System.err.println("Failed to send new device email notification: " + e.getMessage());
            }
        }

        // Send real-time WebSocket notification
        if (realtimeNotificationService != null) {
            realtimeNotificationService.broadcastNotification(
                String.format("New device login for %s %s from %s", 
                    user.getFirstName(), 
                    user.getLastName(),
                    device.getLocation() != null ? device.getLocation() : "Unknown Location"),
                "NEW_DEVICE_LOGIN"
            );
        }
    }

    /**
     * Log login attempt to history
     */
    private void logLoginAttempt(User user, String deviceFingerprint, String ipAddress,
                                 String deviceInfo, String location, String status, String failureReason) {
        LoginHistory loginHistory = new LoginHistory();
        loginHistory.setUser(user);
        loginHistory.setUsername(user.getUsername());
        loginHistory.setDeviceFingerprint(deviceFingerprint);
        loginHistory.setIpAddress(ipAddress);
        loginHistory.setDeviceInfo(deviceInfo);
        loginHistory.setLocation(location);
        loginHistory.setLoginStatus(status);
        loginHistory.setFailureReason(failureReason);
        loginHistory.setLoginTimestamp(LocalDateTime.now());
        
        // Link to device if it exists
        userDeviceRepository.findByUserAndDeviceFingerprint(user, deviceFingerprint)
            .ifPresent(loginHistory::setDevice);
        
        loginHistoryRepository.save(loginHistory);
    }

    /**
     * Generate device fingerprint from request
     */
    private String generateDeviceFingerprint(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String acceptLanguage = request.getHeader("Accept-Language");
        String acceptEncoding = request.getHeader("Accept-Encoding");
        
        // Create a simple fingerprint (in production, use more sophisticated method)
        String fingerprint = String.format("%s|%s|%s", 
            userAgent != null ? userAgent : "",
            acceptLanguage != null ? acceptLanguage : "",
            acceptEncoding != null ? acceptEncoding : ""
        );
        
        return Integer.toHexString(fingerprint.hashCode());
    }

    /**
     * Extract client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Extract device information from User-Agent
     */
    private String extractDeviceInfo(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    /**
     * Extract or estimate location from IP address
     */
    private String extractLocation(String ipAddress) {
        // In production, integrate with a GeoIP service like MaxMind or IP-API
        // For now, return a placeholder
        if (ipAddress == null) {
            return "Unknown Location";
        }
        
        // Check for local/private IPs
        if (ipAddress.startsWith("192.168.") || ipAddress.startsWith("10.") || 
            ipAddress.startsWith("172.") || ipAddress.equals("127.0.0.1") ||
            ipAddress.equals("0:0:0:0:0:0:0:1") || ipAddress.equals("::1")) {
            return "Local Network";
        }
        
        // For production, call GeoIP API here
        return "Kenya"; // Default for now
    }

    /**
     * Parse device info from User-Agent string
     */
    private DeviceDetails parseDeviceInfo(String userAgent) {
        DeviceDetails details = new DeviceDetails();
        
        if (userAgent == null) {
            return details;
        }
        
        String ua = userAgent.toLowerCase();
        
        // Detect device type
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            details.deviceType = "MOBILE";
        } else if (ua.contains("tablet") || ua.contains("ipad")) {
            details.deviceType = "TABLET";
        } else {
            details.deviceType = "DESKTOP";
        }
        
        // Detect browser
        if (ua.contains("edg/")) {
            details.browser = "Microsoft Edge";
        } else if (ua.contains("chrome/")) {
            details.browser = "Google Chrome";
        } else if (ua.contains("firefox/")) {
            details.browser = "Mozilla Firefox";
        } else if (ua.contains("safari/") && !ua.contains("chrome")) {
            details.browser = "Apple Safari";
        } else if (ua.contains("opera") || ua.contains("opr/")) {
            details.browser = "Opera";
        } else {
            details.browser = "Unknown Browser";
        }
        
        // Detect OS
        if (ua.contains("windows nt 10")) {
            details.operatingSystem = "Windows 10/11";
        } else if (ua.contains("windows nt")) {
            details.operatingSystem = "Windows";
        } else if (ua.contains("mac os x")) {
            details.operatingSystem = "macOS";
        } else if (ua.contains("android")) {
            details.operatingSystem = "Android";
        } else if (ua.contains("iphone") || ua.contains("ipad")) {
            details.operatingSystem = "iOS";
        } else if (ua.contains("linux")) {
            details.operatingSystem = "Linux";
        } else {
            details.operatingSystem = "Unknown OS";
        }
        
        // Generate device name
        details.deviceName = String.format("%s on %s", details.browser, details.operatingSystem);
        
        return details;
    }

    /**
     * Inner class to hold parsed device details
     */
    private static class DeviceDetails {
        String deviceName = "Unknown Device";
        String deviceType = "DESKTOP";
        String browser = "Unknown Browser";
        String operatingSystem = "Unknown OS";
    }
}
