# Member Onboarding Email & Password Change Implementation Plan

## Executive Summary

This document outlines the complete implementation strategy for:
1. **Email notifications** to newly onboarded members with login credentials & APK download link
2. **Member self-service password change** capability in the member portal
3. **Customer Support staff permissions** to manage member passwords
4. **Secure first-time login** experience with credentials provided via email

---

## Current System State

### ✅ What's Already in Place
- **Email Infrastructure**: Spring Boot Mail starter already in pom.xml
- **Email Configuration**: application.properties has SendGrid SMTP config (needs your API key)
- **Member Entity**: Has email field, phone, nationalId, employeeId
- **User-Member Link**: User table has `memberId` field linking to member
- **Initial Credentials**: Member username = employeeId/memberNumber, password = nationalId
- **Member Approval Flow**: MemberService.approveMember() creates user accounts
- **Member Onboarding Created**: Creates default accounts and user credentials
- **Activity Logging**: System logs all major actions

### ❌ What's Missing
- **Email Service**: No EmailService implementation
- **Email Templates**: No HTML email templates
- **Onboarding Email**: Not triggered on member approval
- **Member Password Change**: No endpoint or UI
- **Customer Support Permissions**: Can't reset member passwords yet
- **Email Sending Triggers**: No integration points

---

## Implementation Architecture

### Email Sending Flow
```
1. Member Approved by Treasurer → approveMember() called
2. MemberService calls new EmailService.sendMemberOnboardingEmail()
3. EmailService renders HTML template with credentials
4. Email sent to member.email via SendGrid
5. Activity logged in UserActivityLog
```

### Password Change Flow
```
Member Portal (Member) or Staff Portal (Customer Support)
    ↓
PUT /api/member/change-password (for members)
PUT /api/members/{memberId}/change-password (for customer support)
    ↓
Verify current password (for members only)
    ↓
Encode new password with BCrypt
    ↓
Save to database
    ↓
Log activity
    ↓
Return success
```

---

## Phase 1: Email Service Implementation

### 1. Create EmailService (New File)

**File**: `backend/src/main/java/com/minet/sacco/service/EmailService.java`

Key Features:
- Send emails asynchronously using `@Async`
- HTML template rendering
- Error handling and logging
- Activity logging for email sends
- Support for multiple email types

```java
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine; // For HTML template processing

    @Autowired
    private UserActivityLogRepository activityLogRepository;

    /**
     * Send member onboarding email with login credentials
     */
    @Async
    public void sendMemberOnboardingEmail(
        Member member, 
        String apkDownloadLink) {
        
        try {
            String username = member.getEmployeeId() != null ? 
                member.getEmployeeId() : member.getMemberNumber();
            
            // Create email content
            Map<String, Object> variables = new HashMap<>();
            variables.put("firstName", member.getFirstName());
            variables.put("lastName", member.getLastName());
            variables.put("username", username);
            variables.put("password", member.getNationalId());
            variables.put("apkLink", apkDownloadLink);
            variables.put("memberNumber", member.getMemberNumber());
            
            String htmlContent = renderTemplate("member-onboarding", variables);
            
            // Send email
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(member.getEmail());
            message.setSubject("Welcome to Minet SACCO - Your Login Credentials");
            message.setText(htmlContent);
            
            mailSender.send(message);
            
            // Log activity
            logEmailSent(member, "MEMBER_ONBOARDING_EMAIL", 
                        "Onboarding email with credentials sent");
            
        } catch (Exception e) {
            System.err.println("ERROR: Failed to send onboarding email: " + e.getMessage());
            logEmailFailed(member, "MEMBER_ONBOARDING_EMAIL", 
                          "Failed to send: " + e.getMessage());
        }
    }

    /**
     * Send password change confirmation
     */
    @Async
    public void sendPasswordChangeConfirmation(User user) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("username", user.getUsername());
            variables.put("timestamp", LocalDateTime.now());
            
            String htmlContent = renderTemplate("password-changed", variables);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("Password Changed - Minet SACCO");
            message.setText(htmlContent);
            
            mailSender.send(message);
            logEmailSent(null, "PASSWORD_CHANGE_EMAIL", 
                        "Password change confirmation email sent");
        } catch (Exception e) {
            System.err.println("ERROR: Failed to send password change email: " + 
                             e.getMessage());
        }
    }

    private String renderTemplate(String templateName, 
                                 Map<String, Object> variables) {
        // Use Thymeleaf or FreeMarker to render HTML template
        // For now, simple String.format approach
        // TODO: Implement proper template rendering
        return buildHtmlEmail(templateName, variables);
    }

    private String buildHtmlEmail(String type, 
                                 Map<String, Object> variables) {
        if ("member-onboarding".equals(type)) {
            return String.format(
                "<html><body>" +
                "<h2>Welcome to Minet SACCO, %s!</h2>" +
                "<p>Your account has been approved and activated.</p>" +
                "<h3>Mobile App Login Credentials:</h3>" +
                "<p><strong>Username:</strong> %s (your employee ID)</p>" +
                "<p><strong>Password:</strong> %s (your national ID)</p>" +
                "<p><strong>Member Number:</strong> %s</p>" +
                "<h3>Next Steps:</h3>" +
                "<ol>" +
                "<li><a href=\"%s\">Download the Minet SACCO Mobile App</a></li>" +
                "<li>Log in with your credentials above</li>" +
                "<li>Change your password on first login</li>" +
                "</ol>" +
                "<p>Questions? Contact Customer Support</p>" +
                "</body></html>",
                variables.get("firstName"),
                variables.get("username"),
                variables.get("password"),
                variables.get("memberNumber"),
                variables.get("apkLink")
            );
        }
        return "";
    }

    private void logEmailSent(Member member, String type, String details) {
        // TODO: Create EmailSentLog entity and log
    }

    private void logEmailFailed(Member member, String type, String reason) {
        // TODO: Create EmailFailureLog and log
    }
}
```

