# Aging Analysis Report - Design Document

## Overview
The Aging Analysis Report shows loan delinquency status by categorizing overdue loans into age brackets (30, 60, 90+ days), helping management identify problem loans and take corrective action.

## Report Structure
```
AGING ANALYSIS REPORT
As of: [Date]

LOAN AGING SUMMARY
├── Current (0-30 days)
├── 31-60 Days Overdue
├── 61-90 Days Overdue
└── 90+ Days Overdue

TOTAL LOANS: X
TOTAL OVERDUE AMOUNT: KES X
PERCENTAGE OVERDUE: X%
```

## Data Calculation
- **Current**: Last payment within 30 days or no payment due yet
- **31-60 Days**: Last payment 31-60 days ago
- **61-90 Days**: Last payment 61-90 days ago
- **90+ Days**: Last payment 90+ days ago

## API Endpoint
`GET /api/reports/aging-analysis?asOfDate=YYYY-MM-DD`

## Correctness Properties
1. All loans are categorized into exactly one bracket
2. Total overdue = sum of all overdue brackets
3. Percentage calculations are accurate
4. No loans are double-counted
