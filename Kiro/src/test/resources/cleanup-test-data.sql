-- Cleanup test data before each property test
DELETE FROM appraisal_forms WHERE id >= 1;
DELETE FROM appraisal_cycles WHERE id >= 1;
DELETE FROM appraisal_templates WHERE id >= 1;
DELETE FROM user_roles WHERE user_id >= 1000;
DELETE FROM users WHERE id >= 1000;
