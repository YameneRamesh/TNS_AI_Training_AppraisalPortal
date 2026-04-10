-- Cleanup script for integration tests
-- Executed before each test method to ensure clean state

-- Delete in order of foreign key dependencies
DELETE FROM audit_logs;
DELETE FROM appraisal_forms;
DELETE FROM appraisal_cycles;
DELETE FROM appraisal_templates;

-- Reset identity columns (optional, for consistent IDs in tests)
-- Note: SQL Server uses DBCC CHECKIDENT to reset identity
-- DBCC CHECKIDENT ('audit_logs', RESEED, 0);
-- DBCC CHECKIDENT ('appraisal_forms', RESEED, 0);
-- DBCC CHECKIDENT ('appraisal_cycles', RESEED, 0);
-- DBCC CHECKIDENT ('appraisal_templates', RESEED, 0);
