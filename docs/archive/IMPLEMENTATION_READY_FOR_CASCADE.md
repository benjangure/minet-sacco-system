# Implementation Ready for Cascade - 4 Missing Reports

## Status: READY FOR BUILD ✅

All 4 missing reports have been specified and verified. Critical corrections have been applied.

---

## WHAT TO BUILD

### Priority Order (Build in this sequence)

#### Phase 1 - Week 1
1. **Withdrawal Monitoring Report** (Simplest - transaction table only)
2. **Guarantor Report** (Medium - guarantor + account tables)

#### Phase 2 - Week 2
3. **Loan Eligibility Report** (Medium - multiple tables)
4. **Monthly Contribution Tracking Report** (Complex - bulk tables)

---

## CRITICAL CORRECTIONS APPLIED TO SPEC

### ❌ WRONG - DO NOT USE
```
Frozen Self Guarantee = SUM(loan.outstandingBalance) 
                       WHERE member_id = X AND status = DISBURSED
```

### ✅ CORRECT - USE THIS
```
Frozen Self Guarantee = SUM(guarantor.pledgeAmount) 
                       WHERE guarantor.member_id = X 
                       AND guarantor.self_guarantee = true 
                       AND guarantor.status = ACTIVE
```

**Why:** A member with external guarantors has outstanding balance but zero frozen savings. Only self-guarantee pledges freeze savings.

---

### ❌ WRONG - DO NOT USE
```
Months Contributed = COUNT(DISTINCT MONTH(bulk_transaction_items.date))
```

### ✅ CORRECT - USE THIS
```
Months Contributed = member.consecutiveMonthsCounter
```

**Why:** The COUNT approach fails across year boundaries. Use the field already maintained by bulk upload.

---

