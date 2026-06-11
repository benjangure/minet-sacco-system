-- Drop existing tables in reverse dependency order
DROP TABLE IF EXISTS gl_account_audit;
DROP TABLE IF EXISTS gl_manual_entries;
DROP TABLE IF EXISTS gl_account_calculations;
DROP TABLE IF EXISTS gl_accounts;

-- GL ACCOUNTS MASTER TABLE
CREATE TABLE IF NOT EXISTS gl_accounts (
  id INT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(50) UNIQUE NOT NULL,
  name VARCHAR(255) NOT NULL,
  account_type ENUM('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE') NOT NULL,
  balance_calculation_type ENUM('AGGREGATION', 'FORMULA', 'MANUAL_ENTRY', 'COMPUTED') NOT NULL,
  calculation_config LONGTEXT NOT NULL,
  is_active BOOLEAN DEFAULT TRUE,
  display_order INT DEFAULT 100,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  INDEX idx_type (account_type),
  INDEX idx_code (code),
  INDEX idx_active (is_active)
);

-- GL ACCOUNT CALCULATIONS TABLE (For complex accounts with multiple calculations)
CREATE TABLE IF NOT EXISTS gl_account_calculations (
  id INT PRIMARY KEY AUTO_INCREMENT,
  gl_account_id INT NOT NULL,
  calculation_name VARCHAR(255),
  calculation_type ENUM('SUM_FIELD', 'CUSTOM_QUERY', 'LOOKUP', 'PERCENTAGE', 'CONDITIONAL') NOT NULL,
  calculation_config LONGTEXT NOT NULL,
  weight DECIMAL(5,2) DEFAULT 1.0,
  operator ENUM('+', '-', '*', '/') DEFAULT '+',
  sort_order INT DEFAULT 100,
  is_active BOOLEAN DEFAULT TRUE,
  
  FOREIGN KEY (gl_account_id) REFERENCES gl_accounts(id) ON DELETE CASCADE,
  INDEX idx_gl_account (gl_account_id)
);

-- GL MANUAL ENTRIES TABLE (Treasurer adjustments)
CREATE TABLE IF NOT EXISTS gl_manual_entries (
  id INT PRIMARY KEY AUTO_INCREMENT,
  gl_account_id INT NOT NULL,
  entry_date DATE NOT NULL,
  description VARCHAR(500),
  amount DECIMAL(15,2) NOT NULL,
  is_debit BOOLEAN,
  entry_reason ENUM('ACCRUAL', 'ADJUSTMENT', 'ALLOCATION', 'RECLASSIFICATION') NOT NULL,
  approval_status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
  created_by_user_id INT NOT NULL,
  approved_by_user_id INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  approved_at TIMESTAMP,
  
  FOREIGN KEY (gl_account_id) REFERENCES gl_accounts(id) ON DELETE CASCADE,
  INDEX idx_date (entry_date),
  INDEX idx_account (gl_account_id),
  INDEX idx_status (approval_status),
  INDEX idx_created_by (created_by_user_id),
  INDEX idx_approved_by (approved_by_user_id)
);

-- GL CONFIGURATION HISTORY (Audit trail)
CREATE TABLE IF NOT EXISTS gl_account_audit (
  id INT PRIMARY KEY AUTO_INCREMENT,
  gl_account_id INT NOT NULL,
  changed_by_user_id INT NOT NULL,
  change_type ENUM('CREATE', 'UPDATE', 'DELETE', 'ACTIVATE', 'DEACTIVATE') NOT NULL,
  old_config LONGTEXT,
  new_config LONGTEXT,
  change_reason VARCHAR(500),
  changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  FOREIGN KEY (gl_account_id) REFERENCES gl_accounts(id) ON DELETE CASCADE,
  INDEX idx_account (gl_account_id),
  INDEX idx_date (changed_at),
  INDEX idx_changed_by (changed_by_user_id)
);