### 2. Configure Email Properties

Update `application.properties`:

```properties
# Email Configuration with SendGrid
spring.mail.host=smtp.sendgrid.net
spring.mail.port=587
spring.mail.username=apikey
spring.mail.password=${SENDGRID_API_KEY:your-sendgrid-api-key-here}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000

# Email Configuration
app.email.from=noreply@minetsacco.com
app.email.from-name=Minet SACCO
app.apk.download-link=https://play.google.com/store/apps/details?id=com.minet.sacco

# Enable async email sending
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=10
spring.task.execution.pool.queue-capacity=100
```

### 3. Trigger Email on Member Approval

**Modify**: `backend/src/main/java/com/minet/sacco/service/MemberService.java`

In the `approveMember()` method, add:

```java
@Autowired
private EmailService emailService;

@Transactional
public Member approveMember(Long memberId, Long approvedByUserId) {
    Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new RuntimeException("Member not found"));
    
    // ... existing approval logic ...
    
    Member savedMember = memberRepository.save(member);
    createDefaultAccounts(savedMember);
    createMemberUserAccount(savedMember);
    
    // SEND ONBOARDING EMAIL
    String apkLink = System.getenv("APK_DOWNLOAD_LINK") != null ? 
        System.getenv("APK_DOWNLOAD_LINK") : 
        "https://play.google.com/store/apps/details?id=com.minet.sacco";
    emailService.sendMemberOnboardingEmail(savedMember, apkLink);
    
    // ... existing audit logging ...
    
    return savedMember;
}
```

---

## Phase 2: Member Password Change Implementation

### 1. Create PasswordChangeDTO (New File)

**File**: `backend/src/main/java/com/minet/sacco/dto/PasswordChangeRequestDTO.java`

```java
public class PasswordChangeRequestDTO {
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;

    // Getters & Setters
}
```

### 2. Add Member Password Change Endpoint

**Modify**: `backend/src/main/java/com/minet/sacco/controller/MemberPortalController.java`

Add endpoint:

