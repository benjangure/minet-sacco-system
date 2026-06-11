# GL Configuration Frontend Page - Implementation Summary

## Overview
Created a comprehensive GL Configuration page (`GLConfiguration.tsx`) for managing GL accounts and period entries with two main tabs for treasurers and admins.

## Files Created/Modified

### New Files
- **minetsacco-main/src/pages/GLConfiguration.tsx** - Main GL Configuration page with two tabs

### Modified Files
- **minetsacco-main/src/App.tsx** - Added import and route for GLConfiguration
- **minetsacco-main/src/components/AppSidebar.tsx** - Added GL/Accounting navigation section

## Page Structure

### Tab 1: GL Accounts
**Purpose**: View, create, and edit GL accounts

#### Table Columns
- Code
- Name
- Type (Account Type)
- Source (AGGREGATION/MANUAL_ENTRY)
- Normal Balance (DEBIT/CREDIT)
- Section Label
- Status (Active/Inactive)
- Actions (Edit button)

#### "Add Account" Modal Form
**Form Fields:**
1. **Code** (text input, required) - Unique identifier like "NORMAL_LOAN"
2. **Name** (text input, required) - Display name like "Normal Loan"
3. **Account Type** (dropdown, required)
   - ASSET
   - LIABILITY
   - EQUITY
   - REVENUE
   - EXPENSE
4. **Source** (dropdown, required)
   - AGGREGATION (auto-calculation)
   - MANUAL_ENTRY (manual data entry)
5. **Data Source** (dropdown, shown when AGGREGATION selected)
   - Populated from GET /api/gl/data-sources
   - Options: Loans, Member Savings, Member Shares, Transactions
6. **Loan Product** (dropdown, shown when "Loans" selected as data source)
   - Populated from loanProducts array in data sources response
   - Allows mapping to specific loan products like "Normal Loan" or "Emergency Loan 1"
7. **Normal Balance** (dropdown, required)
   - DEBIT or CREDIT
   - Auto-populated based on account type:
     - ASSET → DEBIT
     - EXPENSE → DEBIT
     - LIABILITY → CREDIT
     - EQUITY → CREDIT
     - REVENUE → CREDIT
   - Can be manually overridden
8. **Section Label** (text input, optional)
   - Groups related accounts (e.g., "Cash and Cash Equivalents", "Loans", "Administrative Expenses")
9. **Period Sensitive** (toggle, default off)
   - Only relevant for MANUAL_ENTRY accounts
   - Disabled when source is AGGREGATION
10. **Display Order** (number input, default 100)

#### Edit Functionality
- Edit button per row opens edit dialog
- **Editable fields only**:
  - Name
  - Section Label
  - Period Sensitive
  - Display Order
  - Active status
- **Read-only fields** (cannot be changed after creation):
  - Code
  - Account Type
  - Source (AGGREGATION/MANUAL_ENTRY)

#### API Endpoints Used
- GET /api/gl/accounts - Fetch all GL accounts
- POST /api/gl/accounts - Create new GL account
- PUT /api/gl/accounts/{id} - Update GL account
- GET /api/gl/data-sources - Fetch available data sources

---

### Tab 2: Period Entry
**Purpose**: View, enter, and approve GL entries for specific periods

#### Period Selection
- Month dropdown (1-12, defaults to current month)
- Year dropdown (last 5 years, defaults to current year)
- "Load" button to fetch data

#### Table Columns
- Section (Section Label)
- Code
- Name
- Type (Account Type)
- Source (AUTO/MANUAL badge)
- Amount (editable for MANUAL, read-only for AUTO)
- Status (Period Status badge with colors)

#### Row Behavior

**AGGREGATION rows (AUTO):**
- Shows calculated amount in grey color
- "AUTO" badge for source
- Input is read-only
- Amount populated from calculation service

**MANUAL_ENTRY rows (MANUAL):**
- Shows editable amount input
- Period Status badge with color coding:
  - DRAFT = grey background
  - POSTED = blue background
  - APPROVED = green background
  - LOCKED = red background
- LOCKED entries: input read-only
- editable for DRAFT and POSTED statuses

#### Admin-Only Actions (when role = ADMIN)
Per row action buttons appear based on period status:
- **POSTED entries**: Approve and Reject buttons
- **APPROVED entries**: Lock button

#### Bottom Action Buttons

**"Save Drafts" Button:**
- Appears when entries have been modified
- Calls POST /api/gl/period-entry for each changed MANUAL entry
- Saves entries as DRAFT status
- Only saves entries that were modified

**"Submit All for Approval" Button:**
- Enabled only when at least one DRAFT entry exists
- Calls PUT /api/gl/period-entry/{id}/submit for all DRAFT entries
- Changes status from DRAFT to POSTED

#### API Endpoints Used
- GET /api/gl/period-entry?periodMonth=X&periodYear=Y - Fetch entries for period
- POST /api/gl/period-entry - Create/update manual entry
- PUT /api/gl/period-entry/{id}/submit - Submit for approval
- PUT /api/gl/period-entry/{id}/approve - Approve entry (ADMIN only)
- PUT /api/gl/period-entry/{id}/reject - Reject entry (ADMIN only)
- PUT /api/gl/period-entry/{id}/lock - Lock entry (ADMIN only)

## Key Features

### Smart Form Logic
- Normal balance auto-populated based on account type, with manual override option
- Conditional field visibility:
  - Data Source dropdown only shown when source = AGGREGATION
  - Loan Product dropdown only shown when data source = LOANS
  - Period Sensitive toggle disabled for AGGREGATION accounts
- Loan product options dynamically loaded from data sources response

### State Management
- Separate state for Tab 1 (accounts) and Tab 2 (period entries)
- Track edited amounts and draft entries separately
- Automatic reset of form data when opening add dialog

### Error Handling
- Toast notifications for success/error messages
- Graceful handling of API failures
- Validation of required fields

### User Experience
- Loading states for async operations
- Read-only fields clearly disabled in edit dialog
- Color-coded status badges for quick visual identification
- Responsive table layout with horizontal scroll on mobile

## Navigation Integration
- Added to AppSidebar under new "GL/Accounting" section
- Accessible to ADMIN and TREASURER roles
- Appears between main menu and administration sections
- Uses Sliders icon for GL Configuration entry

## Role-Based Access
- **TREASURER**: Can view, create, and edit GL accounts; can enter manual entries and submit for approval
- **ADMIN**: Full access including approval, rejection, and locking of period entries

## Styling & Components
- Uses existing shadcn/ui components:
  - Card, Button, Input, Label
  - Table with scroll support
  - Badge for status indicators
  - Dialog for modals
  - Select for dropdowns
  - Tabs for tab switching
- Consistent with existing Minet SACCO UI patterns
