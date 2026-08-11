/**
 * PWA Integration Guide
 * This file shows how to integrate the PWA components into your application
 */

// ============================================
// 1. ADD INSTALL PROMPT TO YOUR APP
// ============================================

// File: src/App.tsx or src/main.tsx
import { InstallPrompt } from '@/components/InstallPrompt';

function App() {
  return (
    <>
      {/* Add this at the root level of your app */}
      <InstallPrompt />
      
      {/* Your existing app structure */}
      <Router>
        {/* Your routes */}
      </Router>
    </>
  );
}

// ============================================
// 2. ADD NOTIFICATION SETTINGS TO PROFILE/SETTINGS PAGE
// ============================================

// File: src/pages/MemberProfile.tsx or src/pages/Settings.tsx
import { NotificationSettings } from '@/components/NotificationSettings';

function MemberProfile() {
  return (
    <div className="container">
      <h1>Profile Settings</h1>
      
      {/* Your existing profile content */}
      
      {/* Add the notification settings component */}
      <NotificationSettings />
      
      {/* Rest of your settings */}
    </div>
  );
}

// ============================================
// 3. INITIALIZE PUSH NOTIFICATIONS AFTER LOGIN
// ============================================

// File: src/contexts/AuthContext.tsx or wherever you handle login
import { initializePush } from '@/services/pushNotificationService';

export const AuthProvider = ({ children }) => {
  const handleLogin = async (credentials) => {
    try {
      // Your existing login logic
      const response = await loginAPI(credentials);
      
      // Store user session
      localStorage.setItem('session', JSON.stringify(response.data));
      
      // Initialize push notifications after successful login
      await initializePush();
      
      // Navigate to dashboard
      navigate('/member/dashboard');
    } catch (error) {
      console.error('Login failed:', error);
    }
  };

  return (
    <AuthContext.Provider value={{ login: handleLogin }}>
      {children}
    </AuthContext.Provider>
  );
};

// ============================================
// 4. OPTIONAL: REQUEST NOTIFICATIONS ON DASHBOARD
// ============================================

// File: src/pages/MemberDashboard.tsx
import { useState, useEffect } from 'react';
import { pushNotificationService } from '@/services/pushNotificationService';
import { Button } from '@/components/ui/button';
import { Bell } from 'lucide-react';