```java
@Autowired
private EmailService emailService;

/**
 * Member changes their own password
 */
@PutMapping("/change-password")
public ResponseEntity<ApiResponse<String>> changeMemberPassword(
    @Valid @RequestBody PasswordChangeRequestDTO request) {
    
    try {
        Member member = getCurrentMember();
        User memberUser = userRepository.findByMemberId(member.getId())
            .orElseThrow(() -> new RuntimeException("User account not found"));
        
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), 
                                    memberUser.getPassword())) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Current password is incorrect"));
        }
        
        // Validate new password
        if (request.getNewPassword() == null || 
            request.getNewPassword().length() < 8) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(
                    "Password must be at least 8 characters"));
        }
        
        // Verify password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Passwords do not match"));
        }
        
        // Change password
        memberUser.setPassword(passwordEncoder.encode(
            request.getNewPassword()));
        memberUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(memberUser);
        
        // Log activity
        UserActivityLog log = new UserActivityLog();
        log.setUser(memberUser);
        log.setAction("MEMBER_PASSWORD_CHANGE");
        log.setDetails("Member changed their password");
        log.setCreatedAt(LocalDateTime.now());
        // Save log...
        
        // Send confirmation email
        emailService.sendPasswordChangeConfirmation(memberUser);
        
        return ResponseEntity.ok(
            ApiResponse.success("Password changed successfully"));
    } catch (Exception e) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(e.getMessage()));
    }
}
```

### 3. Add Member Password Change UI

**File**: `minetsacco-main/src/pages/MemberSettings.tsx`

Add password change tab:

```typescript
<Tabs defaultValue="configuration" className="space-y-6">
    <TabsList>
        <TabsTrigger value="configuration">Configuration</TabsTrigger>
        <TabsTrigger value="security">Security</TabsTrigger>
    </TabsList>

    {/* Existing configuration tab */}
    
    <TabsContent value="security" className="space-y-6">
        <Card className="border-none shadow-sm">
            <CardHeader>
                <CardTitle className="flex items-center gap-2">
                    <Lock className="h-5 w-5" />
                    Change Password
                </CardTitle>
            </CardHeader>
            <CardContent>
                <form onSubmit={handlePasswordChange} className="space-y-4">
                    <div className="space-y-2">
                        <Label>Current Password</Label>
                        <Input
                            type="password"
                            value={currentPassword}
                            onChange={e => setCurrentPassword(e.target.value)}
                            placeholder="Enter current password"
                            required
                        />
                    </div>
                    <div className="space-y-2">
                        <Label>New Password</Label>
                        <Input
                            type="password"
                            value={newPassword}
                            onChange={e => setNewPassword(e.target.value)}
                            minLength={8}
                            placeholder="Enter new password (min 8 chars)"
                            required
                        />
                    </div>
                    <div className="space-y-2">
                        <Label>Confirm New Password</Label>
                        <Input
                            type="password"
                            value={confirmPassword}
                            onChange={e => setConfirmPassword(e.target.value)}
                            minLength={8}
                            placeholder="Confirm new password"
                            required
                        />
                    </div>
                    <Button type="submit" disabled={loadingPassword}>
                        {loadingPassword ? "Changing..." : "Change Password"}
                    </Button>
                </form>
            </CardContent>
        </Card>
    </TabsContent>
</Tabs>
```

---

## Phase 3: Customer Support Permissions

### 1. Add Customer Support Password Change Endpoint

**Modify**: `backend/src/main/java/com/minet/sacco/controller/MemberController.java`

Add endpoint:

```java
/**
 * Customer Support can change member passwords
 */
@PutMapping("/{memberId}/change-password")
@PreAuthorize("hasRole('ROLE_CUSTOMER_SUPPORT')")
public ResponseEntity<ApiResponse<String>> changeMemberPasswordAsSupport(
    @PathVariable Long memberId,
    @RequestParam String newPassword,
    @RequestParam String reason,
    Authentication authentication) {
    
    try {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new RuntimeException("Member not found"));
        
        User memberUser = userRepository.findByMemberId(memberId)
            .orElseThrow(() -> new RuntimeException("User account not found"));
        
        // Validate new password
        if (newPassword.length() < 8) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(
                    "Password must be at least 8 characters"));
        }
        
        // Change password
        memberUser.setPassword(passwordEncoder.encode(newPassword));
        memberUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(memberUser);
        
        // Log activity
        String username = authentication.getName();
        User supportStaff = userRepository.findByUsername(username).orElse(null);
        
        UserActivityLog log = new UserActivityLog();
        log.setUser(memberUser);
        log.setAction("CUSTOMER_SUPPORT_PASSWORD_RESET");
        log.setDetails("Password reset by Customer Support: " + reason);
        log.setCreatedAt(LocalDateTime.now());
        // Log who did it...
        
        return ResponseEntity.ok(
            ApiResponse.success("Member password reset successfully"));
    } catch (Exception e) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(e.getMessage()));
    }
}
```

