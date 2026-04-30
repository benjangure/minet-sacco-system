# Minet SACCO - System Design (Updated April 2026)

Comprehensive overview of the system architecture, design patterns, and technical decisions.

---

## System Architecture Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Client Layer                              │
│  ┌──────────────────┐  ┌──────────────────┐  ┌────────────┐ │
│  │  Web Portal      │  │  Mobile App      │  │  Admin CLI │ │
│  │  (React)         │  │  (Capacitor)     │  │            │ │
│  └────────┬─────────┘  └────────┬─────────┘  └────────┬───┘ │
└───────────┼──────────────────────┼──────────────────────┼────┘
            │                      │                      │
            └──────────────────────┼──────────────────────┘
                                   │ HTTPS/REST
┌──────────────────────────────────▼──────────────────────────┐
│                    API Gateway Layer                         │
│              (Spring Boot REST Endpoints)                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Authentication & Authorization (JWT + RBAC)       │   │
│  │  CORS Configuration                                │   │
│  │  Request/Response Validation                       │   │
│  └─────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
            │
            │ Service Layer
            ▼
┌──────────────────────────────────────────────────────────────┐
│                    Business Logic Layer                       │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Member Service      │  Loan Service                │   │
│  │  Account Service     │  Guarantor Service           │   │
│  │  Eligibility Service │  Notification Service        │   │
│  │  Audit Service       │  Bulk Processing Service     │   │
│  │  Report Service      │  [More Services...]          │   │
│  └──────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
            │
            │ Repository Pattern
            ▼
┌──────────────────────────────────────────────────────────────┐
│                    Data Access Layer                          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  JPA Repositories (Spring Data JPA)                 │   │
│  │  Custom Query Methods                               │   │
│  │  Transaction Management                             │   │
│  └──────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
            │
            │ SQL
            ▼
┌──────────────────────────────────────────────────────────────┐
│                    Database Layer                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  PostgreSQL Database                                │   │
│  │  Flyway Migrations (Version Control)                │   │
│  │  Indexes & Constraints                              │   │
│  └──────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
```

---

## Design Patterns Used

### 1. Layered Architecture

**Purpose**: Separation of concerns and maintainability

**Layers**:
- **Presentation Layer**: REST controllers, request/response handling
- **Business Logic Layer**: Services with core business rules
- **Data Access Layer**: Repositories for database operations
- **Database Layer**: PostgreSQL with Flyway migrations

**Benefits**:
- Easy to test each layer independently
- Changes in one layer don't affect others
- Clear responsibility boundaries
- Scalable and maintainable

---

### 2. Repository Pattern

**Purpose**: Abstract database access and provide a collection-like interface

**Implementation**:
```java
// Repository interface
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByMemberId(Long memberId);
    List<Loan> findByStatus(LoanStatus status);
    // Custom queries
}

// Service uses repository
@Service
public class LoanService {
    @Autowired
    private LoanRepository loanRepository;
    
    public Loan getLoan(Long id) {
        return loanRepository.findById(id).orElseThrow(...);
    }
}
```

**Benefits**:
- Decouples business logic from database implementation
- Easy to mock for testing
- Consistent data access patterns
- Supports multiple database backends

---

### 3. Service Layer Pattern

**Purpose**: Encapsulate business logic and coordinate between repositories

**Implementation**:
```java
@Service
@Transactional
public class LoanService {
    @Autowired
    private LoanRepository loanRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private GuarantorRepository guarantorRepository;
    
    public Loan applyForLoan(LoanApplicationRequest request) {
        // Validate member eligibility
        Member member = memberRepository.findById(request.getMemberId())
            .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        
        // Validate guarantors
        List<Guarantor> guarantors = validateGuarantors(request.getGuarantors());
        
        // Create loan
        Loan loan = new Loan();
        loan.setMember(member);
        loan.setAmount(request.getAmount());
        loan.setGuarantors(guarantors);
        
        return loanRepository.save(loan);
    }
}
```

**Benefits**:
- Centralized business logic
- Transaction management
- Reusable across controllers
- Easy to test

---

### 4. DTO (Data Transfer Object) Pattern

**Purpose**: Separate API contracts from internal entities

**Implementation**:
```java
// Entity (internal)
@Entity
public class Loan {
    @Id
    private Long id;
    @ManyToOne
    private Member member;
    @OneToMany
    private List<Guarantor> guarantors;
    // ... more fields
}

