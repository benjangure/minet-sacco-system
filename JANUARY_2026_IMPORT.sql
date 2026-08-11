-- ========================================
-- JANUARY 2026 IMPORT
-- Generated: 2026-08-07 15:08:26
-- ========================================

USE minetsacco;

SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;

-- ========================================
-- CLEAR FINANCIAL DATA
-- ========================================
DELETE FROM loan_repayments;
DELETE FROM loan_topup_history;
DELETE FROM transactions;

UPDATE loans SET 
  amount = 0,
  outstanding_balance = 0,
  interest_collected = 0,
  principal_repaid = 0,
  total_interest = 0,
  monthly_repayment = 0,
  total_repayable = 0;

UPDATE accounts SET balance = 0;

SELECT 'Financial data cleared' AS Status;

-- ========================================
-- IMPORT JANUARY 2026 DATA
-- ========================================

-- MBURU FREDRICK MAINA - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1709042.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MBURU FREDRICK MAINA'))
  AND a.account_type = 'SHARES';

-- MBURU FREDRICK MAINA - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 2629100,
  l.principal_repaid = 174450,
  l.interest_collected = 28036,
  l.amount = 2803550,
  l.original_principal = 2803550
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MBURU FREDRICK MAINA'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- WAITHAKA DAVID CHEGE - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 7560000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('WAITHAKA DAVID CHEGE'))
  AND a.account_type = 'SHARES';

-- NDUTHU GABRIEL MAHUGU - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1776000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('NDUTHU GABRIEL MAHUGU'))
  AND a.account_type = 'SHARES';

-- NDUTHU GABRIEL MAHUGU - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 4959895,
  l.principal_repaid = 76306,
  l.interest_collected = 50362,
  l.amount = 5036201,
  l.original_principal = 5036201
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('NDUTHU GABRIEL MAHUGU'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- ONSANDO JOSEPH - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 4353343.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('ONSANDO JOSEPH'))
  AND a.account_type = 'SHARES';

-- MUIRURI DAVID KAMAU - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1520500.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MUIRURI DAVID KAMAU'))
  AND a.account_type = 'SHARES';

-- MUIRURI DAVID KAMAU - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 3558333,
  l.principal_repaid = 58333,
  l.interest_collected = 36167,
  l.amount = 3616667,
  l.original_principal = 3616667
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MUIRURI DAVID KAMAU'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- ANASTASIA NYAMBURA KIMANI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 181000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('ANASTASIA NYAMBURA KIMANI'))
  AND a.account_type = 'SHARES';

-- GANGLA JOHN OTIENO - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1170000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('GANGLA JOHN OTIENO'))
  AND a.account_type = 'SHARES';

-- GANGLA JOHN OTIENO - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 527346,
  l.principal_repaid = 14649,
  l.interest_collected = 5420,
  l.amount = 541995,
  l.original_principal = 541995
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('GANGLA JOHN OTIENO'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- MAINA FRANCIS WACHIRA - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 20434186.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MAINA FRANCIS WACHIRA'))
  AND a.account_type = 'SHARES';

-- MUTHUI SAMMY - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 4960000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MUTHUI SAMMY'))
  AND a.account_type = 'SHARES';

-- MUTHUI SAMMY - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 5703323,
  l.principal_repaid = 196667,
  l.interest_collected = 59000,
  l.amount = 5899990,
  l.original_principal = 5899990
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MUTHUI SAMMY'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- ERIC RUGO MUGO - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 9453000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('ERIC RUGO MUGO'))
  AND a.account_type = 'SHARES';

-- NDERITU CAROLINE NJERI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 13987221.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('NDERITU CAROLINE NJERI'))
  AND a.account_type = 'SHARES';

-- GITONGA TOBIAS MUGENDI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 3455000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('GITONGA TOBIAS MUGENDI'))
  AND a.account_type = 'SHARES';

-- MBURU MONICA WAMBUI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 2365000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MBURU MONICA WAMBUI'))
  AND a.account_type = 'SHARES';

-- MBURU MONICA WAMBUI - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 2718750,
  l.principal_repaid = 93750,
  l.interest_collected = 28125,
  l.amount = 2812500,
  l.original_principal = 2812500
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MBURU MONICA WAMBUI'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- MACHARIA EDWIN MWANGI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 6055996.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MACHARIA EDWIN MWANGI'))
  AND a.account_type = 'SHARES';

-- NJERU WINCATE MUKAMI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1238000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('NJERU WINCATE MUKAMI'))
  AND a.account_type = 'SHARES';

-- NJERU WINCATE MUKAMI - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 1077550,
  l.principal_repaid = 56150,
  l.interest_collected = 11337,
  l.amount = 1133700,
  l.original_principal = 1133700
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('NJERU WINCATE MUKAMI'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- KEGODE EDWIN AGALOMBA - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 4145000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('KEGODE EDWIN AGALOMBA'))
  AND a.account_type = 'SHARES';

