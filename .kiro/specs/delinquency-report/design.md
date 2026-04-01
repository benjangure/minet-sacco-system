# Delinquency Report - Design Document

## Overview
Lists all members with overdue payments, showing payment status and collection priority.

## Report Structure
```
DELINQUENCY REPORT
As of: [Date]

DELINQUENT MEMBERS
├── Member Number
├── Member Name
├── Loan Number
├── Outstanding Balance
├── Days Overdue
├── Last Payment Date
└── Collection Priority

SUMMARY
├── Total Delinquent Members: X
├── Total Delinquent Amount: KES X
└── Average Days Overdue: X days
```

## Data Calculation
- Find all loans with status DISBURSED and outstanding balance > 0
- Calculate days overdue from last payment date
- Assign priority: High (90+), Medium (60-89), Low (30-59)
- Sum total delinquent amount

## API Endpoint
`GET /api/reports/delinquency?asOfDate=YYYY-MM-DD`

## Correctness Properties
1. Only loans with outstanding balance included
2. Days overdue calculated correctly
3. Priority assignment accurate
4. No duplicate members
