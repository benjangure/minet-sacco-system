# Fund Performance Report - Design Document

## Overview
Shows performance of each fund (savings, shares, benevolent, development, etc.) with balances and activity.

## Report Structure
```
FUND PERFORMANCE REPORT
As of: [Date]

FUND SUMMARY
├── Fund Name
├── Total Balance
├── Member Count
├── Average Balance per Member
├── Contributions (Period)
├── Withdrawals (Period)
└── Net Change

FUND RANKINGS
├── Largest Fund
├── Fastest Growing
└── Most Active
```

## Data Calculation
- Sum account balances by fund type
- Count members per fund
- Calculate contributions and withdrawals from transactions
- Calculate growth rate

## API Endpoint
`GET /api/reports/fund-performance?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`

## Correctness Properties
1. All funds included
2. Balance calculations accurate
3. Member counts correct
4. No double-counting