function MemberDashboard() {
  const [showNotificationPrompt, setShowNotificationPrompt] = useState(false);

  useEffect(() => {
    // Check if user hasn't subscribed yet
    const checkNotificationStatus = async () => {
      const isSubscribed = await pushNotificationService.isSubscribed();
      const permission = pushNotificationService.getPermission();
      
      // Show prompt if not subscribed and permission not denied
      if (!isSubscribed && permission !== 'denied') {
        // Wait a bit before showing prompt (better UX)
        setTimeout(() => {
          setShowNotificationPrompt(true);
        }, 3000);
      }
    };

    checkNotificationStatus();
  }, []);

  const handleEnableNotifications = async () => {
    const result = await pushNotificationService.subscribe();
    if (result.success) {
      setShowNotificationPrompt(false);
      // Show success message
    }
  };

  return (
    <div className="dashboard">
      {/* Notification prompt banner */}
      {showNotificationPrompt && (
        <div className="notification-prompt bg-primary text-white p-4 rounded-lg mb-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Bell className="h-6 w-6" />
              <div>
                <h3 className="font-semibold">Stay Updated!</h3>
                <p className="text-sm">Enable notifications to receive important updates</p>
              </div>
            </div>
            <div className="flex gap-2">
              <Button onClick={handleEnableNotifications} variant="secondary" size="sm">
                Enable
              </Button>
              <Button 
                onClick={() => setShowNotificationPrompt(false)} 
                variant="ghost" 
                size="sm"
              >
                Later
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Your existing dashboard content */}
    </div>
  );
}

// ============================================
// 5. EXAMPLE: SENDING PUSH NOTIFICATIONS FROM BACKEND
// ============================================

/*
Backend Integration Example (Java):

// In any service where you want to send notifications
@Autowired
private PushNotificationService pushNotificationService;

// Example 1: Loan Approval Notification
public void approveLoan(Loan loan) {
    // Your loan approval logic...
    loan.setStatus(LoanStatus.APPROVED);
    loanRepository.save(loan);
    
    // Send push notification
    User user = loan.getMember().getUser();
    PushNotificationDTO notification = new PushNotificationDTO.Builder(
        "💰 Loan Approved!",
        String.format("Your loan of KES %,.2f has been approved and will be disbursed soon.", 
                     loan.getPrincipal())
    )
    .type("LOAN_APPROVED")
    .url("/member/dashboard?tab=loans&loanId=" + loan.getId())
    .requireInteraction(true)
    .build();
    
    pushNotificationService.sendNotificationToUser(user, notification);
}

// Example 2: Deposit Confirmation
public void confirmDeposit(DepositRequest deposit) {
    // Your deposit confirmation logic...
    deposit.setStatus(DepositStatus.CONFIRMED);
    depositRepository.save(deposit);
    
    // Send push notification
    User user = deposit.getMember().getUser();
    PushNotificationDTO notification = new PushNotificationDTO.Builder(
        "💵 Deposit Confirmed",
        String.format("Your deposit of KES %,.2f has been confirmed.", deposit.getAmount())
    )
    .type("DEPOSIT_STATUS_CHANGED")
    .url("/member/dashboard?tab=deposits")
    .build();
    
    pushNotificationService.sendNotificationToUser(user, notification);
}

// Example 3: Guarantor Request
public void requestGuarantor(GuarantorRequest request) {
    // Your guarantor request logic...
    
    // Send push notification to guarantor
    User guarantorUser = request.getGuarantor().getUser();
    PushNotificationDTO notification = new PushNotificationDTO.Builder(
        "🤝 Guarantor Request",
        String.format("%s has requested you to guarantee their loan of KES %,.2f", 
                     request.getLoan().getMember().getFullName(),
                     request.getAmount())
    )
    .type("GUARANTOR_REQUEST")
    .url("/member/guarantor-approvals")
    .requireInteraction(true)
    .build();
    
    pushNotificationService.sendNotificationToUser(guarantorUser, notification);
}

// Example 4: Payment Reminder
public void sendPaymentReminder(Loan loan) {
    User user = loan.getMember().getUser();
    PushNotificationDTO notification = new PushNotificationDTO.Builder(
        "⏰ Payment Reminder",
        String.format("Your loan payment of KES %,.2f is due in 3 days.", 
                     loan.getMonthlyInstallment())
    )
    .type("PAYMENT_DUE")
    .url("/member/dashboard?tab=transact")
    .build();
    
    pushNotificationService.sendNotificationToUser(user, notification);
}

// Example 5: Broadcast to All Members
public void sendSystemAnnouncement(String title, String message) {
    PushNotificationDTO notification = new PushNotificationDTO.Builder(
        title,
        message
    )
    .type("SYSTEM")
    .url("/member/dashboard?tab=notifications")
    .build();
    
    pushNotificationService.sendBroadcastNotification(notification);
}
*/

// ============================================
// 6. NOTIFICATION TYPES AND THEIR URLS
// ============================================

/*
Notification Type Mapping (handled by service worker):

LOAN, LOAN_APPROVED, LOAN_REJECTED, LOAN_DISBURSED, LOAN_STATUS_CHANGED
  → /member/dashboard?tab=loans&loanId={id}

DEPOSIT, DEPOSIT_STATUS_CHANGED
  → /member/dashboard?tab=deposits&depositId={id}

GUARANTOR, GUARANTOR_REQUEST
  → /member/guarantor-approvals

PAYMENT_DUE, PAYMENT_OVERDUE
  → /member/dashboard?tab=transact

APPROVAL
  → /member/dashboard?tab=notifications

SYSTEM, SECURITY_ALERT, NEW_DEVICE_LOGIN
  → /member/dashboard?tab=notifications

DEFAULT (any other type)
  → /member/dashboard?tab=notifications
*/

// ============================================
// 7. TESTING CHECKLIST
// ============================================

/*
Before deploying, test the following:

□ Install prompt appears on supported browsers
□ App can be installed on home screen
□ App opens in standalone mode (no browser UI)
□ Notification settings toggle works
□ Browser permission prompt appears
□ Test notification sends successfully
□ Notification appears on device
□ Clicking notification opens correct page
□ Deep linking works (loanId, depositId parameters)
□ Notifications work when app is closed
□ Notifications work when device is locked
□ Unsubscribe removes subscription
□ Offline pages load from cache
□ Service worker updates properly
*/

// ============================================
// 8. USEFUL HELPER FUNCTIONS
// ============================================

import { 
  isPushSupported, 
  hasPushPermission,
  subscribeToPush,
  unsubscribeFromPush,
  isPushSubscribed,
  sendTestPushNotification
} from '@/services/pushNotificationService';

// Check if push is supported
if (isPushSupported()) {
  console.log('Push notifications are supported');
}

// Check if user has granted permission
if (hasPushPermission()) {
  console.log('User has granted notification permission');
}

// Subscribe to push notifications
const handleSubscribe = async () => {
  const result = await subscribeToPush();
  if (result.success) {
    console.log('Successfully subscribed');
  }
};

// Check subscription status
const checkStatus = async () => {
  const isSubscribed = await isPushSubscribed();
  console.log('Is subscribed:', isSubscribed);
};

// Send test notification
const testNotification = async () => {
  const result = await sendTestPushNotification();
  if (result.success) {
    console.log('Test notification sent');
  }
};

export default function IntegrationExample() {
  return (
    <div>
      <h1>This is a reference file only</h1>
      <p>See the code above for integration examples</p>
    </div>
  );
}
