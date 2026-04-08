-- =====================================================
-- Migration: V1 - Create User and Role Tables
-- Description: Creates users, roles, user_roles, and reporting_hierarchy tables
-- =====================================================

-- Create roles table
CREATE TABLE roles (
    id   INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(50) NOT NULL UNIQUE
);

-- Create users table
CREATE TABLE users (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    employee_id     NVARCHAR(50)  NOT NULL UNIQUE,
    full_name       NVARCHAR(200) NOT NULL,
    email           NVARCHAR(200) NOT NULL UNIQUE,
    password_hash   NVARCHAR(255) NOT NULL,
    designation     NVARCHAR(200),
    department      NVARCHAR(200),
    manager_id      BIGINT REFERENCES users(id),
    is_active       BIT           NOT NULL DEFAULT 1,
    created_at      DATETIME2     NOT NULL DEFAULT GETUTCDATE(),
    updated_at      DATETIME2     NOT NULL DEFAULT GETUTCDATE()
);

-- Create user_roles junction table
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id INT    NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Create reporting_hierarchy table
CREATE TABLE reporting_hierarchy (
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    employee_id  BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    manager_id   BIGINT    NOT NULL REFERENCES users(id),
    effective_dt DATE      NOT NULL,
    end_dt       DATE,
    created_at   DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    CONSTRAINT chk_reporting_dates CHECK (end_dt IS NULL OR end_dt >= effective_dt)
);

-- Create indexes for users table
CREATE INDEX idx_users_employee_id ON users(employee_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_manager_id ON users(manager_id);
CREATE INDEX idx_users_is_active ON users(is_active);
CREATE INDEX idx_users_department ON users(department);

-- Create indexes for user_roles table
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- Create indexes for reporting_hierarchy table
CREATE INDEX idx_reporting_hierarchy_employee_id ON reporting_hierarchy(employee_id);
CREATE INDEX idx_reporting_hierarchy_manager_id ON reporting_hierarchy(manager_id);
CREATE INDEX idx_reporting_hierarchy_effective_dt ON reporting_hierarchy(effective_dt);
CREATE INDEX idx_reporting_hierarchy_end_dt ON reporting_hierarchy(end_dt);
