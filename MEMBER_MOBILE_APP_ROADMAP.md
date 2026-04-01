# Member Mobile App Implementation Roadmap

## Overview

This document outlines the plan for implementing a native mobile app for SACCO members, allowing them to access the system via Android APK while maintaining the same backend infrastructure.

---

## Phase 1: Backend API Enhancement (Weeks 1-2)

### 1.1 Create Member-Specific API Endpoints

**Objective**: Expose member-only endpoints with proper authorization

**Endpoints to Create:**

```
GET    /api/member/profile              # Get member's own profile
GET    /api/member/accounts             # Get member's accounts (savings, shares)
GET    /api/member/transactions         # Get member's transaction history
GET    /api/member/loans                # Get member's loans
GET    /api/member/loans/{id}           # Get specific loan details
POST   /api/member/loans/apply          # Apply for new loan
GET    /api/member/loans/{id}/schedule  # Get repayment schedule
GET    /api/member/statements           # Get account statements
GET    /api/member/notifications        # Get member notifications
POST   /api/member/notifications/{id}/read  # Mark notification as read
GET    /api/member/dashboard            # Get dashboard summary
```

### 1.2 Authorization Implementation

**Add MEMBER role to User entity:**
```java
public enum Role {
    ADMIN, TREASURER, LOAN_OFFICER, TELLER, CUSTOMER_SUPPORT, MEMBER
}
```

**Create @PreAuthorize annotations:**
```java
@RestController
@RequestMapping("/api/member")
public class MemberController {
    
    @GetMapping("/profile")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<MemberDTO> getProfile() {
        // Return authenticated member's profile
    }
}
```

**Ensure member can only access own data:**
```java
private void validateMemberAccess(Long memberId) {
    User currentUser = getCurrentUser();
    if (!currentUser.getMember().getId().equals(memberId)) {
        throw new AccessDeniedException("Cannot access other member's data");
    }
}
```

### 1.3 Create Member DTOs

**New DTOs for mobile app:**
```java
// MemberProfileDTO.java
public class MemberProfileDTO {
    private Long id;
    private String memberNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String employer;
    private String department;
    private Member.Status status;
    private LocalDateTime joinDate;
}

// MemberDashboardDTO.java
public class MemberDashboardDTO {
    private BigDecimal savingsBalance;
    private BigDecimal sharesBalance;
    private BigDecimal totalBalance;
    private Integer activeLoans;
    private BigDecimal totalOutstanding;
    private Integer pendingApplications;
    private List<RecentTransactionDTO> recentTransactions;
}

// LoanApplicationMobileDTO.java
public class LoanApplicationMobileDTO {
    private BigDecimal amount;
    private Integer termMonths;
    private String purpose;
    private Long loanProductId;
    private List<Long> guarantorIds;
}
```

### 1.4 API Documentation

**Update Swagger/OpenAPI documentation:**
- Add member endpoints to API docs
- Document request/response formats
- Include authentication requirements
- Provide example requests/responses

---

## Phase 2: Frontend Web Enhancement (Weeks 2-3)

### 2.1 Create Member Login Flow

**Update Login.tsx:**
```typescript
// Add member login option
<div className="login-options">
    <button onClick={() => setLoginType('admin')}>Admin/Staff Login</button>
    <button onClick={() => setLoginType('member')}>Member Login</button>
</div>

// Show APK download link for member login
{loginType === 'member' && (
    <div className="apk-download">
        <p>Access on mobile:</p>
        <a href="/downloads/minet-sacco-member.apk" download>
            Download APK for Android
        </a>
    </div>
)}
```

### 2.2 Create Member Dashboard Pages

**New pages for members:**

```
/member/dashboard          # Overview of accounts and loans
/member/profile            # Member profile and settings
/member/accounts           # Savings and shares accounts
/member/transactions       # Transaction history
/member/loans              # List of member's loans
/member/loans/apply        # Loan application form
/member/loans/{id}         # Loan details and repayment schedule
/member/statements         # Download account statements
/member/notifications      # Notification center
```

