# Dual-Mode Loan Migration - Quick Start Guide

## Mode Detection: How It Works

The system **automatically detects** which mode to use based on the **Loan Number column**:

| Loan Number | Mode | Action |
|---|---|---|
| **BLANK** (empty) | CREATE | Creates a brand new loan |
| **POPULATED** (e.g., "L001") | UPDATE | Updates existing loan L001 |

**That's it!** Same Excel file, same upload endpoint, different outcomes.

---

## CREATE Mode: Building a New Loan

### Minimal Data (Fastest)
To create a loan with the least amount of information:

```
Loan Number:        [BLANK]
Employee ID:        EMP041
Loan Product Name:  Emergency Loan 1
Principal Amount:   100000
Term Months:        [BLANK - can add later]
Disbursement Date:  [BLANK - can add later]
Loan Status:        DISBURSED
Outstanding Balance:[BLANK - can add later]
Guarantorship Type: NORMAL
Guarantor 1 ID:     EMP066
Guarantor 1 Pledge: 50000
Guarantor 2 ID:     EMP063
Guarantor 2 Pledge: 50000
```

✅ **This is VALID** and creates a loan ready for refinement later.

### Complete Data (All Fields)
For loans with all information available:

```
Loan Number:        [BLANK]
Employee ID:        EMP041
Loan Product Name:  Emergency Loan 1
Principal Amount:   100000
Term Months:        12
Disbursement Date:  15/01/2024
Loan Status:        DISBURSED
Outstanding Balance:75000
Guarantorship Type: NORMAL
Guarantor 1 ID:     EMP066
Guarantor 1 Pledge: 50000
Guarantor 2 ID:     EMP063
Guarantor 2 Pledge: 50000
Purpose:            Emergency medical expenses
```

✅ **This is VALID** and creates a complete loan record.

### What's Required for CREATE?
- ✅ Employee ID (borrower must exist)
- ✅ Loan Product Name (must exist)
- ✅ Principal Amount (must be > 0)
- ✅ Loan Status (DISBURSED, REPAID, or DEFAULTED)
- ❌ Term, Disbursement Date, Outstanding Balance, Guarantorship Type (optional - add via UPDATE later)

---

## UPDATE Mode: Changing an Existing Loan

### Update Only Guarantors
When you need to replace who's guaranteeing the loan:

```
Loan Number:        L001
Employee ID:        [blank/ignored]
Loan Product Name:  [blank/ignored]
Principal Amount:   [blank/ignored]
Term Months:        [blank]
Disbursement Date:  [blank]
Loan Status:        [blank/ignored]
Outstanding Balance:[blank]
Guarantorship Type: [blank/ignored]
Guarantor 1 ID:     EMP010  ← NEW guarantor
Guarantor 1 Pledge: 100000
```

✅ **This is VALID** and only updates the guarantors for loan L001.

### Update Disbursement Date
When the actual disbursement happens later:

```
Loan Number:        L001
Disbursement Date:  01/02/2025  ← Add date now
[all other fields]: [blank/unchanged]
```

✅ **This is VALID** and sets the disbursement date.

### Update Outstanding Balance
When balance is calculated after some repayments:

```
Loan Number:        L001
Outstanding Balance:80000  ← Updated from 75000
[all other fields]:  [blank/unchanged]
```

✅ **This is VALID** and updates the balance.

### Update Multiple Fields
When adding missing information:

```
Loan Number:        L001
Term Months:        24        ← Add/update term
Disbursement Date:  15/03/2025 ← Add/update date
Outstanding Balance:80000      ← Add/update balance
Guarantor 1 ID:     EMP011    ← Replace guarantors
Guarantor 1 Pledge: 60000
```

✅ **This is VALID** and updates all four aspects.

