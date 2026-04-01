# Bulk Loan Applications Template

## Overview
This template is used to upload multiple loan applications at once. The system will validate all records, calculate interest and repayment amounts, and create loans in bulk.

## File Format
- **Format**: Excel (.xlsx)
- **Sheet Name**: "Loan Applications" (or any name)
- **Encoding**: UTF-8

## Required Columns

| Column | Type | Required | Description | Example |
|--------|------|----------|-------------|---------|
| member_number | Text | Yes | Member's unique ID | M001 |
| loan_product_name | Text | Yes | Exact name of loan product | Emergency Loan |
| amount | Number | Yes | Loan amount in KES | 50000 |
| term_months | Number | **YES - CRITICAL** | **Loan term in months - MUST BE SPECIFIED** | 12 |
| purpose | Text | No | Loan purpose/reason | Medical expenses |
| guarantor_1 | Text | No | First guarantor member number | M002 |
| guarantor_2 | Text | No | Second guarantor member number | M003 |
| guarantor_3 | Text | No | Third guarantor member number | M004 |

## ⚠️ CRITICAL: term_months Column

**The `term_months` column is REQUIRED and CRITICAL for accurate calculations.**

- **DO NOT LEAVE BLANK**: Every loan must have a term specified
- **AFFECTS CALCULATIONS**: Different terms result in different interest and monthly payments
- **MUST BE WITHIN PRODUCT LIMITS**: Term must be between product minimum and maximum
- **EXAMPLE**: A 12-month loan will have different calculations than a 6-month loan, even with the same amount

### Why term_months Matters

The term directly affects:
1. **Total Interest**: Longer terms = more interest
2. **Monthly Payment**: Longer terms = lower monthly payment
3. **Total Repayable**: Longer terms = higher total amount to repay

**Example with same loan amount but different terms:**
```
Loan Amount: 50,000 KES
Interest Rate: 12% per annum

6-month term:
- Total Interest: 50,000 × 0.12 × (6/12) = 3,000 KES
- Monthly Payment: 53,000 ÷ 6 = 8,833.33 KES

12-month term:
- Total Interest: 50,000 × 0.12 × (12/12) = 6,000 KES
- Monthly Payment: 56,000 ÷ 12 = 4,666.67 KES

24-month term:
- Total Interest: 50,000 × 0.12 × (24/12) = 12,000 KES
- Monthly Payment: 62,000 ÷ 24 = 2,583.33 KES
```

## How Calculations Work

The system automatically calculates:

1. **Interest Rate**: Taken from the loan product (e.g., 12% per annum)
2. **Total Interest**: Calculated using formula: `Interest = Principal × Rate × Time`
   - Example: 50,000 × 12% × 1 year = 6,000
3. **Total Repayable**: `Principal + Interest`
   - Example: 50,000 + 6,000 = 56,000
4. **Monthly Repayment**: `Total Repayable ÷ Term Months`
   - Example: 56,000 ÷ 12 = 4,666.67 per month

**IMPORTANT**: The `term_months` column in your Excel file determines the loan term used for calculations. **You MUST specify it for each loan.**

## Available Loan Products

| Product Name | Interest Rate | Min Amount | Max Amount | Min Term | Max Term |
|--------------|---------------|-----------|-----------|----------|----------|
| Emergency Loan | 12% | 5,000 | 100,000 | 1 | 12 |
| Development Loan | 10% | 100,000 | 5,000,000 | 12 | 60 |
| School Fees Loan | 8% | 10,000 | 500,000 | 6 | 24 |
| Asset Financing | 11% | 50,000 | 2,000,000 | 12 | 48 |

## Sample Data

### Example 1: Mixed Loan Products with Different Terms
```
member_number | loan_product_name    | amount  | term_months | purpose           | guarantor_1 | guarantor_2 | guarantor_3
M001          | Emergency Loan       | 50000   | 12          | Medical           | M002        | M003        | M004
M002          | Development Loan     | 500000  | 24          | Business          | M001        | M004        |
M003          | School Fees Loan     | 100000  | 18          | Education         | M005        |             |
M004          | Asset Financing      | 300000  | 36          | Vehicle           | M001        | M002        | M003
M005          | Emergency Loan       | 75000   | 6           | Emergency         |             |             |
```

### Example 2: Same Product, Different Terms and Amounts
```
member_number | loan_product_name    | amount  | term_months | purpose
M001          | Emergency Loan       | 25000   | 6           | 
M002          | Emergency Loan       | 50000   | 12          | 
M003          | Emergency Loan       | 75000   | 12          | 
M004          | Emergency Loan       | 100000  | 12          | 
M005          | Emergency Loan       | 50000   | 9           | 
```

### Example 3: Demonstrating Term Impact on Calculations
```
member_number | loan_product_name    | amount  | term_months | purpose
M001          | Emergency Loan       | 50000   | 6           | Short term
M002          | Emergency Loan       | 50000   | 12          | Medium term
M003          | Emergency Loan       | 50000   | 12          | Medium term (same as M002)
```

**Calculations for Example 3:**
```
M001 (6 months):
- Interest Rate: 12% per annum
- Time in Years: 6 ÷ 12 = 0.5 years
- Total Interest: 50,000 × 0.12 × 0.5 = 3,000 KES
- Total Repayable: 50,000 + 3,000 = 53,000 KES
- Monthly Repayment: 53,000 ÷ 6 = 8,833.33 KES

M002 (12 months):
- Interest Rate: 12% per annum
- Time in Years: 12 ÷ 12 = 1 year
- Total Interest: 50,000 × 0.12 × 1 = 6,000 KES
- Total Repayable: 50,000 + 6,000 = 56,000 KES
- Monthly Repayment: 56,000 ÷ 12 = 4,666.67 KES

M003 (12 months - same as M002):
- Same calculations as M002
```