### 2.3 Member Dashboard Component

**MemberDashboard.tsx:**
```typescript
export const MemberDashboard: React.FC = () => {
    const [dashboard, setDashboard] = useState<MemberDashboardDTO | null>(null);
    
    useEffect(() => {
        // Fetch member dashboard data
        api.get('/api/member/dashboard').then(res => setDashboard(res.data));
    }, []);
    
    return (
        <div className="member-dashboard">
            <h1>Welcome, {member.firstName}</h1>
            
            <div className="account-summary">
                <Card title="Savings Balance">
                    KES {dashboard?.savingsBalance}
                </Card>
                <Card title="Shares Balance">
                    KES {dashboard?.sharesBalance}
                </Card>
                <Card title="Active Loans">
                    {dashboard?.activeLoans}
                </Card>
            </div>
            
            <div className="quick-actions">
                <Button onClick={() => navigate('/member/loans/apply')}>
                    Apply for Loan
                </Button>
                <Button onClick={() => navigate('/member/statements')}>
                    Download Statement
                </Button>
            </div>
            
            <RecentTransactions transactions={dashboard?.recentTransactions} />
        </div>
    );
};
```

### 2.4 Loan Application Form

**MemberLoanApplication.tsx:**
```typescript
export const MemberLoanApplication: React.FC = () => {
    const [formData, setFormData] = useState<LoanApplicationMobileDTO>({
        amount: 0,
        termMonths: 12,
        purpose: '',
        loanProductId: null,
        guarantorIds: []
    });
    
    const handleSubmit = async () => {
        try {
            const response = await api.post('/api/member/loans/apply', formData);
            // Show success message
            // Redirect to loan details
        } catch (error) {
            // Show error message
        }
    };
    
    return (
        <form onSubmit={handleSubmit}>
            <input 
                type="number" 
                placeholder="Loan Amount"
                value={formData.amount}
                onChange={(e) => setFormData({...formData, amount: parseFloat(e.target.value)})}
            />
            <select value={formData.termMonths} onChange={(e) => setFormData({...formData, termMonths: parseInt(e.target.value)})}>
                <option value={6}>6 Months</option>
                <option value={12}>12 Months</option>
                <option value={24}>24 Months</option>
            </select>
            <textarea 
                placeholder="Loan Purpose"
                value={formData.purpose}
                onChange={(e) => setFormData({...formData, purpose: e.target.value})}
            />
            <button type="submit">Apply for Loan</button>
        </form>
    );
};
```

---

## Phase 3: Mobile App Development (Weeks 3-6)

### 3.1 Technology Choice

**Recommended: React Native**
- Code sharing with web frontend (TypeScript, React patterns)
- Single codebase for iOS and Android
- Faster development than native
- Can generate APK for Android distribution

**Alternative: Flutter**
- Better performance
- Beautiful UI out of the box
- Separate codebase from web

### 3.2 Project Setup

**Create React Native project:**
```bash
npx react-native init MinetSaccoMember
cd MinetSaccoMember
npm install axios react-navigation react-native-screens
```

**Project structure:**
```
MinetSaccoMember/
├── src/
│   ├── screens/
│   │   ├── LoginScreen.tsx
│   │   ├── DashboardScreen.tsx
│   │   ├── LoansScreen.tsx
│   │   ├── LoanApplicationScreen.tsx
│   │   ├── AccountsScreen.tsx
│   │   ├── TransactionsScreen.tsx
│   │   └── ProfileScreen.tsx
│   ├── components/
│   │   ├── AccountCard.tsx
│   │   ├── LoanCard.tsx
│   │   └── TransactionItem.tsx
│   ├── services/
│   │   ├── api.ts
│   │   └── auth.ts
│   ├── contexts/
│   │   └── AuthContext.tsx
│   └── App.tsx
├── android/
│   └── app/
│       └── build.gradle
└── package.json
```

