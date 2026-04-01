# Member Contribution Report - Design Document

## Overview
Tracks member savings, shares, and fund contributions over a period, showing contribution patterns and member engagement.

## Report Structure
```
MEMBER CONTRIBUTION REPORT
Period: [Start Date] to [End Date]

CONTRIBUTION SUMMARY
├── Total Savings Contributions: KES X
├── Total Shares Contributions: KES X
├── Total Fund Contributions: KES X
└── Total Contributions: KES X

MEMBER BREAKDOWN
├── Active Contributors: X members
├── Inactive Members: X members
└── New Members: X members

TOP CONTRIBUTORS
├── Member 1: KES X
├── Member 2: KES X
└── Member 3: KES X
```

## Data Calculation
- Sum deposits by account type (savings, shares, funds)
- Count active vs inactive members
- Identify top contributors
- Calculate average contribution per member

## API Endpoint
`GET /api/reports/member-contributions?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`

## Correctness Properties
1. All contributions counted exactly once
2. Total = sum of all account types
3. Member counts accurate
4. No negative values
