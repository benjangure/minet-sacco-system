# Profit & Loss Report - Implementation Tasks

## Phase 1: Backend Implementation

### 1. Create Data Transfer Objects (DTOs)
- [x] 1.1 Create `ProfitLossReportDTO` with revenue, expenses, and net profit fields
- [x] 1.2 Create `RevenueDTO` with interest income, fees, and other income breakdowns
- [x] 1.3 Create `ExpenseDTO` with operating expenses, provisions, and other expenses
- [x] 1.4 Create `PeriodDTO` for date range representation

### 2. Create Database Queries
- [x] 2.1 Query for interest income from disbursed/repaid loans
- [x] 2.2 Query for loan loss provisions from defaulted loans
- [x] 2.3 Query for operating expenses from transactions
- [x] 2.4 Query for fees and charges from transactions
- [ ] 2.5 Query for other income sources

### 3. Create ProfitLossReportService
- [x] 3.1 Implement `generateProfitLossReport(startDate, endDate)` method
- [x] 3.2 Implement `calculateInterestIncome(startDate, endDate)` method
- [x] 3.3 Implement `calculateLoanLossProvisions(startDate, endDate)` method
- [x] 3.4 Implement `calculateOperatingExpenses(startDate, endDate)` method
- [x] 3.5 Implement `calculateFeesAndCharges(startDate, endDate)` method
- [x] 3.6 Implement `calculateNetProfitLoss(revenue, expenses)` method
- [x] 3.7 Add comprehensive logging for debugging

### 4. Create API Endpoint
- [x] 4.1 Add `GET /api/reports/profit-loss` endpoint to ReportsController
- [x] 4.2 Add role-based access control (ADMIN, AUDITOR only)
- [x] 4.3 Add input validation for date parameters
- [x] 4.4 Add error handling and user-friendly error messages
- [x] 4.5 Add audit logging for report generation

### 5. Create Export Functionality
- [x] 5.1 Implement Excel export for P&L report
- [x] 5.2 Implement PDF export for P&L report
- [x] 5.3 Add proper formatting and styling to exports
- [x] 5.4 Add report title, period, and company name to exports

## Phase 2: Frontend Implementation

### 6. Create ProfitLossReport Component
- [x] 6.1 Create `ProfitLossReport.tsx` page component
- [x] 6.2 Add date range picker (start and end date)
- [x] 6.3 Add "Generate Report" button
- [x] 6.4 Add loading state during report generation
- [x] 6.5 Add error handling and user feedback

### 7. Display Report Data
- [x] 7.1 Create summary cards for Total Revenue, Total Expenses, Net Profit/Loss
- [x] 7.2 Create revenue breakdown table
- [x] 7.3 Create expense breakdown table
- [x] 7.4 Display profit margin percentage
- [x] 7.5 Add visual indicators for positive/negative values

### 8. Add Export Buttons
- [x] 8.1 Add "Export to Excel" button
- [x] 8.2 Add "Export to PDF" button
- [x] 8.3 Implement Excel export functionality
- [x] 8.4 Implement PDF export functionality
- [x] 8.5 Add success/error notifications

### 9. Add to Reports Menu
- [x] 9.1 Add P&L Report option to Reports.tsx dropdown
- [x] 9.2 Add navigation to P&L Report page
- [x] 9.3 Update sidebar menu if needed

## Phase 3: Testing & Validation

### 10. Unit Tests
- [x] 10.1 Test interest income calculation with various loan scenarios
- [x] 10.2 Test loan loss provision calculation
- [x] 10.3 Test operating expense calculation
- [x] 10.4 Test net profit/loss calculation
- [x] 10.5 Test date range filtering

### 11. Integration Tests
- [x] 11.1 Test API endpoint with valid date ranges
- [x] 11.2 Test API endpoint with invalid date ranges
- [x] 11.3 Test access control (ADMIN/AUDITOR only)
- [x] 11.4 Test export functionality (Excel and PDF)

### 12. Data Accuracy Validation
- [x] 12.1 Verify interest income matches loan data
- [x] 12.2 Verify loan loss provisions match defaulted loans
- [x] 12.3 Verify operating expenses match transaction data
- [x] 12.4 Verify net profit/loss calculation accuracy
- [x] 12.5 Test with sample data and manual calculations

### 13. Manual Testing
- [x] 13.1 Generate report for various date ranges
- [x] 13.2 Verify report data accuracy
- [x] 13.3 Test Excel export and verify formatting
- [x] 13.4 Test PDF export and verify formatting
- [x] 13.5 Test with different user roles

## Phase 4: Documentation & Deployment

### 14. Documentation
- [x] 14.1 Document API endpoint and parameters
- [x] 14.2 Document calculation methodology
- [x] 14.3 Create user guide for generating reports
- [x] 14.4 Document any assumptions or limitations

### 15. Deployment
- [x] 15.1 Code review and approval
- [x] 15.2 Merge to main branch
- [x] 15.3 Deploy to production
- [x] 15.4 Monitor for any issues

## Correctness Properties to Validate

### CP1: Revenue Completeness
**Property**: All interest-bearing loans in the period are included in revenue
**Test**: 
- Create test loans with different statuses
- Verify only DISBURSED and REPAID loans are included
- Verify interest amounts are correct

### CP2: Expense Accuracy
**Property**: All expense transactions are captured
**Test**:
- Create test expense transactions
- Verify all expenses are included in the report
- Verify amounts are correct

### CP3: Balance Equation
**Property**: Net Profit/Loss = Total Revenue - Total Expenses
**Test**:
- Generate report with known values
- Verify equation holds true
- Test with various revenue and expense combinations

### CP4: Non-Negative Values
**Property**: All component values are >= 0
**Test**:
- Verify no negative revenue values
- Verify no negative expense values
- Verify calculations don't produce negative intermediate values

### CP5: Period Consistency
**Property**: All data points use the same date range
**Test**:
- Generate report for specific period
- Verify all components use same date range
- Test boundary conditions (start and end dates)

