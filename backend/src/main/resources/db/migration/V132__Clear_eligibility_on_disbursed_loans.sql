-- Clear eligibility status fields on all DISBURSED loans
-- These were set during approval and should not be displayed on active loans
UPDATE loans
SET 
    member_eligibility_status = NULL,
    member_eligibility_errors = NULL,
    member_eligibility_warnings = NULL
WHERE status = 'DISBURSED';