### Calculation Example for Row 1:
```
Member: M001
Product: Emergency Loan (12% interest rate)
Amount: 50,000 KES
Term: 12 months

Calculations:
- Interest Rate: 12% per annum
- Time in Years: 12 ÷ 12 = 1 year
- Total Interest: 50,000 × 0.12 × 1 = 6,000 KES
- Total Repayable: 50,000 + 6,000 = 56,000 KES
- Monthly Repayment: 56,000 ÷ 12 = 4,666.67 KES

Result:
- Loan Amount: 50,000 KES
- Interest: 6,000 KES
- Total to Repay: 56,000 KES
- Monthly Payment: 4,666.67 KES
```

## Validation Rules

The system will validate:

1. **Member Exists**: Member number must exist in system
2. **Member Active**: Member status must be "ACTIVE"
3. **Product Exists**: Loan product name must match exactly (case-sensitive)
4. **Product Active**: Loan product must be active
5. **Amount Valid**: 
   - Must be >= product minimum
   - Must be <= product maximum
6. **Term Valid**:
   - Must be >= product minimum term
   - Must be <= product maximum term
   - **Must be specified (not blank)**
7. **Guarantors Valid** (if provided):
   - Guarantor member numbers must exist
   - Guarantors must be ACTIVE
   - Guarantors cannot be the applicant
8. **No Duplicates**: Same member cannot appear twice in batch

## Error Handling

If validation fails, the system will:
1. Show which records failed
2. Display error reason for each failed record
3. Show the calculations that were attempted
4. Allow you to fix and re-upload
5. Only process successful records if you choose to proceed

## Upload Steps

1. **Prepare File**: Create Excel file with data
2. **Login as TREASURER**: Only TREASURER can upload batches
3. **Go to Bulk Processing**: Click "Bulk Processing" in sidebar
4. **Upload Batch**:
   - Click "Upload Batch" button
   - Select file
   - Select batch type: "LOAN_APPLICATIONS"
   - Click "Upload"
5. **Review Results**: Check validation results and calculations
6. **Approve Batch**: Click "Approve Batch" to create all loans
7. **Approve Loans**: CREDIT_COMMITTEE approves each loan
8. **Disburse**: TREASURER disburses approved loans

## Tips

- **Column Order**: Doesn't matter, system matches by header name
- **Extra Columns**: System ignores extra columns
- **Blank Rows**: System skips blank rows
- **Case Sensitive**: Product names must match exactly
- **Spaces**: Trim spaces from member numbers and product names
- **Numbers**: Use plain numbers, not formatted (no commas or currency symbols)
- **Term Months**: ALWAYS specify this - it affects interest calculations
- **Guarantors**: Optional, but recommended for larger loans
- **Verify Calculations**: Check the displayed calculations match your expectations

## Common Mistakes

❌ **Wrong**: `member_number = "M-001"` (should be `M001`)
❌ **Wrong**: `amount = "50,000"` (should be `50000`)
❌ **Wrong**: `loan_product_name = "emergency loan"` (should be `Emergency Loan`)
❌ **Wrong**: `term_months = "12 months"` (should be `12`)
❌ **CRITICAL WRONG**: Missing `term_months` column or leaving it blank
❌ **CRITICAL WRONG**: Assuming system will use product minimum term if you don't specify

✓ **Correct**: `member_number = "M001"`
✓ **Correct**: `amount = 50000`
✓ **Correct**: `loan_product_name = "Emergency Loan"`
✓ **Correct**: `term_months = 12`
✓ **CRITICAL CORRECT**: Include `term_months` for EVERY loan with a specific value
✓ **CRITICAL CORRECT**: Verify term is within product limits (e.g., Emergency Loan: 1-12 months)

## Batch Processing

After uploading:
1. **Status: PENDING_APPROVAL** - Awaiting TREASURER approval
2. **Status: APPROVED** - Batch approved, loans created
3. **Status: PROCESSING** - Loans being created
4. **Status: COMPLETED** - All loans created successfully
5. **Status: FAILED** - Some or all loans failed to create

## Loan Creation

When batch is approved:
- Each record creates a new loan
- Loan status: **PENDING** (awaiting CREDIT_COMMITTEE approval)
- Loan number: Auto-generated on disbursement (e.g., LN-2026-001)
- Application date: Today's date
- Created by: TREASURER who approved batch
- Calculations: Interest, total repayable, and monthly payment are calculated and stored based on the `term_months` you specified

## Next Steps

After loans are created:
1. **CREDIT_COMMITTEE**: Review and approve/reject each loan
   - Can see member and guarantor eligibility
   - Can see all calculations
2. **TREASURER**: Disburse approved loans
3. **Member**: Receives funds in their account
4. **Repayment**: Member starts repaying according to schedule

## Support

For issues or questions:
- Check validation error messages
- Verify member numbers exist
- Verify product names match exactly
- Check amount and term are within product limits
- **Ensure `term_months` is specified for EVERY loan** - this is critical for accurate calculations
- Verify term is within product minimum and maximum
- Ensure file is in Excel format (.xlsx)