### ✅ NEW REQUIREMENT
**Guarantor Report must support TWO view modes:**
1. **Single Member View** - Shows guarantor details for one member (requires Member Number filter)
2. **All Members View** - Shows guarantorship capacity for all members at once (Treasurer's daily view)

---

## REPORT SPECIFICATIONS

### Report 1: WITHDRAWAL MONITORING REPORT
**Complexity:** ⭐ (Simplest)
**Data Source:** `transaction` table only
**Build Time:** 2-3 days

**What It Shows:**
- Every withdrawal transaction
- Member, amount, date/time, method (M-Pesa/manual), processed by, balance after
- Summary totals by method

**Filters:**
- Date Range (required)
- Member Number (optional)
- Withdrawal Method (optional)
- Transaction Status (optional)

**Access:** Admin, Treasurer, Auditor

**Export:** Excel, PDF

---

### Report 2: GUARANTOR REPORT
**Complexity:** ⭐⭐ (Medium)
**Data Source:** `guarantor` table + `account` table + `loan` table
**Build Time:** 3-4 days

**What It Shows (Single Member View):**
- Member's total savings
- Frozen self-guarantee amount (from guarantor table, self_guarantee = true)
- Available savings for guaranteeing others
- Active guarantor pledges for other members
- Available guarantorship capacity
- Loans they are guaranteeing with repayment progress

**What It Shows (All Members View):**
- Summary table for all members showing:
  - Member Number, Name
  - Total Savings
  - Frozen Self Guarantee
  - Available Savings
  - Total Pledges (guaranteeing others)
  - Available Guarantorship Capacity
  - Status (Can Guarantee / Cannot Guarantee)

**Filters:**
- View Mode: Single Member / All Members (required)
- Member Number (required if Single Member)
- Guarantor Status (optional)

**Access:** Admin, Treasurer, Auditor, Loan Officer

**Export:** Excel, PDF

**Critical Calculation:**
```
Frozen Self Guarantee = SUM(guarantor.pledgeAmount) 
                       WHERE guarantor.member_id = X 
                       AND guarantor.self_guarantee = true 
                       AND guarantor.status = ACTIVE
```

---

### Report 3: LOAN ELIGIBILITY REPORT
**Complexity:** ⭐⭐ (Medium)
**Data Source:** `account` table + `loan` table + `member` table
**Build Time:** 3-4 days

**What It Shows:**
- Member's savings balance
- Frozen amount (self-guarantee pledges only)
- Available savings
- Gross eligibility (Available Savings * 3)
- Outstanding loan balance
- Remaining eligibility
- Months contributed (from member.consecutiveMonthsCounter)
- Eligibility Status: ELIGIBLE or NOT ELIGIBLE with reason

**Filters:**
- Member Number (required)

**Access:** Admin, Treasurer, Auditor, Loan Officer, Customer Support

**Export:** Excel, PDF

**Critical Calculation:**
```
Frozen Amount = SUM(guarantor.pledgeAmount) 
               WHERE guarantor.member_id = X 
               AND guarantor.self_guarantee = true 
               AND guarantor.status = ACTIVE

Months Contributed = member.consecutiveMonthsCounter
```

---

### Report 4: MONTHLY CONTRIBUTION TRACKING REPORT
**Complexity:** ⭐⭐⭐ (Most Complex)
**Data Source:** `bulk_batches` table + `bulk_transaction_items` table + `member` table
**Build Time:** 4-5 days

**What It Shows:**
- Per bulk upload batch:
  - Batch date, status, uploaded by
  - Members expected vs in file
  - Missing members count
  - Total savings posted
  - Total loan repayments posted
  - Members who became eligible this month
  - Members at month 5 of 6 (one month away)
  - Detailed member table with contribution amounts and status

**Filters:**
- Date Range (required)
- Batch Status (optional)

**Access:** Admin, Treasurer, Auditor

**Export:** Excel, PDF

---

## IMPLEMENTATION CHECKLIST

### Before Starting
- [ ] Read MISSING_REPORTS_SPECIFICATION.md completely
- [ ] Understand the 4 critical corrections above
- [ ] Verify guarantor table has `self_guarantee` field
- [ ] Verify member table has `consecutiveMonthsCounter` field
- [ ] Verify bulk_batches and bulk_transaction_items tables exist

### For Each Report
- [ ] Create Service class with generate method
- [ ] Create DTO classes for report structure
- [ ] Create Controller endpoints (GET, export/excel, export/pdf)
- [ ] Add @PreAuthorize annotations for access control
- [ ] Add to Reports.tsx frontend
- [ ] Implement filters in frontend
- [ ] Test with sample data
- [ ] Verify calculations match specification
- [ ] Test Excel export
- [ ] Test PDF export
- [ ] Verify date range filtering
- [ ] Verify member filtering
- [ ] End-to-end test

---

## DATA VERIFICATION

Before implementation, verify these fields exist:

### Guarantor Table
```sql
SELECT * FROM guarantor LIMIT 1;
-- Should have: id, loan_id, member_id, pledge_amount, self_guarantee, status, created_at
```

### Member Table
```sql
SELECT * FROM member LIMIT 1;
-- Should have: id, member_number, first_name, last_name, consecutive_months_counter, status
```

### Bulk Tables
```sql
SELECT * FROM bulk_batches LIMIT 1;
SELECT * FROM bulk_transaction_items LIMIT 1;
-- Should have: id, batch_id, member_id, amount, transaction_type, date
```

---

## COMMON MISTAKES TO AVOID

1. ❌ Using `loan.outstandingBalance` as frozen amount
   - ✅ Use `guarantor.pledgeAmount` where `self_guarantee = true`

2. ❌ Counting months from bulk_transaction_items across years
   - ✅ Use `member.consecutiveMonthsCounter` field

3. ❌ Forgetting to filter guarantor pledges by `self_guarantee` flag
   - ✅ Always include `AND guarantor.self_guarantee = true` in WHERE clause

4. ❌ Not including "All Members" view in Guarantor Report
   - ✅ Implement both Single Member and All Members views

5. ❌ Forgetting access control annotations
   - ✅ Add @PreAuthorize for each endpoint

6. ❌ Not testing with actual data
   - ✅ Test with real member/loan/guarantor data before marking complete

---

## REFERENCE DOCUMENTS

- **Full Specification:** MISSING_REPORTS_SPECIFICATION.md
- **Data Capture Overview:** SYSTEM_DATA_CAPTURE_AND_REPORTS.md
- **Existing Reports Pattern:** REPORTS_IMPLEMENTATION_VERIFICATION.md

---

## QUESTIONS FOR CLARIFICATION

If any of these are unclear, ask before starting:

1. Should "All Members" view in Guarantor Report be paginated?
2. Should reports support date range filtering for historical data?
3. Should member filtering be by Member Number or Member ID?
4. Should PDF exports include charts/graphs or just tables?
5. Should reports include audit trail (who generated, when)?

---

## SUCCESS CRITERIA

Report is complete when:
- ✅ All calculations match specification exactly
- ✅ All filters work correctly
- ✅ Excel export is readable and formatted
- ✅ PDF export is readable and formatted
- ✅ Access control is enforced
- ✅ Tested with real data
- ✅ No errors in logs
- ✅ Performance is acceptable (< 5 seconds for most reports)