// DTO (API contract)
public class LoanApplicationRequest {
    private Long memberId;
    private Long loanProductId;
    private BigDecimal amount;
    private Integer termMonths;
    private List<GuarantorRequest> guarantors;
    // ... getters/setters
}

// Controller uses DTO
@PostMapping("/apply")
public ResponseEntity<ApiResponse> applyForLoan(@RequestBody LoanApplicationRequest request) {
    Loan loan = loanService.applyForLoan(request);
    return ResponseEntity.ok(new ApiResponse("Loan applied successfully", loan));
}
```

**Benefits**:
- API contracts independent of database schema
- Validation at API boundary
- Flexible response formatting
- Security (hide internal fields)

---

### 5. Dependency Injection

**Purpose**: Loose coupling and testability

**Implementation**:
```java
@Service
public class LoanService {
    @Autowired
    private LoanRepository loanRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private NotificationService notificationService;
    
    // Dependencies injected by Spring
}
```

**Benefits**:
- Easy to mock for testing
- Loose coupling between components
- Centralized dependency management
- Flexible configuration

---

### 6. Transaction Management

**Purpose**: Ensure data consistency and ACID properties

**Implementation**:
```java
@Service
@Transactional
public class LoanService {
    
    @Transactional
    public Loan applyForLoan(LoanApplicationRequest request) {
        // All operations in this method are in one transaction
        // If any operation fails, all changes are rolled back
        
        Loan loan = createLoan(request);
        createGuarantors(loan, request.getGuarantors());
        sendNotifications(loan);
        
        return loan;
    }
}
```

**Benefits**:
- Data consistency
- Automatic rollback on errors
- ACID compliance
- Prevents partial updates

---

### 7. Role-Based Access Control (RBAC)

**Purpose**: Secure access to features based on user roles

**Implementation**:
```java
@RestController
@RequestMapping("/api/loans")
public class LoanController {
    
    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'MEMBER')")
    public ResponseEntity<ApiResponse> applyForLoan(@RequestBody LoanApplicationRequest request) {
        // Only LOAN_OFFICER and MEMBER can apply
        return loanService.applyForLoan(request);
    }
    
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse> approveLoan(@PathVariable Long id) {
        // Only CREDIT_COMMITTEE can approve
        return loanService.approveLoan(id);
    }
}
```

**Benefits**:
- Secure access control
- Declarative security
- Centralized permission management
- Audit trail of who did what

---

### 8. Audit Logging

**Purpose**: Track all changes for compliance and debugging

**Implementation**:
```java
@Service
public class AuditService {
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    public void logAction(String action, String entityType, Long entityId, 
                         String oldValue, String newValue, Long userId) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setUserId(userId);
        log.setTimestamp(LocalDateTime.now());
        
        auditLogRepository.save(log);
    }
}
```

**Benefits**:
- Compliance with regulations
- Debugging and troubleshooting
- Security monitoring
- Historical tracking

---

## Data Flow Patterns

### Loan Application Flow

```
1. Member/Loan Officer submits application
   ↓
2. Controller receives request (LoanApplicationRequest DTO)
   ↓
3. Controller validates input
   ↓
4. Service layer processes:
   - Validate member eligibility
   - Validate guarantors
   - Check loan limits
   - Create loan record
   - Create guarantor records
   - Send notifications
   ↓
5. Repository saves to database
   ↓
6. Transaction commits
   ↓
7. Response returned to client
```

### Guarantor Eligibility Check Flow

```
1. Loan officer enters guarantee amount
   ↓
2. Frontend calls /loans/validate-guarantor-eligibility
   ↓
3. Backend service:
   - Fetch guarantor member record
   - Get guarantor's savings balance
   - Get guarantor's frozen savings
   - Calculate available capacity
   - Check if available >= guarantee amount
   ↓
4. Return eligibility status (✓ or ✗)
   ↓
5. Frontend displays result in real-time
```

### Loan Approval Workflow

```
1. Loan Officer reviews application
   ↓
2. Loan Officer recommends (PENDING_LOAN_OFFICER_REVIEW → PENDING_CREDIT_COMMITTEE)
   ↓
3. Credit Committee reviews
   ↓
4. Credit Committee approves (PENDING_CREDIT_COMMITTEE → APPROVED)
   ↓
5. Treasurer disburses (APPROVED → DISBURSED)
   ↓
