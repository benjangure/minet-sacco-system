# Loan Number Generation - Diagnostic Report for Claude

## Problem Statement
When trying to disburse the 4th loan, the system fails with:
```
Duplicate entry 'LN-2026-00003'
constraint [loan_number]
```

## Current Loan Status in Database
- Loan 1: LN-2026-00001 (Status: REPAID)
- Loan 2: LN-2026-00002 (Status: DISBURSED)
- Loan 3: LN-2026-00003 (Status: DISBURSED)
- Loan 4: Should be LN-2026-00004 (Status: APPROVED, trying to disburse)

## Root Cause
The loan number generation logic only counts DISBURSED loans:
```java
// OLD LOGIC (WRONG):
Long count = loanRepository.countByYearAndDisbursed(year);
// Returns: 2 (only counts loans 2 and 3)
// Next number generated: LN-2026-00003 (2 + 1) = DUPLICATE!
```

## Attempted Fix
Changed to count ALL loans with numbers:
```java
// NEW LOGIC (SHOULD BE CORRECT):
Long count = loanRepository.countAllLoansWithNumbersByYear(year);
// Should return: 3 (counts loans 1, 2, and 3)
// Next number should be: LN-2026-00004 (3 + 1) = CORRECT
```

## Files Modified
1. **LoanNumberGenerationService.java**
   - Changed method calls from `countByYearAndDisbursed()` to `countAllLoansWithNumbersByYear()`
   - Updated both `generateLoanNumber()` and `generateLoanNumberForYear()` methods

2. **LoanRepository.java**
   - Added new query method:
   ```java
   @Query("SELECT COUNT(l) FROM Loan l WHERE l.loanNumber IS NOT NULL AND YEAR(l.applicationDate) = :year")
   Long countAllLoansWithNumbersByYear(@Param("year") int year);
   ```

## Current Error After Rebuild
```
Could not resolve attribute 'createdAt' of 'com.minet.sacco.entity.Loan'
```

This error suggests the old query with `createdAt` is still being used somewhere, even after rebuild.

## Loan Entity Fields (Verified)
The Loan entity has these timestamp fields:
- `applicationDate` (LocalDateTime) - when loan was created
- `approvalDate` (LocalDateTime) - when loan was approved
- `disbursementDate` (LocalDateTime) - when loan was disbursed
- NO `createdAt` field exists

## Questions for Claude
1. Why is the error still showing `createdAt` after rebuild? Is there a cache issue?
2. Should we use a different approach entirely (e.g., find max sequence number instead of counting)?
3. Is the `applicationDate` field the correct one to use for year filtering?
4. Should we check if there's a compilation cache in the IDE that needs clearing?

## Alternative Approach to Consider
Instead of counting loans, find the highest sequence number and increment:
```java
@Query("SELECT MAX(CAST(SUBSTRING(l.loanNumber, 11) AS INTEGER)) FROM Loan l WHERE l.loanNumber LIKE CONCAT('LN-', :year, '-%')")
Integer findMaxSequenceForYear(@Param("year") int year);
```

Then in service:
```java
Integer maxSeq = loanRepository.findMaxSequenceForYear(year);
long nextSeq = (maxSeq != null ? maxSeq : 0) + 1;
return String.format("LN-%d-%05d", year, nextSeq);
```

## Database Query to Verify
Run this to see actual loan numbers in database:
```sql
SELECT id, loan_number, status, application_date FROM loans WHERE YEAR(application_date) = 2026 ORDER BY id;
```

Expected result:
```
id | loan_number | status | application_date
1  | LN-2026-00001 | REPAID | 2026-04-27
5  | LN-2026-00002 | DISBURSED | 2026-04-27
7  | LN-2026-00003 | DISBURSED | 2026-04-27
13 | NULL | APPROVED | 2026-04-30
```

## Next Steps for Claude
1. Verify the current state of both files (LoanNumberGenerationService.java and LoanRepository.java)
2. Check if there's a compilation/cache issue preventing the new code from being used
3. Consider the alternative approach (max sequence) if the current approach continues to fail
4. Run the database query to confirm actual loan numbers
5. Test the fix by attempting to disburse loan 13 again