-- KEGODE EDWIN AGALOMBA - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 1825592,
  l.principal_repaid = 107388,
  l.interest_collected = 19330,
  l.amount = 1932980,
  l.original_principal = 1932980
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('KEGODE EDWIN AGALOMBA'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- THUMBI JUDE NDUNG'U - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1910000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('THUMBI JUDE NDUNG''U'))
  AND a.account_type = 'SHARES';

-- THUMBI JUDE NDUNG'U - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 1192634,
  l.principal_repaid = 63302,
  l.interest_collected = 12559,
  l.amount = 1255936,
  l.original_principal = 1255936
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('THUMBI JUDE NDUNG''U'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- KIMANI ROBERT MURIUKI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1825000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('KIMANI ROBERT MURIUKI'))
  AND a.account_type = 'SHARES';

-- MUTUNGA KATEE - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1360000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MUTUNGA KATEE'))
  AND a.account_type = 'SHARES';

-- MUTUNGA KATEE - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 232980,
  l.principal_repaid = 14561,
  l.interest_collected = 2475,
  l.amount = 247541,
  l.original_principal = 247541
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MUTUNGA KATEE'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- AIRO JOHN OYAMO - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 243000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('AIRO JOHN OYAMO'))
  AND a.account_type = 'SHARES';

-- AIRO JOHN OYAMO - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 641417,
  l.principal_repaid = 14917,
  l.interest_collected = 6563,
  l.amount = 656333,
  l.original_principal = 656333
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('AIRO JOHN OYAMO'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- AIRO JOHN OYAMO - Emergency Loan 1
UPDATE loans l
SET 
  l.outstanding_balance = 33750,
  l.principal_repaid = 3750,
  l.interest_collected = 375,
  l.amount = 37500,
  l.original_principal = 37500
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('AIRO JOHN OYAMO'))
      AND lp.name LIKE '%Emergency%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- MWANGI JAYNE NJERI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 298000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MWANGI JAYNE NJERI'))
  AND a.account_type = 'SHARES';

-- MWANGI JAYNE NJERI - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 241475,
  l.principal_repaid = 3773,
  l.interest_collected = 2452,
  l.amount = 245248,
  l.original_principal = 245248
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MWANGI JAYNE NJERI'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- MATIVO IRENE - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 601000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MATIVO IRENE'))
  AND a.account_type = 'SHARES';

-- MATIVO IRENE - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 888889,
  l.principal_repaid = 13889,
  l.interest_collected = 9028,
  l.amount = 902778,
  l.original_principal = 902778
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MATIVO IRENE'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- MUTURI FELISTA IGOKI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 775000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MUTURI FELISTA IGOKI'))
  AND a.account_type = 'SHARES';

-- KIMANI ALICE NJERI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 497000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('KIMANI ALICE NJERI'))
  AND a.account_type = 'SHARES';

-- ONANI MICHAEL OWALO - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 3820700.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('ONANI MICHAEL OWALO'))
  AND a.account_type = 'SHARES';

-- NAIVASHA JANE - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 933000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('NAIVASHA JANE'))
  AND a.account_type = 'SHARES';

