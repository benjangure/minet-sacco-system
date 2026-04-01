# Member Registration - Date of Birth Format Guide

## Problem
When you download the Member Registration template and edit the "Date of Birth" column in Excel, the dates get auto-formatted to your system locale (e.g., 9/17/1999 in US format). When you upload the file, the system rejects it with a validation error.

## Root Cause
Excel automatically detects date values and formats them according to your computer's regional settings. This causes the original format (YYYY-MM-DD) to be converted to a different format (M/D/YYYY), which may not match what the backend expects.

## Solution

### Accepted Date Formats
The system accepts dates in ANY of these formats:
- **YYYY-MM-DD** (e.g., 1990-01-15) - **RECOMMENDED**
- DD/MM/YYYY (e.g., 15/01/1990)
- MM/DD/YYYY (e.g., 01/15/1990)
- dd-MM-yyyy (e.g., 15-01-1990)
- M-d-yyyy (e.g., 1-15-1990)

### How to Prevent Excel Auto-Formatting

**Option 1: Format Column as Text (RECOMMENDED)**
1. Download the Member Registration template
2. Open it in Excel
3. Right-click on the "Date of Birth" column header
4. Select "Format Cells"
5. Choose the "Text" tab
6. Click OK
7. Now edit the dates - they will stay in YYYY-MM-DD format
8. Save the file
9. Upload to the system

**Option 2: Use Apostrophe Prefix**
1. When entering dates, prefix with an apostrophe: `'1990-01-15`
2. Excel will treat it as text and won't auto-format
3. The apostrophe won't be saved in the file

**Option 3: Use a Different Format**
1. If Excel keeps reformatting, use DD/MM/YYYY format instead
2. Example: 15/01/1990
3. The system will accept this format

### Step-by-Step Instructions

#### If You Already Have Dates in Wrong Format:

1. **Select the Date of Birth column**
   - Click on the column header (e.g., "E")

2. **Format as Text**
   - Right-click → Format Cells
   - Select "Text" → OK

3. **Re-enter the Dates**
   - Click on each cell with a date
   - Delete the current content
   - Type the date in YYYY-MM-DD format (e.g., 1990-01-15)
   - Press Enter

4. **Save the File**
   - Ctrl+S (or Cmd+S on Mac)

5. **Upload to the System**
   - Go to Bulk Processing
   - Select "Member Registration"
   - Upload your corrected file

### Example

**Before (Wrong Format - Will Fail):**
```
First Name | Last Name | Date of Birth
John       | Doe       | 1/15/1990
Jane       | Smith     | 5/20/1985
```

**After (Correct Format - Will Work):**
```
First Name | Last Name | Date of Birth
John       | Doe       | 1990-01-15
Jane       | Smith     | 1985-05-20
```

## Instructions Sheet in Template

The downloaded Member Registration template now includes an "Instructions" sheet with this information. Always refer to it when editing dates.

## Troubleshooting

**Q: I formatted the column as text but Excel still shows the date in wrong format?**
A: You need to re-enter the dates after formatting. Excel won't change existing values, only new ones.

**Q: Can I use different date formats for different rows?**
A: Yes! The system accepts mixed formats. You can use 1990-01-15 in one row and 15/01/1990 in another.

**Q: The upload still fails after following these steps?**
A: Check that:
- There are no extra spaces before/after the date
- The date is valid (e.g., not 1990-13-45)
- All required columns are filled
- No special characters in the date

## Backend Support

The backend date parser supports all these formats automatically:
- yyyy-MM-dd
- M/d/yyyy
- MM/dd/yyyy
- d/M/yyyy
- dd/MM/yyyy
- yyyy/MM/dd
- dd-MM-yyyy
- d-M-yyyy
- M-d-yyyy
- MM-dd-yyyy

So as long as you use a standard date format, the upload will work.
