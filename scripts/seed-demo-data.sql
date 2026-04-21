-- Optional local demo helper.
-- Run against authdb only after reviewing the values.
-- This promotes the known local demo account to ADMIN if it exists.

UPDATE users
SET role = 'ROLE_ADMIN',
    enabled = true,
    deleted = false,
    failed_login_attempts = 0,
    lock_time = NULL,
    suspension_end_time = NULL
WHERE email = 'admin@example.com';

-- Optional: normalize demo users created from the UI.
UPDATE users
SET enabled = true,
    deleted = false,
    failed_login_attempts = 0,
    lock_time = NULL,
    suspension_end_time = NULL
WHERE email LIKE 'demo%@example.com';
