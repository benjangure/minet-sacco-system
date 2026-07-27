# Deployment Checklist - Dual-Mode Loan Migration

**Target Environment:** Production  
**Deployment Date:** [To be scheduled]  
**Version:** 1.0  
**Risk Level:** LOW (Backward compatible, no schema changes)

---

## Pre-Deployment

### Code Review
- [ ] Review `LoanGuarantorUpdateService.java` for logic correctness
- [ ] Review `LoanMigrationService.java` changes for edge cases
- [ ] Verify no new dependencies introduced
- [ ] Confirm Maven build passes: `mvn clean package`
- [ ] Verify test suite runs: `mvn test`

### Documentation Review
- [ ] Technical implementation doc reviewed
- [ ] Quick start guide reviewed
- [ ] Error messages checked for clarity
- [ ] Example scenarios verified

### Database Preparation
- [ ] Confirm NO schema migration scripts needed
- [ ] Backup production database
- [ ] Verify loan_migration_items table has all required columns:
  - [ ] loan_number (nullable)
  - [ ] term_months (nullable)
  - [ ] disbursement_date (nullable)
  - [ ] outstanding_balance (nullable)
- [ ] Check audit_log table has sufficient capacity

### Environment Setup
- [ ] Staging environment matches production schema
- [ ] All required services running (database, message queue if used)
- [ ] Verify network connectivity
- [ ] Check disk space (logs, uploads)

---

## Pre-Production Testing

### Unit Tests
- [ ] `LoanGuarantorUpdateService` tests pass
- [ ] `LoanMigrationService` CREATE mode tests pass
- [ ] `LoanMigrationService` UPDATE mode tests pass
- [ ] Validation tests pass (all error scenarios)
- [ ] Code coverage meets team standard (>80%)

### Integration Tests
- [ ] Full CREATE workflow tested end-to-end
- [ ] Full UPDATE workflow tested end-to-end
- [ ] Mixed batch (CREATE + UPDATE) tested
- [ ] Guarantor freeze/unfreeze mechanics verified
- [ ] Audit trail creation verified

### System Tests
- [ ] Application starts without errors
- [ ] Loan migration endpoints accessible
- [ ] Template download works
- [ ] File upload works
- [ ] Database transactions are atomic

### Performance Tests
- [ ] Batch of 100 CREATE rows: < 30 seconds
- [ ] Batch of 50 UPDATE rows (guarantor changes): < 30 seconds
- [ ] Memory usage stable (no leaks)
- [ ] No database connection pool exhaustion

### Security Tests
- [ ] Unauthorized access blocked
- [ ] Audit trail captures user/timestamp
- [ ] No SQL injection vulnerabilities
- [ ] Error messages don't leak sensitive data
- [ ] File upload validates file type

### Regression Tests
- [ ] Old CREATE-only workflows still work
- [ ] Existing loans not affected by deployment
- [ ] API response format unchanged
- [ ] Error codes unchanged
- [ ] Authentication/authorization unchanged

---

## Staging Deployment

### Code Deployment
- [ ] Deploy `LoanGuarantorUpdateService.java`
- [ ] Deploy updated `LoanMigrationService.java`
- [ ] Rebuild application: `mvn clean package`
- [ ] Deploy WAR file to staging

### Validation
- [ ] Application starts (check logs)
- [ ] Services autowire correctly (check logs)
- [ ] Database connections established
- [ ] Endpoints respond: `/api/loan-migration/template/download`
- [ ] No startup errors

### Staging Test Suite
- [ ] Run full test suite (2+ hours)
- [ ] Test CREATE mode (10+ scenarios)
- [ ] Test UPDATE mode (10+ scenarios)
- [ ] Test error scenarios (5+ edge cases)
- [ ] Test guarantor replacement (3+ scenarios)
- [ ] Monitor logs for warnings/errors

### Staging User Testing (Optional)
- [ ] Finance team tests CREATE workflow
- [ ] Finance team tests UPDATE workflow
- [ ] Collect feedback
- [ ] Document any issues

### Staging Sign-Off
- [ ] [ ] QA Lead: All tests passed _________________ Date: _____
- [ ] [ ] Tech Lead: Code ready for production _________________ Date: _____
- [ ] [ ] Product Owner: Features meet requirements _________________ Date: _____

---

## Production Deployment Plan

### Deployment Window
- [ ] Maintenance window scheduled: _________ (recommended: off-peak hours)
- [ ] Deployment duration estimated: 30 minutes
- [ ] Rollback plan prepared (see below)
- [ ] Stakeholders notified

### Deployment Steps
1. [ ] Create backup of production database
2. [ ] Deploy application WAR file
3. [ ] Restart application server
4. [ ] Verify application starts (check logs)
5. [ ] Verify database connectivity
6. [ ] Test loan migration endpoints
7. [ ] Test template download
8. [ ] Verify audit trail logging

### Post-Deployment Validation
- [ ] Application running (no errors in logs)
- [ ] Can access `/api/loan-migration/template/download`
- [ ] Can upload test file (small batch)
- [ ] Audit trail records actions
- [ ] Performance acceptable (response times normal)
- [ ] Database storage healthy (space, performance)

### Monitoring (First 24 Hours)
- [ ] Monitor error logs every 2 hours
- [ ] Monitor CPU/memory usage
- [ ] Monitor database performance
- [ ] Monitor response times
- [ ] Alert on:
  - [ ] Any exceptions in logs
  - [ ] CPU usage > 80%
  - [ ] Memory usage > 85%
  - [ ] Database connections > 90% pool