6. Funds transferred to member account
   ↓
7. Loan status changes to ACTIVE
   ↓
8. Guarantor savings frozen
```

---

## Database Design

### Entity Relationships

```
Member (1) ──────────────── (N) Account
           ├─ SAVINGS account
           ├─ SHARES account
           └─ CONTRIBUTIONS account

Member (1) ──────────────── (N) Loan
           └─ Loan application

Loan (1) ──────────────── (N) Guarantor
         └─ Up to 3 guarantors per loan

Member (1) ──────────────── (N) Guarantor
           └─ Can guarantee multiple loans

Loan (1) ──────────────── (N) Transaction
         └─ Repayment transactions

Member (1) ──────────────── (N) Transaction
           └─ Deposit/withdrawal transactions

User (1) ──────────────── (N) AuditLog
         └─ Audit trail of actions
```

### Key Tables

**members**
- Primary key: id
- Unique: member_number, employee_id, phone
- Indexes: status, created_at

**accounts**
- Primary key: id
- Foreign key: member_id
- Unique: (member_id, type)
- Indexes: member_id, type

**loans**
- Primary key: id
- Foreign key: member_id, loan_product_id
- Indexes: member_id, status, created_at

**guarantors**
- Primary key: id
- Foreign key: loan_id, member_id
- Indexes: loan_id, member_id, status

**transactions**
- Primary key: id
- Foreign key: account_id
- Indexes: account_id, type, created_at

**audit_log**
- Primary key: id
- Foreign key: user_id
- Indexes: user_id, entity_type, entity_id, timestamp

---

## API Design

### RESTful Principles

**Resource-Oriented URLs**:
```
GET    /api/members              # List all members
GET    /api/members/{id}         # Get specific member
POST   /api/members              # Create member
PUT    /api/members/{id}         # Update member
DELETE /api/members/{id}         # Delete member

GET    /api/loans                # List all loans
GET    /api/loans/{id}           # Get specific loan
POST   /api/loans/apply          # Apply for loan
POST   /api/loans/{id}/approve   # Approve loan
POST   /api/loans/{id}/disburse  # Disburse loan
```

**HTTP Status Codes**:
- 200 OK - Successful GET/PUT
- 201 Created - Successful POST
- 204 No Content - Successful DELETE
- 400 Bad Request - Invalid input
- 401 Unauthorized - Missing/invalid token
- 403 Forbidden - Insufficient permissions
- 404 Not Found - Resource not found
- 500 Internal Server Error - Server error

**Response Format**:
```json
{
  "success": true,
  "message": "Operation successful",
  "data": {
    "id": 1,
    "name": "John Doe",
    ...
  }
}
```

---

## Security Architecture

### Authentication Flow

```
1. User submits credentials (username, password)
   ↓
2. AuthController receives login request
   ↓
3. AuthService validates credentials
   - Fetch user from database
   - Compare password hash (BCrypt)
   ↓
4. If valid:
   - Generate JWT token
   - Return token to client
   ↓
5. Client stores token (localStorage/sessionStorage)
   ↓
6. Client includes token in Authorization header for subsequent requests
   ↓
7. JwtAuthenticationFilter validates token
   - Verify signature
   - Check expiration
   - Extract user info
   ↓
8. If valid, request proceeds; if invalid, return 401 Unauthorized
```

### Authorization Flow

```
1. Request arrives with valid JWT token
   ↓
2. JwtAuthenticationFilter extracts user and roles
   ↓
3. Spring Security checks @PreAuthorize annotations
   ↓
4. If user has required role:
   - Request proceeds to controller
   ↓
5. If user lacks required role:
   - Return 403 Forbidden
```

### Password Security

```
1. User enters password during registration
   ↓
2. Password hashed using BCrypt (salt + hash)
   ↓
3. Hash stored in database (never plain text)
   ↓
4. During login:
   - User enters password
   - BCrypt compares with stored hash
   - If match, authentication succeeds
```

---

## Frontend Architecture

### Component Hierarchy

```
App
├── ProtectedRoute
│   ├── StaffPortal
│   │   ├── AppSidebar
│   │   ├── Dashboard
│   │   ├── Members
│   │   ├── Loans (with Loan Officer feature)
│   │   ├── Savings
│   │   ├── Reports
│   │   └── [more pages...]
│   │
│   └── MemberPortal
│       ├── MemberSidebar
│       ├── MemberDashboard
│       ├── MemberLoanApplication
│       └── [more pages...]
│
└── PublicPages
    ├── Login
    ├── Guide
    └── Index