### 2. Admin UI to Reset Member Passwords

**Add to**: `minetsacco-main/src/pages/CustomerSupportPortal.tsx`

Add password reset functionality:

```typescript
const resetMemberPassword = async (memberId: Long, newPassword: string, reason: string) => {
    try {
        const response = await fetch(
            `${API_BASE_URL}/members/${memberId}/change-password?newPassword=${newPassword}&reason=${reason}`,
            {
                method: "PUT",
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json",
                }
            }
        );
        
        if (response.ok) {
            toast({
                title: "Success",
                description: "Member password has been reset"
            });
        }
    } catch (error) {
        toast({
            title: "Error",
            description: "Failed to reset password",
            variant: "destructive"
        });
    }
};
```

---

## Database Changes Needed

### EmailSentLog Table (Optional but Recommended)

```sql
CREATE TABLE email_sent_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT,
    email_address VARCHAR(100),
    email_type VARCHAR(50),
    status ENUM('SENT', 'FAILED', 'BOUNCED'),
    error_message TEXT,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES members(id)
);
```

### Update User Table (Already has what we need)

- `password` field: ✅ Already exists
- `updated_at` field: ✅ Already exists
- `memberId` field: ✅ Already exists

---

## Configuration Requirements

### SendGrid Setup
1. Create SendGrid account at sendgrid.com
2. Get API key
3. Set environment variable: `export SENDGRID_API_KEY=your-key`
4. Or update application.properties with your key

### Application Properties
Add to `application.properties`:
```properties
app.email.from=noreply@minetsacco.com
app.apk.download-link=https://your-apk-link.com
```

---

## Security Considerations

✅ **Implemented**:
- BCrypt password hashing
- Current password verification for member self-service
- Activity logging for all password changes
- Authorization checks (CUSTOMER_SUPPORT role required)
- No password in logs

⚠️ **Recommendations**:
- Use HTTPS for all password endpoints
- Implement rate limiting on password change attempts
- Consider 2-factor authentication for password reset
- Audit password changes regularly
- Send notification emails when password changed

---

## Implementation Timeline

| Phase | Component | Estimated Time |
|-------|-----------|-----------------|
| 1 | EmailService + Templates | 2-3 hours |
| 2 | Email Configuration | 1 hour |
| 3 | Member Password Change (Backend) | 2 hours |
| 4 | Member Password Change UI | 1.5 hours |
| 5 | Customer Support Password Reset | 1.5 hours |
| 6 | Testing & Refinement | 2-3 hours |
| **Total** | | **10-12 hours** |

---

## Testing Checklist

### Email Testing
- [ ] Member approved → email sent successfully
- [ ] Email contains correct credentials
- [ ] Email contains APK download link
- [ ] Email failure handled gracefully
- [ ] Email sending logs created

### Member Password Change
- [ ] Member can change password with correct current password
- [ ] Incorrect current password rejected
- [ ] Password requirements enforced (min 8 chars)
- [ ] Password confirmation validated
- [ ] Confirmation email sent
- [ ] New password works on next login

### Customer Support Password Reset
- [ ] Only CUSTOMER_SUPPORT role can reset passwords
- [ ] Password change logged with reason
- [ ] New temporary password issued correctly
- [ ] Member notified of password reset

---

## Files to Create/Modify

### New Files
- `backend/.../service/EmailService.java`
- `backend/.../dto/PasswordChangeRequestDTO.java`
- Database migration: `V#__Add_email_tracking.sql` (optional)

### Modified Files
- `backend/.../service/MemberService.java` (add email trigger)
- `backend/.../controller/MemberPortalController.java` (add password change)
- `backend/.../controller/MemberController.java` (add customer support reset)
- `minetsacco-main/src/pages/MemberSettings.tsx` (add password UI)
- `minetsacco-main/src/pages/CustomerSupportPortal.tsx` (add reset option)
- `backend/src/main/resources/application.properties` (email config)
- `backend/pom.xml` (add Thymeleaf for templates if not using simple HTML)

---

## Next Steps

1. Confirm SendGrid or alternative email provider
2. Define APK download link (Play Store or custom)
3. Review and approve email template design
4. Confirm password policy requirements
5. Plan database migration if using email tracking
6. Implement in phases as outlined above

Ready to proceed with implementation?
