# Profit & Loss Report - Requirements Document

## Business Requirements

### Overview
The Profit & Loss (Income Statement) Report is a critical financial statement that shows the SACCO's financial performance over a specified period. It enables management to understand revenue sources, control expenses, and measure profitability.

### User Stories

#### US1: Generate P&L Report for Custom Period
**As a** SACCO Manager/Auditor  
**I want to** generate a Profit & Loss report for any custom date range  
**So that** I can analyze financial performance for specific periods (monthly, quarterly, annually)

**Acceptance Criteria:**
- User can select start and end dates
- Report shows all revenue and expense categories
- Report calculates net profit/loss accurately
- Report displays profit margin percentage
- Report can be generated for any date range

#### US2: View Revenue Breakdown
**As a** SACCO Manager  
**I want to** see detailed revenue breakdown by source  
**So that** I can understand which revenue streams are most important

**Acceptance Criteria:**
- Interest income from loans is clearly shown
- Interest income from savings/shares is shown separately
- Fees and charges are itemized
- Other income sources are listed
- Total revenue is calculated correctly

#### US3: View Expense Breakdown
**As a** SACCO Manager  
**I want to** see detailed expense breakdown by category  
**So that** I can identify cost control opportunities

**Acceptance Criteria:**
- Operating expenses are itemized (salaries, rent, utilities, etc.)
- Loan loss provisions are shown separately
- Other expenses are listed
- Total expenses are calculated correctly
- Expenses are accurate and complete

#### US4: Export P&L Report
**As a** SACCO Manager  
**I want to** export the P&L report to Excel and PDF formats  
**So that** I can share it with stakeholders and archive it

**Acceptance Criteria:**
- Report can be exported to Excel format
- Report can be exported to PDF format
- Exported file includes all data and formatting
- File naming includes report type and date range
- Export is quick and reliable

#### US5: View Summary Metrics
**As a** SACCO Manager  
**I want to** see key summary metrics at a glance  
**So that** I can quickly understand financial performance

**Acceptance Criteria:**
- Total revenue is prominently displayed
- Total expenses are prominently displayed
- Net profit/loss is prominently displayed
- Profit margin percentage is shown
- Summary cards are easy to read

### Functional Requirements

#### FR1: Data Accuracy
- All interest income must be calculated from actual loan data
- All expenses must be from recorded transactions
- Loan loss provisions must reflect actual defaulted loans
- All calculations must be mathematically correct
- Period filtering must be accurate and consistent

#### FR2: Report Components
- Revenue section with subsections for interest, fees, and other income
- Expense section with subsections for operating costs, provisions, and other expenses
- Net profit/loss calculation
- Profit margin percentage calculation
- Summary metrics display

#### FR3: Date Range Flexibility
- Support any custom date range (daily, weekly, monthly, quarterly, annual)
- Include both start and end dates in the period
- Handle year-end and fiscal year reporting

#### FR4: Export Functionality
- Export to Excel with proper formatting
- Export to PDF with professional layout
- Include report title, period, and all data
- Filename includes report type and date range

#### FR5: Access Control
- Only ADMIN and AUDITOR roles can access P&L reports
- Audit log all report generation activities

### Non-Functional Requirements

#### NFR1: Performance
- Report generation should complete within 5 seconds for typical data volumes
- Support date ranges up to 5 years
- Optimize database queries for performance

#### NFR2: Accuracy
- All calculations must be accurate to 2 decimal places
- No rounding errors in totals
- Consistent calculation methodology

#### NFR3: Usability
- Clear, intuitive interface for date selection
- Easy-to-read report layout
- Responsive design for different screen sizes

#### NFR4: Reliability
- Report generation must be reliable and consistent
- Handle edge cases (no data in period, negative values, etc.)
- Proper error handling and user feedback

### Business Rules

#### BR1: Interest Income Recognition
- Interest income is recognized when loans are disbursed
- Only loans with status DISBURSED or REPAID are included
- Interest is calculated based on loan product configuration

#### BR2: Loan Loss Provisions
- Provisions are created for all DEFAULTED loans
- Provision amount equals the outstanding balance
- Provisions are recognized in the period the loan was created

#### BR3: Operating Expenses
- All expenses must be properly categorized
- Expenses are recognized when transactions are recorded
- Expenses include salaries, rent, utilities, and other operational costs

#### BR4: Revenue Recognition
- Revenue is recognized when earned
- Interest income is recognized when loans are disbursed
- Fees are recognized when charged

### Constraints

#### C1: Data Availability
- Report can only be generated for periods where data exists
- Historical data must be available in the system

#### C2: Calculation Methodology
- All calculations must follow SACCO accounting standards
- Calculations must be consistent with other financial reports

#### C3: Reporting Period
- Reports must support any date range
- Must handle fiscal year reporting

### Success Criteria

1. **Accuracy**: All P&L calculations are verified against manual calculations
2. **Completeness**: All revenue and expense categories are included
3. **Performance**: Report generates within 5 seconds
4. **Usability**: Users can generate reports without training
5. **Reliability**: Report generation succeeds 99.9% of the time
6. **Auditability**: All report generation is logged and traceable