-- NAIVASHA JANE - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 660000,
  l.principal_repaid = 20000,
  l.interest_collected = 6800,
  l.amount = 680000,
  l.original_principal = 680000
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('NAIVASHA JANE'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- NYAORO ANDREW OJUANG' - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 68000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('NYAORO ANDREW OJUANG'''))
  AND a.account_type = 'SHARES';

-- KARUKI ESTHER WAMUTIRA - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 275000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('KARUKI ESTHER WAMUTIRA'))
  AND a.account_type = 'SHARES';

-- KARUKI ESTHER WAMUTIRA - Emergency Loan 1
UPDATE loans l
SET 
  l.outstanding_balance = 100000,
  l.principal_repaid = 12500,
  l.interest_collected = 1125,
  l.amount = 112500,
  l.original_principal = 112500
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('KARUKI ESTHER WAMUTIRA'))
      AND lp.name LIKE '%Emergency%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- MWANGI LENET - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1004201.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MWANGI LENET'))
  AND a.account_type = 'SHARES';

-- WANJOHI IRENE WANJIKU - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 486050.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('WANJOHI IRENE WANJIKU'))
  AND a.account_type = 'SHARES';

-- WANJOHI IRENE WANJIKU - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 962501,
  l.principal_repaid = 26736,
  l.interest_collected = 9892,
  l.amount = 989237,
  l.original_principal = 989237
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('WANJOHI IRENE WANJIKU'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- PERTET ESTHER SHIKU - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 182000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('PERTET ESTHER SHIKU'))
  AND a.account_type = 'SHARES';

-- AWUOR EDNAH OKWIRI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 957745.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('AWUOR EDNAH OKWIRI'))
  AND a.account_type = 'SHARES';

-- AWUOR EDNAH OKWIRI - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 564819,
  l.principal_repaid = 19477,
  l.interest_collected = 5843,
  l.amount = 584295,
  l.original_principal = 584295
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('AWUOR EDNAH OKWIRI'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- BUNYALI JULIUS HABAKKUK - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 2190000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('BUNYALI JULIUS HABAKKUK'))
  AND a.account_type = 'SHARES';

-- BUNYALI JULIUS HABAKKUK - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 435000,
  l.principal_repaid = 15000,
  l.interest_collected = 4500,
  l.amount = 450000,
  l.original_principal = 450000
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('BUNYALI JULIUS HABAKKUK'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- ANGWENYI GLADYS KERUBO - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 316000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('ANGWENYI GLADYS KERUBO'))
  AND a.account_type = 'SHARES';

-- ANGWENYI GLADYS KERUBO - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 100000,
  l.principal_repaid = 16667,
  l.interest_collected = 1167,
  l.amount = 116667,
  l.original_principal = 116667
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('ANGWENYI GLADYS KERUBO'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- IRENE MUTHONI MWANGI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 309000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('IRENE MUTHONI MWANGI'))
  AND a.account_type = 'SHARES';

-- IRENE MUTHONI MWANGI - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 751619,
  l.principal_repaid = 11744,
  l.interest_collected = 7634,
  l.amount = 763363,
  l.original_principal = 763363
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('IRENE MUTHONI MWANGI'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- GENSON NJUE MBAE - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 620000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('GENSON NJUE MBAE'))
  AND a.account_type = 'SHARES';

-- GENSON NJUE MBAE - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 1673063,
  l.principal_repaid = 25349,
  l.interest_collected = 16984,
  l.amount = 1698412,
  l.original_principal = 1698412
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('GENSON NJUE MBAE'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- LIDIA AKELO - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 281000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('LIDIA AKELO'))
  AND a.account_type = 'SHARES';

-- LIDIA AKELO - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 247626,
  l.principal_repaid = 11256,
  l.interest_collected = 2589,
  l.amount = 258882,
  l.original_principal = 258882
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('LIDIA AKELO'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- BERITA JUDY MUMBE - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 399000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('BERITA JUDY MUMBE'))
  AND a.account_type = 'SHARES';

-- BERITA JUDY MUMBE - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 182000,
  l.principal_repaid = 7000,
  l.interest_collected = 1890,
  l.amount = 189000,
  l.original_principal = 189000
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('BERITA JUDY MUMBE'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- CAROLINE KENDI ITONGA - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1051000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('CAROLINE KENDI ITONGA'))
  AND a.account_type = 'SHARES';

-- EVELYN NYAMBURA KIHARA - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 419000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('EVELYN NYAMBURA KIHARA'))
  AND a.account_type = 'SHARES';

-- MAKAU LYDIA RUGURU - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 214000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MAKAU LYDIA RUGURU'))
  AND a.account_type = 'SHARES';

-- MAKAU LYDIA RUGURU - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 186598,
  l.principal_repaid = 11662,
  l.interest_collected = 1983,
  l.amount = 198260,
  l.original_principal = 198260
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MAKAU LYDIA RUGURU'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- MAKAU LYDIA RUGURU - Emergency Loan 1
UPDATE loans l
SET 
  l.outstanding_balance = 8334,
  l.principal_repaid = 833,
  l.interest_collected = 92,
  l.amount = 9167,
  l.original_principal = 9167
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MAKAU LYDIA RUGURU'))
      AND lp.name LIKE '%Emergency%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- JANET NJERI NDUNGU - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 594000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('JANET NJERI NDUNGU'))
  AND a.account_type = 'SHARES';

-- IGNATIUS SHISOKA MUYONGA - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 62000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('IGNATIUS SHISOKA MUYONGA'))
  AND a.account_type = 'SHARES';

-- JOYCE NJOKI MUYA - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 580000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('JOYCE NJOKI MUYA'))
  AND a.account_type = 'SHARES';

-- ROSE WANJA KINYATI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 261000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('ROSE WANJA KINYATI'))
  AND a.account_type = 'SHARES';

-- SERAPHINE ANYANGA OKUMU - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 318000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('SERAPHINE ANYANGA OKUMU'))
  AND a.account_type = 'SHARES';

-- NICKSON MASITA ONG'ERA - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 599000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('NICKSON MASITA ONG''ERA'))
  AND a.account_type = 'SHARES';

-- NICKSON MASITA ONG'ERA - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 1458333,
  l.principal_repaid = 20833,
  l.interest_collected = 14792,
  l.amount = 1479167,
  l.original_principal = 1479167
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('NICKSON MASITA ONG''ERA'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- KABUGI ANNE JATIAGA - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1160000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('KABUGI ANNE JATIAGA'))
  AND a.account_type = 'SHARES';

-- KABUGI ANNE JATIAGA - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 1312274,
  l.principal_repaid = 41009,
  l.interest_collected = 13533,
  l.amount = 1353282,
  l.original_principal = 1353282
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('KABUGI ANNE JATIAGA'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- OBONYO SYDNEY MATHEW - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 724500.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('OBONYO SYDNEY MATHEW'))
  AND a.account_type = 'SHARES';

-- OBONYO SYDNEY MATHEW - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 1193334,
  l.principal_repaid = 16667,
  l.interest_collected = 12100,
  l.amount = 1210000,
  l.original_principal = 1210000
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('OBONYO SYDNEY MATHEW'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- MACHARIA RUTH WAITHIRA - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 312000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MACHARIA RUTH WAITHIRA'))
  AND a.account_type = 'SHARES';

-- MUSAO LEONARD OKUMU - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 938000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MUSAO LEONARD OKUMU'))
  AND a.account_type = 'SHARES';

-- MUSAO LEONARD OKUMU - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 2372409,
  l.principal_repaid = 35517,
  l.interest_collected = 24079,
  l.amount = 2407926,
  l.original_principal = 2407926
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MUSAO LEONARD OKUMU'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- MWAURA HANNAH NYAMBURA - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 800000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MWAURA HANNAH NYAMBURA'))
  AND a.account_type = 'SHARES';

-- KIMANI GLADYS WANGARI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 235000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('KIMANI GLADYS WANGARI'))
  AND a.account_type = 'SHARES';

-- KIMANI GLADYS WANGARI - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 303889,
  l.principal_repaid = 4444,
  l.interest_collected = 3083,
  l.amount = 308334,
  l.original_principal = 308334
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('KIMANI GLADYS WANGARI'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- MUTHAURA ROSE WANJA - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1084333.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MUTHAURA ROSE WANJA'))
  AND a.account_type = 'SHARES';

-- MUTHAURA ROSE WANJA - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 751667,
  l.principal_repaid = 26667,
  l.interest_collected = 7783,
  l.amount = 778333,
  l.original_principal = 778333
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MUTHAURA ROSE WANJA'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- ESTHER WANJIKU WACHIRA - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 865000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('ESTHER WANJIKU WACHIRA'))
  AND a.account_type = 'SHARES';

-- ESTHER WANJIKU WACHIRA - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 1418155,
  l.principal_repaid = 36490,
  l.interest_collected = 14546,
  l.amount = 1454645,
  l.original_principal = 1454645
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('ESTHER WANJIKU WACHIRA'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- AMASA FRANKLIN KIVARA - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 177000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('AMASA FRANKLIN KIVARA'))
  AND a.account_type = 'SHARES';

-- AMASA FRANKLIN KIVARA - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 115780,
  l.principal_repaid = 6811,
  l.interest_collected = 1226,
  l.amount = 122591,
  l.original_principal = 122591
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('AMASA FRANKLIN KIVARA'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- ABDALLA ABDULMAJID MBARUK - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1090000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('ABDALLA ABDULMAJID MBARUK'))
  AND a.account_type = 'SHARES';

-- MWAKIO JEFFERSON MWAINGE - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 979500.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MWAKIO JEFFERSON MWAINGE'))
  AND a.account_type = 'SHARES';

-- MWAKIO JEFFERSON MWAINGE - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 30000,
  l.principal_repaid = 5000,
  l.interest_collected = 350,
  l.amount = 35000,
  l.original_principal = 35000
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MWAKIO JEFFERSON MWAINGE'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- MACHI IRENE KITAWA - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 738000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MACHI IRENE KITAWA'))
  AND a.account_type = 'SHARES';

-- KING'ARA LUCY WANJIRU - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1109000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('KING''ARA LUCY WANJIRU'))
  AND a.account_type = 'SHARES';

-- KING'ARA LUCY WANJIRU - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 498599,
  l.principal_repaid = 62325,
  l.interest_collected = 5609,
  l.amount = 560923,
  l.original_principal = 560923
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('KING''ARA LUCY WANJIRU'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- AWITI GRACE LYNETTE - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 378000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('AWITI GRACE LYNETTE'))
  AND a.account_type = 'SHARES';

-- Migwi CHARLOTTE WANJIKU - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1042400.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Migwi CHARLOTTE WANJIKU'))
  AND a.account_type = 'SHARES';

-- Migwi CHARLOTTE WANJIKU - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 940000,
  l.principal_repaid = 20000,
  l.interest_collected = 9600,
  l.amount = 960000,
  l.original_principal = 960000
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Migwi CHARLOTTE WANJIKU'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- ADELE MACHARIA LINDA WANGUI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 483000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('ADELE MACHARIA LINDA WANGUI'))
  AND a.account_type = 'SHARES';

-- NGURE RACHAEL WANJIKU - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 434000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('NGURE RACHAEL WANJIKU'))
  AND a.account_type = 'SHARES';

-- NGURE RACHAEL WANJIKU - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 1168210,
  l.principal_repaid = 18253,
  l.interest_collected = 11865,
  l.amount = 1186463,
  l.original_principal = 1186463
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('NGURE RACHAEL WANJIKU'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- KINOTI LINDA GATWIRI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 532000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('KINOTI LINDA GATWIRI'))
  AND a.account_type = 'SHARES';

-- KINOTI LINDA GATWIRI - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 210833,
  l.principal_repaid = 4583,
  l.interest_collected = 2154,
  l.amount = 215417,
  l.original_principal = 215417
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('KINOTI LINDA GATWIRI'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- ATIENO NAOMI JUDITH - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 746000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('ATIENO NAOMI JUDITH'))
  AND a.account_type = 'SHARES';

-- ATIENO NAOMI JUDITH - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 1263239,
  l.principal_repaid = 28072,
  l.interest_collected = 12913,
  l.amount = 1291311,
  l.original_principal = 1291311
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('ATIENO NAOMI JUDITH'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- GICHURI JERIOTH MUTHONI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 869000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('GICHURI JERIOTH MUTHONI'))
  AND a.account_type = 'SHARES';

-- GICHURI JERIOTH MUTHONI - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 398576,
  l.principal_repaid = 46508,
  l.interest_collected = 4451,
  l.amount = 445084,
  l.original_principal = 445084
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('GICHURI JERIOTH MUTHONI'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- NDUNDA PERPETUA WANZA - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1404000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('NDUNDA PERPETUA WANZA'))
  AND a.account_type = 'SHARES';

-- NDUNDA PERPETUA WANZA - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 1440000,
  l.principal_repaid = 30000,
  l.interest_collected = 14700,
  l.amount = 1470000,
  l.original_principal = 1470000
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('NDUNDA PERPETUA WANZA'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- WERE LORINE AKOTH - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 504000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('WERE LORINE AKOTH'))
  AND a.account_type = 'SHARES';

-- WERE LORINE AKOTH - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 395761,
  l.principal_repaid = 12766,
  l.interest_collected = 4085,
  l.amount = 408527,
  l.original_principal = 408527
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('WERE LORINE AKOTH'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- WERE LORINE AKOTH - Emergency Loan 1
UPDATE loans l
SET 
  l.outstanding_balance = 110000,
  l.principal_repaid = 10000,
  l.interest_collected = 1200,
  l.amount = 120000,
  l.original_principal = 120000
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('WERE LORINE AKOTH'))
      AND lp.name LIKE '%Emergency%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- MATALANGA JOHN KAMAU - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 748250.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MATALANGA JOHN KAMAU'))
  AND a.account_type = 'SHARES';

-- ALWODI MARK MUNAVI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 251000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('ALWODI MARK MUNAVI'))
  AND a.account_type = 'SHARES';

-- ALWODI MARK MUNAVI - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 570000,
  l.principal_repaid = 10000,
  l.interest_collected = 5800,
  l.amount = 580000,
  l.original_principal = 580000
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('ALWODI MARK MUNAVI'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- RAPONGO COLLINS BWIRE - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 255000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('RAPONGO COLLINS BWIRE'))
  AND a.account_type = 'SHARES';

-- MARY MUTINDI KIMANTHI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 613000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MARY MUTINDI KIMANTHI'))
  AND a.account_type = 'SHARES';

-- MARY MUTINDI KIMANTHI - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 930366,
  l.principal_repaid = 39390,
  l.interest_collected = 9698,
  l.amount = 969756,
  l.original_principal = 969756
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MARY MUTINDI KIMANTHI'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- FLORENCE MWENDE WAMBU - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 217000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('FLORENCE MWENDE WAMBU'))
  AND a.account_type = 'SHARES';

-- CHEGE SAMUEL KARARI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1644025.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('CHEGE SAMUEL KARARI'))
  AND a.account_type = 'SHARES';

-- CHEGE SAMUEL KARARI - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 2619668,
  l.principal_repaid = 41582,
  l.interest_collected = 26613,
  l.amount = 2661250,
  l.original_principal = 2661250
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('CHEGE SAMUEL KARARI'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- JOYCE GATWIRI KITHINJI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 308000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('JOYCE GATWIRI KITHINJI'))
  AND a.account_type = 'SHARES';

-- MAINGA DANIEL LOTI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 4124000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MAINGA DANIEL LOTI'))
  AND a.account_type = 'SHARES';

-- MAINGA DANIEL LOTI - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 2212500,
  l.principal_repaid = 122917,
  l.interest_collected = 23354,
  l.amount = 2335417,
  l.original_principal = 2335417
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MAINGA DANIEL LOTI'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- ROTICH ROBERT ALEX - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 6038400.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('ROTICH ROBERT ALEX'))
  AND a.account_type = 'SHARES';

-- CHRISTINE C MUTHONI MURIITHI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 300500.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('CHRISTINE C MUTHONI MURIITHI'))
  AND a.account_type = 'SHARES';

-- GITAU MARY WAIRIMU - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1183000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('GITAU MARY WAIRIMU'))
  AND a.account_type = 'SHARES';

-- GITAU MARY WAIRIMU - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 3483562,
  l.principal_repaid = 51993,
  l.interest_collected = 35356,
  l.amount = 3535556,
  l.original_principal = 3535556
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('GITAU MARY WAIRIMU'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- MAINA STEPHEN IRUNGU - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1111000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MAINA STEPHEN IRUNGU'))
  AND a.account_type = 'SHARES';

-- MAINA STEPHEN IRUNGU - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 750225,
  l.principal_repaid = 41679,
  l.interest_collected = 7919,
  l.amount = 791904,
  l.original_principal = 791904
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MAINA STEPHEN IRUNGU'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- BUURI PAMELA MWENDE - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 255000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('BUURI PAMELA MWENDE'))
  AND a.account_type = 'SHARES';

-- MURIITHI ROSE WANGARI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 664000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MURIITHI ROSE WANGARI'))
  AND a.account_type = 'SHARES';

-- MURIITHI ROSE WANGARI - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 53333,
  l.principal_repaid = 6667,
  l.interest_collected = 600,
  l.amount = 60000,
  l.original_principal = 60000
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MURIITHI ROSE WANGARI'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- NARANGWI PETER KIBUINE - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 11919732.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('NARANGWI PETER KIBUINE'))
  AND a.account_type = 'SHARES';

-- MICHAEL NG'ANG'A KAMAU - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 50000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MICHAEL NG''ANG''A KAMAU'))
  AND a.account_type = 'SHARES';

-- KINYUA FRIDAH NAITORE - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1202000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('KINYUA FRIDAH NAITORE'))
  AND a.account_type = 'SHARES';

-- DENIS MOSE TANGASO - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 172000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('DENIS MOSE TANGASO'))
  AND a.account_type = 'SHARES';

-- DENIS MOSE TANGASO - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 489439,
  l.principal_repaid = 13228,
  l.interest_collected = 5027,
  l.amount = 502667,
  l.original_principal = 502667
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('DENIS MOSE TANGASO'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- TEDDY AYODI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 9770000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('TEDDY AYODI'))
  AND a.account_type = 'SHARES';

-- MWAGI JOSEPH ONYANGO - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1816000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MWAGI JOSEPH ONYANGO'))
  AND a.account_type = 'SHARES';

-- CHARLES NJUNGE MWAURA - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 480000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('CHARLES NJUNGE MWAURA'))
  AND a.account_type = 'SHARES';

-- CHARLES NJUNGE MWAURA - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 316856,
  l.principal_repaid = 11735,
  l.interest_collected = 3286,
  l.amount = 328592,
  l.original_principal = 328592
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('CHARLES NJUNGE MWAURA'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- PATRICK MULI KALUMBA - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 989500.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('PATRICK MULI KALUMBA'))
  AND a.account_type = 'SHARES';

-- PATRICK MULI KALUMBA - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 1260208,
  l.principal_repaid = 67311,
  l.interest_collected = 13275,
  l.amount = 1327519,
  l.original_principal = 1327519
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('PATRICK MULI KALUMBA'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- MARCOS OTIENO ANYUMBA - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1123000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MARCOS OTIENO ANYUMBA'))
  AND a.account_type = 'SHARES';

-- MARCOS OTIENO ANYUMBA - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 1694444,
  l.principal_repaid = 27778,
  l.interest_collected = 17222,
  l.amount = 1722222,
  l.original_principal = 1722222
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MARCOS OTIENO ANYUMBA'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- NALWENJE ALEX OKOTH - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 78000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('NALWENJE ALEX OKOTH'))
  AND a.account_type = 'SHARES';

-- LUNG'ATSO ASIYA ESIEMINYI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 268000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('LUNG''ATSO ASIYA ESIEMINYI'))
  AND a.account_type = 'SHARES';

-- LUNG'ATSO ASIYA ESIEMINYI - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 261790,
  l.principal_repaid = 8109,
  l.interest_collected = 2699,
  l.amount = 269899,
  l.original_principal = 269899
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('LUNG''ATSO ASIYA ESIEMINYI'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- EUNICE WAMBUI NJOGU - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 252500.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('EUNICE WAMBUI NJOGU'))
  AND a.account_type = 'SHARES';

-- Mr Simeon Odhiambo Owino - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 158000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Simeon Odhiambo Owino'))
  AND a.account_type = 'SHARES';

-- Mr Andrew Gitau Githua Kaminja - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 147000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Andrew Gitau Githua Kaminja'))
  AND a.account_type = 'SHARES';

-- Mr Andrew Gitau Githua Kaminja - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 110833,
  l.principal_repaid = 2917,
  l.interest_collected = 1137,
  l.amount = 113750,
  l.original_principal = 113750
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Andrew Gitau Githua Kaminja'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Ms Ann Wandia Wangu - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 103000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Ms Ann Wandia Wangu'))
  AND a.account_type = 'SHARES';

-- Ms Ann Wandia Wangu - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 226667,
  l.principal_repaid = 3333,
  l.interest_collected = 2300,
  l.amount = 230000,
  l.original_principal = 230000
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Ms Ann Wandia Wangu'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Mr Edwin Were Saaya - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 550000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Edwin Were Saaya'))
  AND a.account_type = 'SHARES';

-- Mr Edwin Were Saaya - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 600000,
  l.principal_repaid = 12500,
  l.interest_collected = 6125,
  l.amount = 612500,
  l.original_principal = 612500
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Edwin Were Saaya'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Mr Kelvin Mulinge Kyalo - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 144000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Kelvin Mulinge Kyalo'))
  AND a.account_type = 'SHARES';

-- Ms Claire Wairimu Kinyanjui - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 180000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Ms Claire Wairimu Kinyanjui'))
  AND a.account_type = 'SHARES';

-- TITUS THUMBI MURIUKI - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 655000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('TITUS THUMBI MURIUKI'))
  AND a.account_type = 'SHARES';

-- JUWEIRIYA ABDALLLA - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 275000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('JUWEIRIYA ABDALLLA'))
  AND a.account_type = 'SHARES';

-- MOSES KURIA - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 423000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MOSES KURIA'))
  AND a.account_type = 'SHARES';

-- MOSES KURIA - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 1145537,
  l.principal_repaid = 18183,
  l.interest_collected = 11637,
  l.amount = 1163720,
  l.original_principal = 1163720
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('MOSES KURIA'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Mr George Mbugua Kariuki - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 320000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr George Mbugua Kariuki'))
  AND a.account_type = 'SHARES';

-- Mr George Mbugua Kariuki - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 850000,
  l.principal_repaid = 12500,
  l.interest_collected = 8625,
  l.amount = 862500,
  l.original_principal = 862500
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr George Mbugua Kariuki'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Ms Caroline Nekesa - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 320000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Ms Caroline Nekesa'))
  AND a.account_type = 'SHARES';

-- Ms Teresia Trizer Wamucii - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 335000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Ms Teresia Trizer Wamucii'))
  AND a.account_type = 'SHARES';

-- Peter Wanjohi Maina - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1435000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Peter Wanjohi Maina'))
  AND a.account_type = 'SHARES';

-- Peter Wanjohi Maina - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 433333,
  l.principal_repaid = 16667,
  l.interest_collected = 4500,
  l.amount = 450000,
  l.original_principal = 450000
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Peter Wanjohi Maina'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Ms Mercy Gathoni Muriithi - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 93000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Ms Mercy Gathoni Muriithi'))
  AND a.account_type = 'SHARES';

-- Mercy Muringi Muthoga - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 135000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mercy Muringi Muthoga'))
  AND a.account_type = 'SHARES';

-- Mr Peter Mwiti Kamundi - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 96500.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Peter Mwiti Kamundi'))
  AND a.account_type = 'SHARES';

-- Mr Peter Mwiti Kamundi - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 269471,
  l.principal_repaid = 4491,
  l.interest_collected = 2740,
  l.amount = 273963,
  l.original_principal = 273963
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Peter Mwiti Kamundi'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Winrose Miroyo Ondego - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 47000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Winrose Miroyo Ondego'))
  AND a.account_type = 'SHARES';

-- Mr Stephen Ngugi Muriu - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 553000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Stephen Ngugi Muriu'))
  AND a.account_type = 'SHARES';

-- Mr Stephen Ngugi Muriu - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 1364000,
  l.principal_repaid = 44000,
  l.interest_collected = 14080,
  l.amount = 1408000,
  l.original_principal = 1408000
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Stephen Ngugi Muriu'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Mr Eric Atinda Orina - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 82000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Eric Atinda Orina'))
  AND a.account_type = 'SHARES';

-- Mr Eric Atinda Orina - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 95407,
  l.principal_repaid = 2327,
  l.interest_collected = 977,
  l.amount = 97734,
  l.original_principal = 97734
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Eric Atinda Orina'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Ms Jane Gakii Miriti - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 78000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Ms Jane Gakii Miriti'))
  AND a.account_type = 'SHARES';

-- Ms Jane Gakii Miriti - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 87744,
  l.principal_repaid = 2438,
  l.interest_collected = 902,
  l.amount = 90182,
  l.original_principal = 90182
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Ms Jane Gakii Miriti'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Ms Elizabeth Nekesa Sitati - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 209000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Ms Elizabeth Nekesa Sitati'))
  AND a.account_type = 'SHARES';

-- Ms Elizabeth Nekesa Sitati - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 236250,
  l.principal_repaid = 5250,
  l.interest_collected = 2415,
  l.amount = 241500,
  l.original_principal = 241500
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Ms Elizabeth Nekesa Sitati'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Pauline Waithira Mwaura - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 175000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Pauline Waithira Mwaura'))
  AND a.account_type = 'SHARES';

-- Kenedy Kirimi Mati - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 115000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Kenedy Kirimi Mati'))
  AND a.account_type = 'SHARES';

-- Gibson Oguda Mbaja - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 63000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Gibson Oguda Mbaja'))
  AND a.account_type = 'SHARES';

-- Mr James Maina Kimani - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 427000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr James Maina Kimani'))
  AND a.account_type = 'SHARES';

-- Mr James Maina Kimani - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 391667,
  l.principal_repaid = 8333,
  l.interest_collected = 4000,
  l.amount = 400000,
  l.original_principal = 400000
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr James Maina Kimani'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Faith Atieno Nyaoro - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 47000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Faith Atieno Nyaoro'))
  AND a.account_type = 'SHARES';

-- Faith Atieno Nyaoro - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 33333,
  l.principal_repaid = 2222,
  l.interest_collected = 356,
  l.amount = 35556,
  l.original_principal = 35556
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Faith Atieno Nyaoro'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Mr Stephen Ndavuti Ndunda - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 9000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Stephen Ndavuti Ndunda'))
  AND a.account_type = 'SHARES';

-- Jorim Ochieng Awuor - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 63000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Jorim Ochieng Awuor'))
  AND a.account_type = 'SHARES';

-- Jorim Ochieng Awuor - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 176431,
  l.principal_repaid = 2846,
  l.interest_collected = 1793,
  l.amount = 179277,
  l.original_principal = 179277
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Jorim Ochieng Awuor'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Mr Joram Katiwa Mutunga - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 215000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Joram Katiwa Mutunga'))
  AND a.account_type = 'SHARES';

-- Mr Joram Katiwa Mutunga - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 104683,
  l.principal_repaid = 1662,
  l.interest_collected = 1063,
  l.amount = 106345,
  l.original_principal = 106345
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Joram Katiwa Mutunga'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Mr Walter Kipkurui Koech - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 33000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Walter Kipkurui Koech'))
  AND a.account_type = 'SHARES';

-- Mr Walter Kipkurui Koech - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 37778,
  l.principal_repaid = 556,
  l.interest_collected = 383,
  l.amount = 38333,
  l.original_principal = 38333
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Walter Kipkurui Koech'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Mr Andrew Muhia Kagiri - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 253000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Andrew Muhia Kagiri'))
  AND a.account_type = 'SHARES';

-- Mr Andrew Muhia Kagiri - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 537463,
  l.principal_repaid = 13109,
  l.interest_collected = 5506,
  l.amount = 550572,
  l.original_principal = 550572
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Andrew Muhia Kagiri'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Mr Dennis William Mukwanja - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 60000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Dennis William Mukwanja'))
  AND a.account_type = 'SHARES';

-- Mr Dennis William Mukwanja - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 30000,
  l.principal_repaid = 1667,
  l.interest_collected = 317,
  l.amount = 31667,
  l.original_principal = 31667
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Dennis William Mukwanja'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Miss Dorcas Wanjugu Maina - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 57000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Miss Dorcas Wanjugu Maina'))
  AND a.account_type = 'SHARES';

-- Kevin Njenga Nguyai - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 60000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Kevin Njenga Nguyai'))
  AND a.account_type = 'SHARES';

-- Kevin Njenga Nguyai - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 58333,
  l.principal_repaid = 8333,
  l.interest_collected = 667,
  l.amount = 66667,
  l.original_principal = 66667
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Kevin Njenga Nguyai'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Ms Naomi Wangui Nganga - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 103000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Ms Naomi Wangui Nganga'))
  AND a.account_type = 'SHARES';

-- Ms Naomi Wangui Nganga - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 197046,
  l.principal_repaid = 4318,
  l.interest_collected = 2014,
  l.amount = 201364,
  l.original_principal = 201364
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Ms Naomi Wangui Nganga'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Mrs Elizabeth Muthoni Karanja - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 60000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mrs Elizabeth Muthoni Karanja'))
  AND a.account_type = 'SHARES';

-- Ms Doreen Muthoni Mpangua - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 63000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Ms Doreen Muthoni Mpangua'))
  AND a.account_type = 'SHARES';

-- Glenn Runanu Kabiru - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 134000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Glenn Runanu Kabiru'))
  AND a.account_type = 'SHARES';

-- Glenn Runanu Kabiru - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 306710,
  l.principal_repaid = 4565,
  l.interest_collected = 3113,
  l.amount = 311275,
  l.original_principal = 311275
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Glenn Runanu Kabiru'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Glenn Runanu Kabiru - Emergency Loan 1
UPDATE loans l
SET 
  l.outstanding_balance = 8333,
  l.principal_repaid = 833,
  l.interest_collected = 92,
  l.amount = 9167,
  l.original_principal = 9167
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Glenn Runanu Kabiru'))
      AND lp.name LIKE '%Emergency%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Mr Benson Waweru Muriuki - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 86500.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Benson Waweru Muriuki'))
  AND a.account_type = 'SHARES';

-- Mr Benson Waweru Muriuki - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 129854,
  l.principal_repaid = 1882,
  l.interest_collected = 1317,
  l.amount = 131736,
  l.original_principal = 131736
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Benson Waweru Muriuki'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Joshua Wafula Kakai - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 90000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Joshua Wafula Kakai'))
  AND a.account_type = 'SHARES';

-- Mr Reynold Onyango Oketch - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 54000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Reynold Onyango Oketch'))
  AND a.account_type = 'SHARES';

-- Felix Ochieng Otieno - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 51000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Felix Ochieng Otieno'))
  AND a.account_type = 'SHARES';

-- Mr Anthony Musembi Ndambuki - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 170000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Anthony Musembi Ndambuki'))
  AND a.account_type = 'SHARES';

-- Mr Edwin Kimathi Mwenda - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 539000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Edwin Kimathi Mwenda'))
  AND a.account_type = 'SHARES';

-- Mr Edwin Kimathi Mwenda - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 1200000,
  l.principal_repaid = 25000,
  l.interest_collected = 12250,
  l.amount = 1225000,
  l.original_principal = 1225000
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Edwin Kimathi Mwenda'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Edward  Jenings Koganga - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 60000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Edward  Jenings Koganga'))
  AND a.account_type = 'SHARES';

-- Peter Mwangi Maina - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 45000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Peter Mwangi Maina'))
  AND a.account_type = 'SHARES';

-- Peter Mwangi Maina - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 49420,
  l.principal_repaid = 2471,
  l.interest_collected = 519,
  l.amount = 51891,
  l.original_principal = 51891
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Peter Mwangi Maina'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Mrs Rachel Nkatha Mwenda - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 130000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mrs Rachel Nkatha Mwenda'))
  AND a.account_type = 'SHARES';

-- Mrs Rachel Nkatha Mwenda - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 262500,
  l.principal_repaid = 12500,
  l.interest_collected = 2750,
  l.amount = 275000,
  l.original_principal = 275000
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mrs Rachel Nkatha Mwenda'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Joseph Oduory Ouma - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 120000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Joseph Oduory Ouma'))
  AND a.account_type = 'SHARES';

-- Evans Kipkorir Yegon - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 72000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Evans Kipkorir Yegon'))
  AND a.account_type = 'SHARES';

-- Miss Grace Wanjiru Muthii - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 66000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Miss Grace Wanjiru Muthii'))
  AND a.account_type = 'SHARES';

-- Miss Grace Wanjiru Muthii - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 155050,
  l.principal_repaid = 2501,
  l.interest_collected = 1576,
  l.amount = 157551,
  l.original_principal = 157551
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Miss Grace Wanjiru Muthii'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Margaret Wanini Munji - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 90000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Margaret Wanini Munji'))
  AND a.account_type = 'SHARES';

-- Mr Japheth Kanyoo Matheka - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 85500.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Japheth Kanyoo Matheka'))
  AND a.account_type = 'SHARES';

-- Mr Japheth Kanyoo Matheka - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 130000,
  l.principal_repaid = 10000,
  l.interest_collected = 1400,
  l.amount = 140000,
  l.original_principal = 140000
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Japheth Kanyoo Matheka'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Maria Kalumu Isaac - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 33000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Maria Kalumu Isaac'))
  AND a.account_type = 'SHARES';

-- Mercy Mwende Kitivo - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 50000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mercy Mwende Kitivo'))
  AND a.account_type = 'SHARES';

-- Mr Robson Chege Mburu - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 72000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Robson Chege Mburu'))
  AND a.account_type = 'SHARES';

-- Mr Robson Chege Mburu - Emergency Loan 1
UPDATE loans l
SET 
  l.outstanding_balance = 22500,
  l.principal_repaid = 2500,
  l.interest_collected = 250,
  l.amount = 25000,
  l.original_principal = 25000
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Robson Chege Mburu'))
      AND lp.name LIKE '%Emergency%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Miss Josephine Kavata Kioko - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 101000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Miss Josephine Kavata Kioko'))
  AND a.account_type = 'SHARES';

-- Ms Cecelina Gacheri Mwobobia - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 690000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Ms Cecelina Gacheri Mwobobia'))
  AND a.account_type = 'SHARES';

-- Daniel Muasya Muasa - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 35000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Daniel Muasya Muasa'))
  AND a.account_type = 'SHARES';

-- Alex Wamae Gatua - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 18000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Alex Wamae Gatua'))
  AND a.account_type = 'SHARES';

-- Levis Mwaniki Gaitho - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 58000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Levis Mwaniki Gaitho'))
  AND a.account_type = 'SHARES';

-- Levis Mwaniki Gaitho - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 167639,
  l.principal_repaid = 2361,
  l.interest_collected = 1700,
  l.amount = 170000,
  l.original_principal = 170000
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Levis Mwaniki Gaitho'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Mr Alex Njenga Mwai - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 59500.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Alex Njenga Mwai'))
  AND a.account_type = 'SHARES';

-- Joseph Gituma - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 150000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Joseph Gituma'))
  AND a.account_type = 'SHARES';

-- Mr Joseph Muigai Wainaina - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 438000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Joseph Muigai Wainaina'))
  AND a.account_type = 'SHARES';

-- Cyrus Mwaura Njoroge - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 50000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Cyrus Mwaura Njoroge'))
  AND a.account_type = 'SHARES';

-- Ms Hadija Duba - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 15000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Ms Hadija Duba'))
  AND a.account_type = 'SHARES';

-- Mwihaki Kabura - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 50000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mwihaki Kabura'))
  AND a.account_type = 'SHARES';

-- Miss Jackline Mwihaki Makumi - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 12000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Miss Jackline Mwihaki Makumi'))
  AND a.account_type = 'SHARES';

-- Abigail Sakini Wabwoba - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 20000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Abigail Sakini Wabwoba'))
  AND a.account_type = 'SHARES';

-- Benard Muthiani Kasuni - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 20000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Benard Muthiani Kasuni'))
  AND a.account_type = 'SHARES';

-- Mr Gideon Kipkirui Bii - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 103000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Gideon Kipkirui Bii'))
  AND a.account_type = 'SHARES';

-- Loice Buyaki Momanyi - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 40000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Loice Buyaki Momanyi'))
  AND a.account_type = 'SHARES';

-- Mr Alvin Mukubwa Kituyi - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 9000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Alvin Mukubwa Kituyi'))
  AND a.account_type = 'SHARES';

-- Mr Alvin Mukubwa Kituyi - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = 55000,
  l.principal_repaid = 11000,
  l.interest_collected = 660,
  l.amount = 66000,
  l.original_principal = 66000
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Alvin Mukubwa Kituyi'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

-- Lucy Njeri Njroge - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 9000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Lucy Njeri Njroge'))
  AND a.account_type = 'SHARES';

-- Miss Ann Maureen Kendi Murithi - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 12000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Miss Ann Maureen Kendi Murithi'))
  AND a.account_type = 'SHARES';

-- Mr Eric Stanley Ng'ethe - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 31079.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Mr Eric Stanley Ng''ethe'))
  AND a.account_type = 'SHARES';

-- Cynthia Jerobon Kiptanui - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 20000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('Cynthia Jerobon Kiptanui'))
  AND a.account_type = 'SHARES';

-- David Makuto Mmata - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 10000.00
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('David Makuto Mmata'))
  AND a.account_type = 'SHARES';

SET FOREIGN_KEY_CHECKS = 1;
SET SQL_SAFE_UPDATES = 1;

-- ========================================
-- SUMMARY
-- ========================================
SELECT 'Import complete' AS Status;
SELECT SUM(balance) AS total_shares FROM accounts WHERE account_type = 'SHARES';
SELECT SUM(outstanding_balance) AS total_loans FROM loans WHERE status IN ('DISBURSED', 'ACTIVE');