```

### State Management

**React Context**:
```typescript
// AuthContext provides authentication state
const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(localStorage.getItem('token'));
  
  const login = async (username, password) => {
    const response = await api.post('/auth/login', { username, password });
    setToken(response.data.token);
    setUser(response.data.user);
    localStorage.setItem('token', response.data.token);
  };
  
  return (
    <AuthContext.Provider value={{ user, token, login }}>
      {children}
    </AuthContext.Provider>
  );
};
```

**Local Component State**:
```typescript
const [loans, setLoans] = useState([]);
const [loading, setLoading] = useState(false);
const [error, setError] = useState(null);

useEffect(() => {
  fetchLoans();
}, []);

const fetchLoans = async () => {
  setLoading(true);
  try {
    const response = await api.get('/loans');
    setLoans(response.data);
  } catch (err) {
    setError(err.message);
  } finally {
    setLoading(false);
  }
};
```

---

## Loan Officer Feature Architecture ✨

### Live Eligibility Checking

**Frontend Flow**:
```
1. Loan officer enters guarantee amount
   ↓
2. onChange event triggered
   ↓
3. Frontend calls checkGuarantorEligibility()
   ↓
4. API call: POST /loans/validate-guarantor-eligibility
   ↓
5. Backend validates:
   - Fetch guarantor member
   - Get savings balance
   - Get frozen savings
   - Calculate available = balance - frozen
   - Check available >= guarantee amount
   ↓
6. Return { eligible: true/false, reason: "..." }
   ↓
7. Frontend displays ✓ or ✗ with reason
   ↓
8. Real-time feedback as user types
```

### Total Guarantee Validation

**Frontend Logic**:
```typescript
const validateTotalGuarantee = () => {
  const totalGuaranteed = guarantors.reduce((sum, g) => sum + g.amount, 0);
  
  if (totalGuaranteed > loanAmount) {
    setError(`Total guaranteed (${totalGuaranteed}) cannot exceed loan amount (${loanAmount})`);
    return false;
  }
  
  return true;
};

const handleAddGuarantor = () => {
  if (!validateTotalGuarantee()) {
    return; // Prevent adding
  }
  
  // Add guarantor
};
```

### Guarantor Search

**Frontend Flow**:
```
1. Loan officer enters employee ID (e.g., EMP009)
   ↓
2. Clicks "Search" or presses Enter
   ↓
3. Frontend calls lookupGuarantorByEmployeeId()
   ↓
4. API call: GET /member/member-by-employee-id/{employeeId}
   ↓
5. Backend queries:
   - Find member by employee_id
   - Get member's savings balance
   - Get member's frozen savings
   - Calculate available capacity
   ↓
6. Return member details with capacity
   ↓
7. Frontend displays guarantor info
   ↓
8. Loan officer enters guarantee amount
   ↓
9. Live eligibility check triggered
```

---

## Performance Considerations

### Database Optimization

**Indexes**:
- Primary keys on all tables
- Foreign keys indexed
- Status fields indexed (frequently filtered)
- Created_at indexed (date range queries)
- Member_id indexed (common joins)

**Query Optimization**:
```java
// Efficient: Uses index on member_id
List<Loan> loans = loanRepository.findByMemberId(memberId);

// Inefficient: Full table scan
List<Loan> loans = loanRepository.findAll()
    .stream()
    .filter(l -> l.getMemberId().equals(memberId))
    .collect(toList());
```

### Caching

**Loan Products** (rarely change):
```java
@Cacheable("loanProducts")
public List<LoanProduct> getAllLoanProducts() {
    return loanProductRepository.findAll();
}
```

**Eligibility Rules** (rarely change):
```java
@Cacheable("eligibilityRules")
public EligibilityRules getEligibilityRules() {
    return eligibilityRulesRepository.findLatest();
}
```

### Pagination

```java
// Efficient: Paginated results
Page<Loan> loans = loanRepository.findAll(PageRequest.of(0, 20));

