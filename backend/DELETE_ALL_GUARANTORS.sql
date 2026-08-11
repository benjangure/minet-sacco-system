-- ============================================================================
-- DELETE ALL GUARANTORS FROM DATABASE
-- ============================================================================
-- ⚠️ WARNING: This will permanently delete ALL guarantor records!
-- Use this only if you want to clean up test data
-- Run on production server with caution
-- ============================================================================

USE minetsacco;

-- Show count before deletion
SELECT 
    COUNT(*) AS total_guarantors,
    COUNT(CASE WHEN approved = TRUE THEN 1 END) AS approved_guarantors,
    COUNT(CASE WHEN approved = FALSE THEN 1 END) AS pending_guarantors
FROM guarantors;

-- ⚠️ UNCOMMENT THE LINE BELOW TO DELETE ALL GUARANTORS
-- DELETE FROM guarantors;

-- Show count after deletion (should be 0)
SELECT COUNT(*) AS remaining_guarantors FROM guarantors;

-- Success message
SELECT 'Review the counts above. Uncomment the DELETE line if you want to proceed.' AS Status;

-- ============================================================================
-- ALTERNATIVE: Delete only PENDING (unapproved) guarantors
-- ============================================================================
-- If you only want to delete pending guarantor requests (not approved ones):
-- DELETE FROM guarantors WHERE approved = FALSE;

-- ============================================================================
-- ALTERNATIVE: Delete guarantors for specific loan
-- ============================================================================
-- If you want to delete guarantors for a specific loan:
-- DELETE FROM guarantors WHERE loan_id = YOUR_LOAN_ID;

-- ============================================================================
-- TO USE THIS SCRIPT:
-- ============================================================================
-- 1. First run as-is to see the count of guarantors
-- 2. If you're sure you want to delete, uncomment the DELETE line
-- 3. Run again to delete all guarantors
-- 4. Verify the count is 0
