-- Seed data: runs on startup (spring.sql.init.mode=always)
-- Passwords are BCrypt hashed. Plain-text values:
--   admin@finance.com   → password: Admin@123
--   analyst@finance.com → password: Analyst@123
--   viewer@finance.com  → password: Viewer@123

MERGE INTO users (id, email, password_hash, full_name, role, status, created_at, updated_at, deleted_at)
KEY(email)
VALUES
  (1, 'admin@finance.com',
   '$2a$12$5zA9JKJP2.k5OKBfvMBiYuIv4F5qdDplfNRF3dXxZT8rp.HxpJqq',
   'System Admin', 'ADMIN', 'ACTIVE', NOW(), NOW(), NULL),

  (2, 'analyst@finance.com',
   '$2a$12$XEbXSDPkJH3B/p3v8RyC4.3pMv2Ub5kT8oC4H7h3rC0vlb7ZxBVKi',
   'Jane Analyst', 'ANALYST', 'ACTIVE', NOW(), NOW(), NULL),

  (3, 'viewer@finance.com',
   '$2a$12$3VVhcQX8nHe7RMoFsFxRBuSi6hEjHQhVYdoTQdLsqZMaOoYxJCEhW',
   'Bob Viewer', 'VIEWER', 'ACTIVE', NOW(), NOW(), NULL);

-- Seed some financial records
MERGE INTO financial_records (id, amount, type, category, record_date, description, created_by_id, created_at, updated_at, deleted_at)
KEY(id)
VALUES
  (1, 50000.00, 'INCOME',  'Salary',        '2024-01-31', 'January salary',         1, NOW(), NOW(), NULL),
  (2, 1200.00,  'EXPENSE', 'Rent',           '2024-02-01', 'Monthly office rent',    1, NOW(), NOW(), NULL),
  (3, 30000.00, 'INCOME',  'Freelance',      '2024-02-15', 'Consulting project fee', 1, NOW(), NOW(), NULL),
  (4, 450.00,   'EXPENSE', 'Utilities',      '2024-02-20', 'Electricity and water',  1, NOW(), NOW(), NULL),
  (5, 50000.00, 'INCOME',  'Salary',         '2024-02-28', 'February salary',        1, NOW(), NOW(), NULL),
  (6, 8000.00,  'EXPENSE', 'Marketing',      '2024-03-05', 'Ad campaign spend',      1, NOW(), NOW(), NULL),
  (7, 50000.00, 'INCOME',  'Salary',         '2024-03-31', 'March salary',           1, NOW(), NOW(), NULL),
  (8, 1200.00,  'EXPENSE', 'Rent',           '2024-03-01', 'Monthly office rent',    1, NOW(), NOW(), NULL),
  (9, 15000.00, 'INCOME',  'Investment',     '2024-03-20', 'Dividend payout',        1, NOW(), NOW(), NULL),
  (10, 3500.00, 'EXPENSE', 'Software',       '2024-03-25', 'Annual SaaS licenses',   1, NOW(), NOW(), NULL);
