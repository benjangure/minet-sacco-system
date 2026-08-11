# 🎯 Complete Feature Specification: Next of Kin Guarantors + Member Exit Management

## 📋 Table of Contents
1. [Design Decisions](#design-decisions)
2. [Complete Feature Flow](#complete-feature-flow)
3. [Database Schema](#database-schema)
4. [Backend Implementation](#backend-implementation)
5. [Frontend Implementation](#frontend-implementation)
6. [User Stories & Workflows](#user-stories--workflows)
7. [Edge Cases & Validations](#edge-cases--validations)
8. [Implementation Phases](#implementation-phases)

---

## 🎨 Design Decisions

### Decision Matrix

| Question | Decision | Rationale |
|----------|----------|-----------|
| **Activation Logic** | B) Approve Simultaneously | Transparent, stronger security, cleaner workflow |
| **Savings Freeze** | A) Immediately Upon Approval | True backup guarantee, prevents capacity issues |
| **UI Preference** | A) Required for All Guarantors | Consistent protection, simpler UX, exit-safe |
| **Member Selection** | A) NOK for multiple primaries OK<br>B) Primary AND NOK on same loan NOT ALLOWED | Flexibility vs conflict of interest |
| **Rejection Handling** | A) Auto-Activate NOK | Seamless replacement, that's the purpose |

---

## 🔄 Complete Feature Flow

### FLOW 1: Loan Application with Next of Kin Guarantors

```
┌─────────────────────────────────────────────────────────────────┐
│                    MEMBER: Apply for Loan                        │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Step 1: Select Loan Product & Amount                           │
│  - Product: Emergency Loan                                       │
│  - Amount: KES 100,000                                          │
│  - Term: 12 months                                              │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Step 2: Add Primary Guarantors (3 required)                    │
│                                                                  │
│  [1] Employee ID: E12345 (Jane Kamau)                          │
│      Guarantee Amount: KES 50,000                               │
│      ✓ Eligible (Available: KES 75,000)                        │
│                                                                  │
│  [2] Employee ID: E67890 (Peter Omondi)                        │
│      Guarantee Amount: KES 30,000                               │
│      ✓ Eligible (Available: KES 45,000)                        │
│                                                                  │
│  [3] Employee ID: E11223 (Mary Wanjiku)                        │
│      Guarantee Amount: KES 20,000                               │
│      ✓ Eligible (Available: KES 30,000)                        │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Step 3: Add Next of Kin Guarantors (REQUIRED - 3 needed)      │
│                                                                  │
│  [NOK 1] For Jane Kamau                                        │
│         Employee ID: E99887 (David Mwangi)                     │
│         Guarantee Amount: KES 50,000 [AUTO-FILLED]             │
│         ✓ Eligible (Available: KES 60,000)                     │
│                                                                  │
│  [NOK 2] For Peter Omondi                                      │
│         Employee ID: E55443 (Grace Akinyi)                     │
│         Guarantee Amount: KES 30,000 [AUTO-FILLED]             │
│         ✓ Eligible (Available: KES 40,000)                     │
│                                                                  │
│  [NOK 3] For Mary Wanjiku                                      │
│         Employee ID: E77665 (James Kipchoge)                   │
│         Guarantee Amount: KES 20,000 [AUTO-FILLED]             │
│         ✓ Eligible (Available: KES 25,000)                     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  System Validations (Before Submission)                         │
│  ✓ NOK count (3) matches Primary count (3)                     │
│  ✓ NOK amounts match Primary amounts                            │
│  ✓ All 6 guarantors have sufficient savings capacity            │
│  ✓ No duplicate members                                         │
│  ✓ No member is both Primary AND NOK on this loan              │
│  ✓ All members are ACTIVE status                               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Step 4: Submit Loan Application                                │
│  Status: PENDING_APPROVAL                                       │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                TREASURER: Review Loan Application                │
│                                                                  │
│  Loan #1234 - John Maina - KES 100,000                         │
│  - Primary Guarantors: 3 (Jane, Peter, Mary)                   │
│  - NOK Guarantors: 3 (David, Grace, James)                     │
│  - All guarantors eligible ✓                                    │
│                                                                  │
│  [Approve Loan] ← Click                                         │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Step 5: Guarantor Approval Requests Sent (6 notifications)     │
│                                                                  │
│  PRIMARY GUARANTORS:                                            │
│  → Jane Kamau: "Approve KES 50,000 guarantee for John Maina"   │
│  → Peter Omondi: "Approve KES 30,000 guarantee for John Maina" │
│  → Mary Wanjiku: "Approve KES 20,000 guarantee for John Maina" │
│                                                                  │
│  NOK GUARANTORS (BACKUP):                                       │
│  → David Mwangi: "Approve KES 50,000 as NOK for Jane"          │
│  → Grace Akinyi: "Approve KES 30,000 as NOK for Peter"         │
│  → James Kipchoge: "Approve KES 20,000 as NOK for Mary"        │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Step 6: All 6 Guarantors Approve                               │
│  ✓ Jane Kamau - APPROVED                                       │
│  ✓ Peter Omondi - APPROVED                                     │
│  ✓ Mary Wanjiku - APPROVED                                     │
│  ✓ David Mwangi (NOK) - APPROVED                               │
│  ✓ Grace Akinyi (NOK) - APPROVED                               │
│  ✓ James Kipchoge (NOK) - APPROVED                             │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Step 7: Loan Disbursed - Savings Frozen                        │
│                                                                  │
│  PRIMARY GUARANTORS (Active):                                   │
│  - Jane: KES 50,000 frozen                                     │
│  - Peter: KES 30,000 frozen                                    │
│  - Mary: KES 20,000 frozen                                     │
│                                                                  │
│  NOK GUARANTORS (Backup - Also Frozen):                         │
│  - David: KES 50,000 frozen                                    │
│  - Grace: KES 30,000 frozen                                    │
│  - James: KES 20,000 frozen                                    │
│                                                                  │
│  Status: ACTIVE                                                 │
└─────────────────────────────────────────────────────────────────┘

**Key Points:**
- ✅ Both Primary and NOK savings frozen immediately
- ✅ NOK guarantors are backup - activated if Primary exits/defaults
- ✅ Total coverage = 2x (Primary + NOK both committed)
```

---

### FLOW 2: Member Exit Triggers Auto-Replacement

```
┌─────────────────────────────────────────────────────────────────┐
│              TREASURER: Opens Members Management                 │
│                                                                  │
│  [Search: Jane Kamau]                                           │
│  Result: Jane Kamau (E12345) - Status: ACTIVE                  │
│  Actions: [View] [Edit] [Mark as Exited] ← Click               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                   Exit Member Confirmation Dialog                │
│                                                                  │
│  ⚠️ Mark Jane Kamau (E12345) as EXITED?                        │
│                                                                  │
│  Exit Reason: [Dropdown]                                        │
│    - RESIGNED ✓ (Selected)                                      │
│    - RETIRED                                                    │
│    - TERMINATED                                                 │
│    - DECEASED                                                   │
│    - OTHER                                                      │
│                                                                  │
│  Exit Date: [2026-08-04] (Today)                                │
│                                                                  │
│  ⚠️ IMPACT ANALYSIS:                                            │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│  Jane is currently a PRIMARY GUARANTOR for:                     │
│                                                                  │
│  📋 Loan #1234 (John Maina) - KES 50,000                       │
│     → NOK Available: David Mwangi ✓                            │
│     → Action: Auto-switch to NOK                                │
│                                                                  │
│  📋 Loan #1567 (Susan Njeri) - KES 40,000                      │
│     → NOK Available: Alice Mutua ✓                             │
│     → Action: Auto-switch to NOK                                │
│                                                                  │
│  Total Guarantees: 2 loans, KES 90,000                         │
│  All loans have NOK backup ✓                                    │
│                                                                  │
│  [Cancel]  [Confirm Exit & Replace Guarantors]                 │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│              System Processing (Automated Steps)                 │
│                                                                  │
│  [1/5] Mark Jane Kamau as EXITED ✓                             │
│        - Status: ACTIVE → EXITED                                │
│        - Exit Date: 2026-08-04                                  │
│        - Exit Reason: RESIGNED                                  │
│                                                                  │
│  [2/5] Find all loans where Jane is PRIMARY guarantor ✓         │
│        - Loan #1234: KES 50,000                                 │
│        - Loan #1567: KES 40,000                                 │
│                                                                  │
│  [3/5] For each loan, activate NOK guarantor ✓                  │
│        Loan #1234:                                              │
│        - OLD: Jane Kamau (PRIMARY, ACTIVE)                      │
│        - NEW: David Mwangi (NOK → PRIMARY, ACTIVE)             │
│        - Jane's status: ACTIVE → REPLACED_DUE_TO_EXIT           │
│                                                                  │
│        Loan #1567:                                              │
│        - OLD: Jane Kamau (PRIMARY, ACTIVE)                      │
│        - NEW: Alice Mutua (NOK → PRIMARY, ACTIVE)              │
│        - Jane's status: ACTIVE → REPLACED_DUE_TO_EXIT           │
│                                                                  │
│  [4/5] Unfreeze Jane's savings ✓                                │
│        - KES 90,000 released back to Jane                       │
│                                                                  │
│  [5/5] Send notifications ✓                                     │
│        → Jane: "You've been marked as EXITED"                   │
│        → John Maina: "Guarantor changed: Jane → David"          │
│        → Susan Njeri: "Guarantor changed: Jane → Alice"         │
│        → David: "You're now PRIMARY guarantor for Loan #1234"   │
│        → Alice: "You're now PRIMARY guarantor for Loan #1567"   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    Result: Exit Complete                         │
│                                                                  │
│  ✅ Jane Kamau marked as EXITED                                 │
│  ✅ 2 loans updated with NOK guarantors                         │
│  ✅ KES 90,000 savings unfrozen                                 │
│  ✅ 5 notifications sent                                        │
│  ✅ Audit trail recorded                                        │
└─────────────────────────────────────────────────────────────────┘
```

---

### FLOW 3: Rejection Handling (Primary Rejects → NOK Auto-Activated)

```
┌─────────────────────────────────────────────────────────────────┐
│         SCENARIO: Primary Guarantor Rejects Request             │
└─────────────────────────────────────────────────────────────────┘