// Inefficient: Load all records
List<Loan> loans = loanRepository.findAll();
```

---

## Error Handling

### Exception Hierarchy

```
Exception
├── RuntimeException
│   ├── ResourceNotFoundException
│   │   └── Member not found
│   ├── ValidationException
│   │   └── Invalid input
│   ├── InsufficientFundsException
│   │   └── Not enough savings
│   └── BusinessRuleException
│       └── Eligibility not met
└── IOException
    └── File upload errors
```

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(404)
            .body(new ApiResponse(false, ex.getMessage(), null));
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse> handleValidation(ValidationException ex) {
        return ResponseEntity.status(400)
            .body(new ApiResponse(false, ex.getMessage(), null));
    }
}
```

---

## Testing Strategy

### Unit Tests

```java
@SpringBootTest
public class LoanServiceTest {
    
    @MockBean
    private LoanRepository loanRepository;
    
    @InjectMocks
    private LoanService loanService;
    
    @Test
    public void testApplyForLoan() {
        // Arrange
        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setMemberId(1L);
        request.setAmount(10000);
        
        // Act
        Loan loan = loanService.applyForLoan(request);
        
        // Assert
        assertNotNull(loan);
        assertEquals(10000, loan.getAmount());
    }
}
```

### Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
public class LoanControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void testApplyForLoanEndpoint() throws Exception {
        mockMvc.perform(post("/api/loans/apply")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"memberId\":1,\"amount\":10000}"))
            .andExpect(status().isOk());
    }
}
```

---

## Deployment Architecture

### Development Environment

```
Developer Machine
├── Backend (Spring Boot on port 8080)
├── Frontend (Vite dev server on port 3000)
├── PostgreSQL (local or Docker)
└── IDE (IntelliJ/VS Code)
```

### Production Environment

```
Production Server
├── Backend (Spring Boot JAR or Docker)
├── Frontend (Static build on CDN/web server)
├── PostgreSQL (Managed database)
├── Nginx (Reverse proxy)
├── SSL/TLS (HTTPS)
└── Monitoring (Logs, metrics)
```

---

## Scalability Considerations

### Horizontal Scaling

**Stateless Backend**:
- Each backend instance is independent
- No session state stored locally
- JWT tokens used for authentication
- Can run multiple instances behind load balancer

**Database Replication**:
- Primary database for writes
- Read replicas for queries
- Automatic failover

### Vertical Scaling

**Caching Layer**:
- Redis for session caching
- Cache frequently accessed data
- Reduce database load

**Database Optimization**:
- Indexes on frequently queried fields
- Partitioning large tables
- Archive old data

---

## Monitoring & Observability

### Logging

```java
@Service
public class LoanService {
    private static final Logger logger = LoggerFactory.getLogger(LoanService.class);
    
    public Loan applyForLoan(LoanApplicationRequest request) {
        logger.info("Loan application received for member: {}", request.getMemberId());
        
        try {
            Loan loan = createLoan(request);
            logger.info("Loan created successfully: {}", loan.getId());
            return loan;
        } catch (Exception e) {
            logger.error("Error creating loan", e);
            throw e;
        }
    }
}
```

### Metrics

- Request count and latency
- Database query performance
- Error rates
- User activity
- System resource usage

### Health Checks

```
GET /actuator/health
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

---

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Spring Boot | Industry standard, large ecosystem, easy deployment |
| PostgreSQL | Reliable, ACID compliant, good for financial data |
| JWT Tokens | Stateless, scalable, secure |
| React | Component-based, large community, good tooling |
| Layered Architecture | Clear separation of concerns, testable, maintainable |
| Repository Pattern | Decouples business logic from data access |
| DTOs | API contracts independent of database schema |
| Flyway Migrations | Version control for database schema |
| Audit Logging | Compliance, debugging, security |
| Role-Based Access | Secure, flexible permission management |

---

## Future Enhancements

1. **Microservices Architecture** - Split into separate services
2. **Event-Driven Architecture** - Kafka for async processing
3. **GraphQL API** - Alternative to REST
4. **Machine Learning** - Loan risk prediction
5. **Advanced Analytics** - Business intelligence
6. **Mobile App Native Features** - Biometric login, offline mode
7. **Integration with External Banks** - Real-time payment processing
8. **Blockchain** - Immutable audit trail

---

## References

- Spring Boot Documentation: https://spring.io/projects/spring-boot
- PostgreSQL Documentation: https://www.postgresql.org/docs/
- React Documentation: https://react.dev
- JWT Introduction: https://jwt.io/introduction
- RESTful API Design: https://restfulapi.net/

