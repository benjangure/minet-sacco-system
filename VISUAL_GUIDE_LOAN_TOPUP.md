# Visual Guide: Loan Top-Up Feature

## What You Should See Now

### 1. Loans Table - Delete Button

```
┌──────────────────────────────────────────────────────────────────────┐
│ Loans                                                  [+ New Loan]  │
├──────────────────────────────────────────────────────────────────────┤
│ Loan No.  │ Member          │ Product  │ Amount     │ Status │ Actions│
├──────────────────────────────────────────────────────────────────────┤
│ LN-2026-  │ Mr Katee       │ Normal   │ KES        │ DISBUR │ 👁️ 👥  │
│ 00002     │ Mutunga        │ Loan     │ 329,297    │  SED   │ ✏️ 🗑️  │ ← DELETE ICON (red)
└──────────────────────────────────────────────────────────────────────┘
```

**Before**: Only 👁️ (Eye), 👥 (Guarantors), ✏️ (Edit) icons
**After**: Added 🗑️ (Trash2) icon in RED for TREASURER

---

### 2. Loan Details Dialog - NEW Layout

```
┌─────────────────────────────────────────────────────────────────────────┐
│  Loan Details                                                    [Close]│
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─ Loan Summary ─────────────────────────────────────────────────────┐│
│  │ ID: 366  │ Status: DISBURSED │ Member: Mr Katee Mutunga           ││
│  └──────────────────────────────────────────────────────────────────────┘│
│                                                                          │
│  ┌─ Amount Details ───────────────────────────────────────────────────┐│
│  │ Principal: KES 329,297  │ Rate: 12%  │ Term: 48 months            ││
│  │ Outstanding: KES 138,635.69  │ Interest Collected: KES 15,360.6   ││
│  └──────────────────────────────────────────────────────────────────────┘│
│                                                                          │
│  ┌─ 💰 Loan Top-Up ────────────────────────────────────────[+ Add Top-Up]┐ ← NEW!
│  │                                                                       ││
│  │  Top-Up History (1)                                                  ││
│  │  ┌──────────────────────────────────────────────────────────────┐   ││
│  │  │ KES 50,000                         Top-Up #1                 │   ││
│  │  │ 1/12/2025                                                     │   ││
│  │  │ Before: KES 138,635.69  →  After: KES 188,635.69            │   ││
│  │  │ ✓ Principal paid before top-up: KES 190,661.31              │   ││
│  │  │ Purpose: Test top-up                                         │   ││
│  │  └──────────────────────────────────────────────────────────────┘   ││
│  │                                                                       ││
│  └───────────────────────────────────────────────────────────────────────┘│
│                                                                          │
│  ┌─ Repayment Progress ────────────────────────────────────────────────┐│ ← Was here before
│  │  Repayment Status: 57.90% ████████████░░░░░░░░░░░░░░                ││
│  │  Principal: KES 329,297  │  Interest Collected: KES 15,360.6        ││
│  │  Principal Repaid: KES 190,661.31  │  Outstanding: KES 138,635.69  ││
│  │  💡 Eligibility Impact: As you repay, capacity increases...          ││
│  └──────────────────────────────────────────────────────────────────────┘│
│                                                                          │
│  ┌─ 📝 Phase A: Edit Loan Fields ────────────────────────────────────┐│ ← Already exists
│  │  (No guarantor changes)                               [Edit Fields]││
│  └──────────────────────────────────────────────────────────────────────┘│
│                                                                          │
│  ┌─ Guarantors (5) ────────────────────────────────────────────────────┐│
│  │  [Guarantor cards shown here]                                        ││
│  └──────────────────────────────────────────────────────────────────────┘│
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

**KEY CHANGE**: Top-up section now appears ABOVE "Repayment Progress"

---

### 3. Top-Up Dialog (NEW)

```
┌─────────────────────────────────────────────────────────┐
│  Add Loan Top-Up                              [X Close] │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌─ Loan Summary (purple theme) ──────────────────────┐ │
│  │ Loan: LN-2026-00002                                 │ │
│  │ Member: Mr Katee Mutunga                            │ │
│  │ Original Principal: KES 329,297                     │ │
│  │ Current Outstanding: KES 138,635.69                 │ │
│  └─────────────────────────────────────────────────────┘ │
│                                                          │
│  Top-Up Amount (KES) *                                   │
│  ┌─────────────────────────────────────────────────────┐│
│  │ 25000                                                ││ ← User enters amount
│  └─────────────────────────────────────────────────────┘│
│                                                          │
│  ┌─ Top-Up Preview (blue theme) ──────────────────────┐ │
│  │ Current Outstanding:    KES 138,635.69              │ │
│  │ Top-Up Amount:          +KES 25,000                 │ │
│  │ New Outstanding:        KES 163,635.69              │ │
│  │ Principal Paid So Far:  KES 190,661.31              │ │ ← PRESERVED!
│  │                                                      │ │
│  │ ✓ This top-up will be added to the loan.           │ │
│  │   Previous payments are preserved.                  │ │
│  └─────────────────────────────────────────────────────┘ │
│                                                          │
│  Purpose (Optional)                                      │
│  ┌─────────────────────────────────────────────────────┐│
│  │ Additional business capital                          ││
│  └─────────────────────────────────────────────────────┘│
│                                                          │
│  ⚠️ Note: If you need new guarantors for this top-up,  │
│     you can add them after clicking "Add Top-Up".       │
│                                                          │
│                              [Cancel]  [Add Top-Up]     │
└─────────────────────────────────────────────────────────┘
```

---

### 4. Delete Loan Dialog (NEW)

```
┌─────────────────────────────────────────────────────────┐
│  ⚠️ Delete Loan                               [X Close] │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌─ Loan Details (red theme) ─────────────────────────┐ │
│  │ Loan: LN-2026-00002                                 │ │
│  │ Member: Mr Katee Mutunga                            │ │
│  │ Amount: KES 329,297                                 │ │
│  │ Status: DISBURSED                                   │ │
│  └─────────────────────────────────────────────────────┘ │
│                                                          │
│  ⚠️ Warning: This action cannot be undone. All loan     │
│     data, guarantors, and repayment history will be     │
│     permanently deleted.                                 │
│                                                          │
│  Reason for Deletion *                                   │
│  ┌─────────────────────────────────────────────────────┐│
│  │ Duplicate entry - migrated loan already exists      ││
│  │                                                      ││
│  │                                                      ││
│  └─────────────────────────────────────────────────────┘│
│                                                          │
│                              [Cancel]  [Confirm Delete] │
└─────────────────────────────────────────────────────────┘
```

---

### 5. Phase A Edit Form (Already Exists - Verified)

```
┌─────────────────────────────────────────────────────────┐
│  📝 Phase A: Edit Loan Fields                           │
│  (No guarantor changes)                                  │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Loan Status                                             │
│  [DISBURSED  ▼]  ← Dropdown                             │
│                                                          │
│  Disbursement Date                                       │
│  [2025-01-12]  ← Date picker                            │
│                                                          │
│  Interest Rate (%)                                       │
│  [12]  ← Number input                                   │
│                                                          │
│  Outstanding Balance (KES) - Max: 329,297               │
│  [138635.69]  ← Number input                            │
│                                                          │
│  Interest Collected (KES) [Migrated Loans Only]         │
│  [15360.6]  ← Number input                              │
│                                                          │
│  Purpose                                                 │
│  ┌─────────────────────────────────────────────────────┐│
│  │ Migrated loan                                        ││
│  └─────────────────────────────────────────────────────┘│
│                                                          │
│  ✅ This form only sends Phase A fields. No guarantor   │
│     data will be included in the request.               │
│                                                          │
│              [Cancel]  [Update Loan Fields]             │
└─────────────────────────────────────────────────────────┘
```

**CRITICAL**: NO guarantor fields in Phase A!

---

## Color Coding

### Top-Up Section
- **Background**: Purple-to-indigo gradient (`from-purple-50 to-indigo-50`)
- **Border**: Purple (`border-purple-200`)
- **Button**: Purple (`bg-purple-600`)
- **Theme**: 💰 (money bag emoji)

### Delete Dialog
- **Background**: Red (`bg-red-50`)
- **Border**: Red (`border-red-200`)
- **Button**: Red (`bg-red-600`)
- **Icon**: ⚠️ (warning emoji)

### Phase A Edit
- **Background**: Indigo (`bg-indigo-50`)
- **Border**: Indigo (`border-indigo-200`)
- **Button**: Indigo (`bg-indigo-600`)
- **Icon**: 📝 (memo emoji)

### Repayment Progress
- **Background**: Blue gradient (`from-blue-50 to-indigo-50`)
- **Border**: Blue (`border-blue-200`)
- **Icon**: 💡 (light bulb emoji)

---

## User Flow Example

### Adding a Top-Up (Full Flow)

1. **Navigate to Loans**
   ```
   [Dashboard] → [Loans]
   ```

2. **Find Loan 366**
   ```
   Search: "Katee Mutunga" or "LN-2026-00002"
   ```

3. **Open Loan Details**
   ```
   Click 👁️ (Eye icon) on loan row
   ```

4. **See Top-Up Section** (NEW!)
   ```
   Purple section appears ABOVE "Repayment Progress"
   Shows history if any top-ups exist
   ```

5. **Click "Add Top-Up"**
   ```
   Purple button on right side of top-up section
   ```

6. **Enter Amount**
   ```
   Type: 25000
   Preview automatically loads showing new outstanding
   ```

7. **Enter Purpose (Optional)**
   ```
   Type: "Additional business capital"
   ```

8. **Submit**
   ```
   Click "Add Top-Up" button
   Toast: "Loan top-up added successfully"
   ```

9. **Verify**
   ```
   - New top-up appears in history
   - Outstanding updated: 138,635.69 + 25,000 = 163,635.69
   - Principal repaid PRESERVED: 190,661.31 ✓
   - Percentage still calculated on original principal
   ```

---

## What Changed vs. Before

### Before (Old UI)
```
- No top-up section at all
- No delete button
- Phase A edit existed but hard to verify restrictions
```

### After (New UI)
```
✅ Top-up section visible ABOVE "Repayment Progress"
✅ Delete button (🗑️) in table actions for TREASURER
✅ Phase A edit restrictions clearly documented
✅ All features production-ready
```

---

## Quick Visual Test

### Open Loan 366 and check this order:

1. ✅ **Loan Summary** (gray/blue)
2. ✅ **Amount Details** (gray/blue)
3. ✅ **💰 Loan Top-Up** (PURPLE - NEW!)  ← Check this is HERE
4. ✅ **Repayment Progress** (blue gradient)
5. ✅ **📝 Phase A Edit** (indigo)
6. ✅ **Guarantors** (yellow/green)

**If top-up is NOT between "Amount Details" and "Repayment Progress", something is wrong!**

---

## Browser Console Check

Open browser console (F12) and check for errors:

**Expected**: No errors, clean console
**Common Issues**:
- `Cannot read property 'id' of null` → State not initialized
- `404 Not Found /api/loans/...` → Backend not running
- `CORS error` → Backend CORS config issue

---

## Quick Commands

### Start Backend
```bash
cd backend
mvn spring-boot:run
```

### Start Frontend
```bash
cd minetsacco-main
npm run dev
```

### Check Backend Health
```bash
curl http://localhost:9090/api/health
# OR
curl http://localhost:9090/api/loans
```

### Check Frontend
```bash
# Open browser
http://localhost:3000
```

---

**Document Version**: 1.0
**Created**: 2026-07-28
**Purpose**: Visual guide for testing and verification
