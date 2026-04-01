# Bulk Processing Excel Templates

## Monthly Contributions Template

Use this template for uploading monthly salary deductions (savings, shares, loan repayments, and all fund contributions).

### Excel Format (Updated for Kenyan SACCO Standard)

| Employee ID | Savings | Shares | Loan Repayment | Loan Number | Benevolent | Development | School Fees | Holiday | Emergency |
|-------------|---------|--------|----------------|-------------|------------|-------------|-------------|---------|-----------|
| EMP001      | 5000    | 2000   | 3000           | LN-2024-001 | 200        | 100         | 1000        | 1500    | 500       |
| EMP002      | 8000    | 3000   | 5000           | LN-2024-002 | 200        | 100         | 1000        | 1500    | 500       |
| EMP003      | 6000    | 2500   | 0              |             | 200        | 100         | 1000        | 1500    | 500       |

### Column Descriptions

1. **Employee ID** (Required)
   - Format: EMP001, EMP002, EMP003, etc.
   - Must match existing member's employee ID in system
   - Example: EMP001

2. **Savings Amount** (Optional)
   - Withdrawable deposits
   - Numeric value (no currency symbols)
   - Minimum: 0
   - Leave blank or 0 if no savings contribution
   - Example: 5000

3. **Shares Amount** (Optional)
   - Non-withdrawable share capital
   - Numeric value (no currency symbols)
   - Minimum: 0
   - Leave blank or 0 if no shares contribution
   - Example: 2000

4. **Loan Repayment Amount** (Optional)
   - Numeric value (no currency symbols)
   - Minimum: 0
   - Leave blank or 0 if no loan repayment
   - Example: 3000

5. **Loan Number** (Required if Loan Repayment Amount > 0)
   - Format: LN-YYYY-XXX
   - Must match existing active loan
   - Leave blank if no loan repayment
   - Example: LN-2024-001

6. **Benevolent Fund Amount** (Optional)
   - Welfare fund (funerals, medical emergencies)
   - Numeric value (no currency symbols)
   - Minimum: 0
   - Example: 200

7. **Development Fund Amount** (Optional)
   - SACCO development projects
   - Numeric value (no currency symbols)
   - Minimum: 0
   - Example: 100

8. **School Fees Amount** (Optional)
   - Education fund for children
   - Numeric value (no currency symbols)
   - Minimum: 0
   - Example: 1000

9. **Holiday Fund Amount** (Optional)
   - Christmas/holiday savings (withdrawn in December)
   - Numeric value (no currency symbols)
   - Minimum: 0
   - Example: 1500

10. **Emergency Fund Amount** (Optional)
    - Personal emergency savings
    - Numeric value (no currency symbols)
    - Minimum: 0
    - Example: 500

### Validation Rules

- At least one amount (Savings, Shares, Loan Repayment, or any Fund) must be greater than 0
- Employee ID must exist in the system (matched to member's employee_id field)
- If Loan Repayment Amount > 0, Loan Number is required
- Loan Number must match an active loan for the member
- All amounts must be positive numbers
- Maximum amount per contribution: KES 1,000,000
- Maximum file size: 5MB
- Supported formats: .xlsx, .xls, .csv

### Example Scenarios

#### Scenario 1: Full Monthly Deduction (All Contributions)
```
Employee ID | Savings | Shares | Loan Repayment | Loan Number | Benevolent | Development | School Fees | Holiday | Emergency
EMP001      | 5000   | 2000  | 3000          | LN-2024-001 | 200       | 100        | 1000       | 1500   | 500
Total: KES 13,300
```

#### Scenario 2: Mandatory Only (Savings, Shares, Loan)
```
Employee ID | Savings | Shares | Loan Repayment | Loan Number | Benevolent | Development | School Fees | Holiday | Emergency
EMP001      | 5000   | 2000  | 3000          | LN-2024-001 | 0         | 0          | 0          | 0      | 0
Total: KES 10,000
```

#### Scenario 3: No Loan Repayment
```
Employee ID | Savings | Shares | Loan Repayment | Loan Number | Benevolent | Development | School Fees | Holiday | Emergency
EMP001      | 5000   | 2000  | 0             |             | 200       | 100        | 1000       | 1500   | 500
Total: KES 10,300
```

#### Scenario 4: Loan Repayment Only
```
Employee ID | Savings | Shares | Loan Repayment | Loan Number | Benevolent | Development | School Fees | Holiday | Emergency
EMP001      | 0      | 0     | 5000          | LN-2024-001 | 0         | 0          | 0          | 0      | 0
Total: KES 5,000
```

### Typical Minet Employee Monthly Deduction

```
Employee: John Doe (EMP001)
Gross Salary: KES 100,000

SACCO Deductions:
1. Shares:          KES 2,000  (Non-withdrawable)
2. Savings:         KES 5,000  (Withdrawable)
3. School Fees:     KES 1,000  (Withdrawable in Jan/Apr/Sep)
4. Holiday Fund:    KES 1,500  (Withdrawable in December)
5. Benevolent:      KES 200    (Welfare fund)
6. Development:     KES 100    (SACCO projects)
7. Emergency:       KES 500    (Personal emergency)
8. Loan Repayment:  KES 3,000  (Active loan)

Total SACCO:        KES 13,300
Take-home:          KES 86,700
```

## Upload Process

1. **Prepare Excel File**
   - Use the template format above
   - Use Employee ID (EMP001, EMP002, etc.) in first column
   - Ensure all data is accurate
   - Save as .xlsx or .csv

2. **Upload File**
   - Navigate to Bulk Processing page
   - Select "Monthly Contributions" as batch type
   - Choose your Excel file
   - Click "Upload"

3. **Review Validation Results**
   - System validates all entries
   - Shows summary: Total records, Total amount
   - Displays any validation errors
   - Status: PENDING or VALIDATION_FAILED

4. **Maker-Checker Approval**
   - Uploader (Maker) cannot approve own batch
   - Another Treasurer/Admin (Checker) reviews
   - Checker can:
     - Approve: Starts processing
     - Reject: Provide reason

5. **Processing**
   - System processes all transactions
   - Updates member accounts
   - Records loan repayments
   - Generates audit trail

6. **View Results**
   - Check batch status: COMPLETED or PARTIALLY_COMPLETED
   - Download detailed report
   - Review any failed transactions

## Security Features

- Role-based access (Treasurer/Admin only)
- Maker-Checker approval workflow
- Comprehensive audit trail
- Transaction rollback capability
- File validation and virus scanning

## Tips for Success

1. **Test with Small Batch First**
   - Upload 5-10 records initially
   - Verify results before full upload

2. **Double-Check Employee IDs**
   - Most common error is incorrect employee IDs
   - Verify against member list
   - Format: EMP001, EMP002, etc.

3. **Verify Loan Numbers**
   - Ensure loan is active and belongs to member
   - Check outstanding balance

4. **Use Consistent Formatting**
   - No currency symbols (KES, $, etc.)
   - No commas in numbers
   - Use plain numbers only

5. **Keep Backup**
   - Save original Excel file
   - Keep for reconciliation

## Troubleshooting

### Common Errors

1. **"Member not found"**
   - Check employee ID format (should be EMP001, EMP002, etc.)
   - Verify member exists in system with that employee ID

2. **"Loan not found"**
   - Check loan number format
   - Verify loan is active

3. **"File too large"**
   - Split into multiple batches
   - Maximum 5MB per file

4. **"Invalid file format"**
   - Use .xlsx, .xls, or .csv only
   - No macros or formulas

5. **"Cannot approve own batch"**
   - Different user must approve
   - Maker-Checker rule enforced
