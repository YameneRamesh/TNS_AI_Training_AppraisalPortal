-- =====================================================
-- Migration: V4 - Insert Seed Data for Roles
-- Description: Inserts the four system roles: EMPLOYEE, MANAGER, HR, ADMIN
-- =====================================================

-- Insert system roles
INSERT INTO roles (name) VALUES ('EMPLOYEE');
INSERT INTO roles (name) VALUES ('MANAGER');
INSERT INTO roles (name) VALUES ('HR');
INSERT INTO roles (name) VALUES ('ADMIN');
