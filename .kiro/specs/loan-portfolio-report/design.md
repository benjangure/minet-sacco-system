# Loan Portfolio Report - Design Document

## Overview
Shows the complete loan portfolio breakdown by status (active, disbursed, repaid, defaulted, pending), helping management understand loan distribution and portfolio health.

## Report Structure
```
LOAN PORTFOLIO REPORT
As of: [Date]

PORTFOLIO SUMMARY
├── Pending Approval: X loans, KES X
├── Active/Disbursed: X loans, KES X
├── Repaid: X loans, KES X
├── Defaulted: X loans, KES X
└── Conditional Approval: X loans, KES X

TOTAL LOANS: X
TOTAL PORTFOLIO VALUE: KES X
PORTFOLIO HEALTH: X%
```

## Data Calculation
- Count and sum loans by status
- Calculate portfolio health = (Active + Repaid) / Total * 100
- Show breakdown by loan product

## API Endpoint
`GET /api/reports/loan-portfolio?asOfDate=YYYY-MM-DD`

## Correctness Properties
1. All loans counted exactly once
2. Total portfolio = sum of all statuses
3. Portfolio health percentage is accurate
4. No negative values