### Communication
- [ ] [ ] Notify dev team: Production deployed _________________ Time: _____
- [ ] [ ] Notify support team: Feature now available _________________ Time: _____
- [ ] [ ] Notify users: New UPDATE capability available _________________ Time: _____

---

## Rollback Plan

**Trigger Rollback If:**
- [ ] Application won't start
- [ ] Loan migration endpoints throw errors
- [ ] Existing CREATE workflows broken
- [ ] Data corruption detected
- [ ] Database performance degraded > 50%
- [ ] Cannot reach SLA (99.5% uptime)

### Rollback Steps
1. [ ] Stop application server
2. [ ] Restore previous WAR file (pre-deployment version)
3. [ ] Restart application server
4. [ ] Verify application starts
5. [ ] Test existing workflows work
6. [ ] Verify database queries normal
7. [ ] Document rollback reason
8. [ ] Notify stakeholders

### Rollback Verification
- [ ] Application running (old version)
- [ ] CREATE mode works (existing data intact)
- [ ] No data loss
- [ ] Performance normal
- [ ] Monitoring shows stability

---

## Post-Deployment (First Week)

### Daily Checks
- [ ] Day 1: No errors in application logs
- [ ] Day 1: No database performance issues
- [ ] Day 2: User feedback collected (any issues?)
- [ ] Day 3: Check audit trail logging working correctly
- [ ] Day 5: Run full test suite again

### Issue Resolution
- [ ] Document any issues encountered
- [ ] Prioritize by severity
- [ ] Fix and re-test issues
- [ ] Update documentation if needed
- [ ] Communicate resolution to stakeholders

### Success Criteria
- [ ] Zero critical issues
- [ ] Zero data corruption
- [ ] Users successfully creating loans (CREATE)
- [ ] Users successfully updating loans (UPDATE)
- [ ] Audit trail complete and accurate
- [ ] Performance metrics: response time < 1 sec
- [ ] Uptime: > 99.5%

### Documentation Update
- [ ] Update internal wiki with deployment notes
- [ ] Document any configuration changes
- [ ] Update team on lessons learned
- [ ] Archive this checklist with sign-offs

---

## Sign-Off

### Pre-Deployment Approval
**QA Lead:**  
- [ ] Tested: _________________________ Date: _____ Signature: _____

**Tech Lead:**  
- [ ] Reviewed code: _________________________ Date: _____ Signature: _____

**Database Admin:**  
- [ ] DB backup verified: _________________________ Date: _____ Signature: _____

**Product Owner:**  
- [ ] Features approved: _________________________ Date: _____ Signature: _____

### Deployment Approval
**Deployment Manager:**  
- [ ] Ready to deploy: _________________________ Date: _____ Signature: _____

**System Owner:**  
- [ ] Production environment approved: _________________________ Date: _____ Signature: _____

### Post-Deployment Sign-Off
**Operations:**  
- [ ] Deployment successful: _________________________ Date: _____ Signature: _____

**QA Lead:**  
- [ ] Post-deployment validation passed: _________________________ Date: _____ Signature: _____

**Business Owner:**  
- [ ] Feature accepted for production: _________________________ Date: _____ Signature: _____

---

## Deployment Metrics

### Code Changes
- **New Files:** 1 (LoanGuarantorUpdateService.java - 300 lines)
- **Modified Files:** 1 (LoanMigrationService.java - +600 lines)
- **Deleted Files:** 0
- **Breaking Changes:** None
- **Backward Compatibility:** 100%

### Database Impact
- **Schema Changes:** None required
- **Data Migration:** None required
- **Estimated Time:** < 5 minutes
- **Rollback Time:** < 5 minutes

### Testing Impact
- **New Tests Added:** (To be added by team)
- **Existing Tests Affected:** None (all pass)
- **Test Coverage:** >80% (recommended)

### Performance Impact
- **Expected Improvement:** None (same or better)
- **Expected Degradation:** None
- **Query Performance:** Unchanged or improved
- **Response Times:** <1 second (typical)

---

## Lessons Learned Template

### What Went Well
- [ ] _________________________________________________
- [ ] _________________________________________________
- [ ] _________________________________________________

### What Could Be Improved
- [ ] _________________________________________________
- [ ] _________________________________________________
- [ ] _________________________________________________

### Action Items for Next Deployment
- [ ] _________________________________________________
- [ ] _________________________________________________
- [ ] _________________________________________________

---

## References

**Related Documentation:**
- `DUAL_MODE_LOAN_MIGRATION_IMPLEMENTATION.md` - Technical details
- `DUAL_MODE_LOAN_MIGRATION_QUICK_START.md` - User guide
- `IMPLEMENTATION_SUMMARY.md` - Overview

**Contact Information:**
- **Tech Lead:** _________________________ Phone: __________ Email: __________
- **Database Admin:** _________________________ Phone: __________ Email: __________
- **QA Lead:** _________________________ Phone: __________ Email: __________
- **On-Call Support:** _________________________ Phone: __________

---

**Deployment Status:** ⏳ PENDING APPROVAL

**Date Prepared:** June 23, 2026  
**Prepared By:** Development Team  
**Last Updated:** June 23, 2026