### 3.3 Core Screens

**LoginScreen.tsx:**
```typescript
export const LoginScreen: React.FC = () => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const { login } = useAuth();
    
    const handleLogin = async () => {
        try {
            await login(username, password);
            // Navigate to dashboard
        } catch (error) {
            Alert.alert('Login Failed', error.message);
        }
    };
    
    return (
        <View style={styles.container}>
            <Text style={styles.title}>Minet SACCO</Text>
            <TextInput 
                placeholder="Username"
                value={username}
                onChangeText={setUsername}
                style={styles.input}
            />
            <TextInput 
                placeholder="Password"
                value={password}
                onChangeText={setPassword}
                secureTextEntry
                style={styles.input}
            />
            <TouchableOpacity onPress={handleLogin} style={styles.button}>
                <Text style={styles.buttonText}>Login</Text>
            </TouchableOpacity>
        </View>
    );
};
```

**DashboardScreen.tsx:**
```typescript
export const DashboardScreen: React.FC = () => {
    const [dashboard, setDashboard] = useState<MemberDashboardDTO | null>(null);
    
    useEffect(() => {
        api.get('/api/member/dashboard').then(res => setDashboard(res.data));
    }, []);
    
    return (
        <ScrollView style={styles.container}>
            <Text style={styles.greeting}>Welcome, {member.firstName}</Text>
            
            <View style={styles.accountsSection}>
                <AccountCard 
                    title="Savings" 
                    amount={dashboard?.savingsBalance}
                />
                <AccountCard 
                    title="Shares" 
                    amount={dashboard?.sharesBalance}
                />
            </View>
            
            <View style={styles.quickActions}>
                <TouchableOpacity style={styles.actionButton}>
                    <Text>Apply for Loan</Text>
                </TouchableOpacity>
                <TouchableOpacity style={styles.actionButton}>
                    <Text>View Loans</Text>
                </TouchableOpacity>
            </View>
            
            <RecentTransactions transactions={dashboard?.recentTransactions} />
        </ScrollView>
    );
};
```

**LoansScreen.tsx:**
```typescript
export const LoansScreen: React.FC = () => {
    const [loans, setLoans] = useState<LoanDTO[]>([]);
    
    useEffect(() => {
        api.get('/api/member/loans').then(res => setLoans(res.data));
    }, []);
    
    return (
        <ScrollView style={styles.container}>
            <Text style={styles.title}>My Loans</Text>
            {loans.map(loan => (
                <LoanCard 
                    key={loan.id}
                    loan={loan}
                    onPress={() => navigateToLoanDetails(loan.id)}
                />
            ))}
        </ScrollView>
    );
};
```

### 3.4 Navigation Setup

**App.tsx:**
```typescript
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';

const Stack = createNativeStackNavigator();
const Tab = createBottomTabNavigator();

export const App: React.FC = () => {
    const { isAuthenticated } = useAuth();
    
    return (
        <NavigationContainer>
            {isAuthenticated ? (
                <Tab.Navigator>
                    <Tab.Screen name="Dashboard" component={DashboardScreen} />
                    <Tab.Screen name="Loans" component={LoansScreen} />
                    <Tab.Screen name="Accounts" component={AccountsScreen} />
                    <Tab.Screen name="Profile" component={ProfileScreen} />
                </Tab.Navigator>
            ) : (
                <Stack.Navigator>
                    <Stack.Screen name="Login" component={LoginScreen} />
                </Stack.Navigator>
            )}
        </NavigationContainer>
    );
};
```

### 3.5 Build APK

**Generate Android APK:**
```bash
cd android
./gradlew assembleRelease
# APK generated at: android/app/build/outputs/apk/release/app-release.apk
```

**Sign APK for distribution:**
```bash
keytool -genkey -v -keystore minet-sacco.keystore -keyalg RSA -keysize 2048 -validity 10000 -alias minet-sacco
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore minet-sacco.keystore app-release.apk minet-sacco
zipalign -v 4 app-release.apk app-release-aligned.apk
```

