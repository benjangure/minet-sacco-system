# 🎯 Complete Feature Flow: Next of Kin Guarantors + Member Exit Management

## Executive Summary

This document outlines the complete implementation of two interconnected features:
1. **Next of Kin (NOK) Guarantor System**: Backup guarantors for loan/top-up applications
2. **Member Exit Management**: Automatic guarantor replacement when members exit the SACCO

---

## 📋 Table of Contents

1. [Business Requirements](#business-requirements)
2. [Feature Flow Diagrams](#feature-flow-diagrams)
3. [Database Schema Changes](#database-schema-changes)
4. [Backend Implementation](#backend-implementation)
5. [Frontend Implementation](#frontend-implementation)
6. [User Journeys](#user-journeys)
7. [Edge Cases & Validations](#edge-cases--validations)
8. [Testing Checklist](#testing-checklist)

---

## 🎯 Business Requirements

### Feature 1: Next of Kin Guarantor System

**Problem**: What happens if a primary guarantor is unavailable, exits, or cannot fulfill their obligation?

**Solution**: Allow loan applicants to select backup (next of kin) guarantors who can step in.

**Rules**:
- ✅ Number of NOK guarantors MUST equal number of primary guarantors
- ✅ Each NOK covers the SAME amount as their corresponding primary guarantor
- ✅ NOK must be active SACCO members with sufficient savings capacity
- ✅ NOK cannot be the same person as primary guarantor
- ✅ Approval workflow: Both primary and NOK approve simultaneously

### Feature 2: Member Exit Management

**Problem**: When a member exits the SACCO and they are a guarantor, their guarantees need to be handled.

**Solution**: Treasurer can mark members as EXITED, triggering automatic guarantor replacement.

**Rules**:
- ✅ Only TREASURER or ADMIN can mark members as EXITED
- ✅ Exit reasons: RESIGNED, RETIRED, TERMINATED, DECEASED, OTHER
- ✅ When marked as EXITED:
  - If member is a PRIMARY guarantor → NOK automatically becomes PRIMARY
  - If member is a NOK guarantor → Warning shown, but no auto-replacement
  - If member has NO NOK → Borrower notified to find replacement guarantor
- ✅ Exit date and reason recorded for audit trail
- ✅ Member's savings frozen/processed per SACCO exit policy
- ✅ Notifications sent to:
  - Exited member
  - Borrowers where member was guarantor
  - Replacement NOK guarantors (if applicable)

---

## 📊 Feature Flow Diagrams

### Flow 1: Loan Application with NOK Guarantors

```
[MEMBER] Apply for Loan
    ↓
Select Loan Product & Amount
    ↓
Add Primary Guarantors (3 members)
    ├─ Guarantor 1: KES 50,000
    ├─ Guarantor 2: KES 30,000
    └─ Guarantor 3: KES 20,000
    ↓
Add NOK Guarantors (3 members - REQUIRED)
    ├─ NOK 1 for G1: KES 50,000 (auto-filled)
    ├─ NOK 2 for G2: KES 30,000 (auto-filled)
    └─ NOK 3 for G3: KES 20,000 (auto-filled)
    ↓
System Validates:
    ✓ NOK count matches Primary count
    ✓ NOK amounts match Primary amounts
    ✓ All NOK members have sufficient savings
    ✓ No duplicate members (Primary ≠ NOK)
    ↓
Submit Application
    ↓
[TREASURER] Reviews & Approves Loan
    ↓
Approval Requests Sent to:
    ├─ Primary Guarantor 1
    ├─ Primary Guarantor 2
    ├─ Primary Guarantor 3
    ├─ NOK Guarantor 1
    ├─ NOK Guarantor 2
    └─ NOK Guarantor 3
    ↓
All 6 Guarantors Approve
    ↓
Loan Disbursed
    ├─ Primary guarantors' savings frozen
    └─ NOK guarantors' savings frozen (as backup)
```

### Flow 2: Member Exit Triggers Guarantor Replacement

```
[TREASURER] Opens Members Page
    ↓
Selects Member "John Doe"
    ↓
Click "Mark as Exited"
    ↓
Dialog Opens:
    ├─ Exit Reason (dropdown)
    ├─ Exit Date (date picker)
    └─ Confirmation warning
    ↓
System Checks:
    ├─ Is member a PRIMARY guarantor? → YES
    │   └─ Loans: #123, #456, #789
    └─ Does member have NOK guarantors? → YES
    ↓
