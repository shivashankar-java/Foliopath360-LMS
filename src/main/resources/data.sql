-- Seed roles for the Foliopath360 LMS (idempotent)
INSERT IGNORE INTO roles (role_name, role_description) VALUES ('ADMIN', 'Administrator with full system access');
INSERT IGNORE INTO roles (role_name, role_description) VALUES ('STUDENT', 'Student with course access');