---

## Phase 4: APK Distribution (Week 6)

### 4.1 Host APK on Web Server

**Create download endpoint:**
```java
@GetMapping("/downloads/minet-sacco-member.apk")
public ResponseEntity<Resource> downloadAPK() {
    File file = new File("public/apk/minet-sacco-member.apk");
    Resource resource = new FileSystemResource(file);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=minet-sacco-member.apk")
        .body(resource);
}
```

### 4.2 Update Login Page

**Add APK download link:**
```typescript
{loginType === 'member' && (
    <div className="apk-section">
        <h3>Access on Mobile</h3>
        <p>Download the Minet SACCO app for Android:</p>
        <a href="/downloads/minet-sacco-member.apk" className="download-btn">
            📱 Download APK
        </a>
        <p className="version-info">Version 1.0.0 | Size: ~45MB</p>
    </div>
)}
```

### 4.3 Installation Instructions

**Display on login page:**
```
How to Install:
1. Download the APK file to your Android phone
2. Open Settings > Security > Enable "Unknown Sources"
3. Open the downloaded APK file
4. Tap "Install"
5. Open the app and login with your credentials
```

---

## Phase 5: Testing & Deployment (Week 7)

### 5.1 Testing Checklist

**Functional Testing:**
- [ ] Login with member credentials
- [ ] View account balances
- [ ] View transaction history
- [ ] Apply for loan
- [ ] View loan details and repayment schedule
- [ ] Download statements
- [ ] View notifications
- [ ] Logout

**Performance Testing:**
- [ ] App startup time < 3 seconds
- [ ] API response time < 2 seconds
- [ ] Offline mode (cache recent data)
- [ ] Battery consumption acceptable

**Security Testing:**
- [ ] JWT token validation
- [ ] Secure storage of credentials
- [ ] HTTPS only communication
- [ ] No sensitive data in logs

### 5.2 Deployment Steps

1. **Backend**: Deploy updated API endpoints
2. **Frontend Web**: Deploy member login page with APK download
3. **Mobile App**: Build and sign APK, upload to server
4. **Documentation**: Create user guide for members

---

## Phase 6: Post-Launch (Ongoing)

### 6.1 Monitoring

- Track app downloads and active users
- Monitor API performance for mobile endpoints
- Collect user feedback and bug reports
- Track crash reports

### 6.2 Future Enhancements

- **Push Notifications**: Notify members of loan approvals, payment reminders
- **Biometric Login**: Fingerprint/Face ID authentication
- **Offline Mode**: Cache data for offline access
- **Payment Integration**: Direct mobile money payments
- **iOS App**: Expand to Apple App Store
- **Advanced Analytics**: Member spending patterns, savings goals

---

## Technology Stack Summary

| Component | Technology | Version |
|-----------|-----------|---------|
| Backend API | Spring Boot | 3.2.0 |
| Mobile Framework | React Native | 0.72+ |
| Language | TypeScript | 5.0+ |
| HTTP Client | Axios | 1.4+ |
| Navigation | React Navigation | 6.0+ |
| State Management | Context API | - |
| Build Tool | Gradle | 8.0+ |
| Target Platform | Android | 8.0+ (API 26+) |

---

## Resource Requirements

**Development Team:**
- 1 Backend Developer (API enhancement)
- 1 Frontend Developer (Web member pages)
- 1 Mobile Developer (React Native)
- 1 QA Engineer (Testing)

**Timeline:** 7 weeks
**Estimated Cost:** $15,000 - $25,000

---

## Success Metrics

- APK downloads: 100+ in first month
- Active mobile users: 50+ members
- App rating: 4.0+ stars
- API response time: < 500ms
- App crash rate: < 0.1%

---

## Conclusion

This roadmap provides a clear path to implementing a member mobile app while leveraging the existing backend infrastructure. The phased approach allows for incremental development and testing, reducing risk and enabling early feedback from members.