### What CAN'T Be Changed in UPDATE?
These fields are **read-only** and ignored if provided:
- ❌ Employee ID (identifies the borrower - can't change)
- ❌ Loan Product Name (historical - can't change)
- ❌ Principal Amount (original amount - can't change)
- ❌ Interest Rate % (uses product default always)
- ❌ Loan Status (system determines this)

### What CAN Be Changed in UPDATE?
Only these fields are editable:
- ✅ Disbursement Date (if blank in template, kept as-is)
- ✅ Outstanding Balance (if blank in template, kept as-is)
- ✅ Term Months (if blank in template, kept as-is)
- ✅ Guarantor 1-6 (if any provided, ALL are replaced)

---

## Guarantor Rules

### For CREATE Mode (NORMAL Guarantorship)
- Must provide at least 1 guarantor
- Each guarantor must:
  - Have an Employee ID in system
  - Be ACTIVE status
  - Have available savings to cover pledge (for DISBURSED loans)
- Guarantor pledges must sum to Principal Amount

### For UPDATE Mode (Guarantor Replacement)
- Can leave all guarantor columns blank (no change)
- If you provide ANY guarantor info, ALL old guarantors are replaced
- Each new guarantor must:
  - Have an Employee ID in system
  - Be ACTIVE status
  - Have available savings (old guarantors auto-unfrozen first)

### Example: Changing Guarantors

**Before Update:**
- Loan L001 has Guarantor A and B (total pledge = 100k)
- Both have frozen savings of 50k each

**Your Upload:**
```
Loan Number:        L001
Guarantor 1 ID:     EMP011
Guarantor 1 Pledge: 80000
Guarantor 2 ID:     EMP012
Guarantor 2 Pledge: 20000
```

**After Update:**
- Guarantor A and B are UNFROZEN (50k each released back)
- Guarantor C (EMP011) and D (EMP012) are FROZEN (80k + 20k)
- Audit trail records: "Guarantors changed from A,B → C,D"

---

## Step-by-Step Workflow

### Step 1: Download Template
```
GET /api/loan-migration/template/download
```
This gives you the Excel file with proper columns and examples.

### Step 2: Fill Your Data

**For CREATE rows:**
- Leave Loan Number BLANK
- Fill mandatory fields (Employee ID, Product, Principal, Status, Guarantor Type)
- Fill optional fields as available

**For UPDATE rows:**
- Put existing Loan Number (e.g., "L001")
- Leave other fields blank UNLESS you want to change them
- Fill only the fields you want to update

### Step 3: Upload File
```
POST /api/loan-migration/upload
```

### Step 4: Review Results
Response shows per-row status:
```json
{
  "batchId": 123,
  "totalRecords": 5,
  "successfulRecords": 4,
  "failedRecords": 1,
  "status": "PARTIALLY_COMPLETED",
  "message": "4 loans imported, 1 failed. Check item details for errors."
}
```

### Step 5: Check Error Details
```
GET /api/loan-migration/batch/123/items
```

Shows which rows succeeded and which failed (with error reasons).

---

## Common Scenarios

### Scenario 1: Initial Loan Creation (Minimal)
**Situation:** Treasurer importing 50 legacy loans but doesn't have term info yet

| Loan# | Emp ID | Product | Principal | Status | Guarantor Type | G1 ID | G1 Pledge |
|---|---|---|---|---|---|---|---|
| | EMP001 | Emergency 1 | 50000 | DISBURSED | NORMAL | EMP010 | 30000 |

❌ **FAILS** - Guarantor pledges don't sum to principal (30k ≠ 50k)

| Loan# | Emp ID | Product | Principal | Status | Guarantor Type | G1 ID | G1 Pledge | G2 ID | G2 Pledge |
|---|---|---|---|---|---|---|---|---|---|
| | EMP001 | Emergency 1 | 50000 | DISBURSED | NORMAL | EMP010 | 30000 | EMP011 | 20000 |

✅ **SUCCEEDS** - Loan created, term can be added later via UPDATE

---

### Scenario 2: Add Term & Disbursement Date Later
**Situation:** Treasurer now has term info, wants to update existing loans

| Loan# | Term | Disburse Date |
|---|---|---|
| L001 | 12 | 15/01/2024 |
| L002 | 24 | 03/02/2025 |

✅ **SUCCEEDS** - Both loans updated with term and disbursement date

---

### Scenario 3: Replace Guarantors Mid-Year
**Situation:** One guarantor left the sacco, need to replace them

| Loan# | G1 ID | G1 Pledge | G2 ID | G2 Pledge |
|---|---|---|---|---|
| L001 | EMP020 | 50000 | EMP021 | 50000 |

**Before:** Guarantors were EMP001 (50k) and EMP002 (50k)  
**After:** Guarantors are EMP020 (50k) and EMP021 (50k)

✅ **SUCCEEDS** - Old guarantors unfrozen, new guarantors frozen, audit trail created

---

### Scenario 4: Update Outstanding Balance After Repayment
**Situation:** Member made repayment, balance changed

| Loan# | Outstanding Balance |
|---|---|
| L001 | 40000 |

**Before:** Outstanding was 50000  
**After:** Outstanding is 40000

✅ **SUCCEEDS** - Balance updated, member's repayment recorded

---

## Error Messages & Solutions

| Error | Cause | Solution |
|---|---|---|
| "Loan 'L001' not found" | Trying to UPDATE a non-existent loan | Check loan number spelling; use CREATE mode if new |
| "Member 'EMP999' not found" | Guarantor ID doesn't exist | Verify member ID in system; register if needed |
| "Guarantor 'EMP010' is not ACTIVE" | Guarantor member suspended/deleted | Use active member only; check member status |
| "Member has insufficient available savings" | Not enough unfreezed savings for pledge | Unfreeze other pledges first; use smaller pledge |
| "Guarantor pledges total (40k) must equal principal (50k)" | Sum mismatch in CREATE mode | Adjust guarantor pledge amounts to match principal |
| "Outstanding balance cannot exceed principal" | Balance > amount in UPDATE | Fix balance value; must be ≤ principal |

---

## Tips & Best Practices

✅ **DO:**
- Download fresh template each time
- Leave fields blank for UPDATE if not changing them
- Verify all Member IDs before upload
- Check errors carefully after batch upload
- Keep backup of original Excel

❌ **DON'T:**
- Mix CREATE and UPDATE in same file (actually OK - each row auto-detected)
- Leave mandatory CREATE fields blank (they'll fail)
- Try to change Employee ID in UPDATE (it won't work)
- Use future dates for disbursement
- Duplicate guarantor IDs in same row

---

## Support

**Need Help?**
- Check audit trail for change history: `GET /api/audit-trail?loanId=123`
- Download batch item details for error explanations
- Contact system admin for member registration issues
- Review this guide again for mode confusion

**For Developers:**
- See `DUAL_MODE_LOAN_MIGRATION_IMPLEMENTATION.md` for technical details
- Review `LoanMigrationService.java` source code
- Check `LoanGuarantorUpdateService.java` for guarantor mechanics
