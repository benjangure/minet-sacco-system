-- V10: Add new account types for Kenyan SACCO standard
-- Adds: SCHOOL_FEES, HOLIDAY_FUND, EMERGENCY_FUND

-- Note: No schema changes needed as AccountType is an enum in Java
-- This migration is for documentation and future reference

-- The new account types will be automatically available:
-- 1. SCHOOL_FEES - Education fund for children
-- 2. HOLIDAY_FUND - Christmas/holiday savings (withdrawn in December)
-- 3. EMERGENCY_FUND - Personal emergency savings

-- These accounts will be created automatically when members are registered
-- or when bulk processing creates them on-demand
