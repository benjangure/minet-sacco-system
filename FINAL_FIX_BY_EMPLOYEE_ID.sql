USE minetsacco;
SET SQL_SAFE_UPDATES = 0;

-- ========================================
-- FINAL FIX - EMPLOYEE ID MATCHING
-- ========================================

-- MBURU FREDRICK MAINA (Payroll: 1087) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1709042.00
WHERE m.employee_id = '1087'
  AND a.account_type = 'SHARES';

-- MBURU FREDRICK MAINA (Payroll: 1087) - Normal Loan
UPDATE loans 
SET 
  amount = 2803550,
  outstanding_balance = 2629100,
  principal_repaid = 174450,
  interest_collected = 28036,
  original_principal = 2803550
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1087')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- WAITHAKA DAVID CHEGE (Payroll: 1191) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 7560000.00
WHERE m.employee_id = '1191'
  AND a.account_type = 'SHARES';

-- NDUTHU GABRIEL MAHUGU (Payroll: 1242) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1776000.00
WHERE m.employee_id = '1242'
  AND a.account_type = 'SHARES';

-- NDUTHU GABRIEL MAHUGU (Payroll: 1242) - Normal Loan
UPDATE loans 
SET 
  amount = 5036201,
  outstanding_balance = 4959895,
  principal_repaid = 76306,
  interest_collected = 50362,
  original_principal = 5036201
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1242')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- ONSANDO JOSEPH (Payroll: 1214) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 4353343.00
WHERE m.employee_id = '1214'
  AND a.account_type = 'SHARES';

-- MUIRURI DAVID KAMAU (Payroll: 1297) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1520500.00
WHERE m.employee_id = '1297'
  AND a.account_type = 'SHARES';

-- MUIRURI DAVID KAMAU (Payroll: 1297) - Normal Loan
UPDATE loans 
SET 
  amount = 3616667,
  outstanding_balance = 3558333,
  principal_repaid = 58333,
  interest_collected = 36167,
  original_principal = 3616667
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1297')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- ANASTASIA NYAMBURA KIMANI (Payroll: 13118) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 181000.00
WHERE m.employee_id = '13118'
  AND a.account_type = 'SHARES';

-- GANGLA JOHN OTIENO (Payroll: 2054) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1170000.00
WHERE m.employee_id = '2054'
  AND a.account_type = 'SHARES';

-- GANGLA JOHN OTIENO (Payroll: 2054) - Normal Loan
UPDATE loans 
SET 
  amount = 541995,
  outstanding_balance = 527346,
  principal_repaid = 14649,
  interest_collected = 5420,
  original_principal = 541995
WHERE member_id = (SELECT id FROM members WHERE employee_id = '2054')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- MAINA FRANCIS WACHIRA (Payroll: 2076) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 20434186.00
WHERE m.employee_id = '2076'
  AND a.account_type = 'SHARES';

-- MUTHUI SAMMY (Payroll: 4044) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 4960000.00
WHERE m.employee_id = '4044'
  AND a.account_type = 'SHARES';

-- MUTHUI SAMMY (Payroll: 4044) - Normal Loan
UPDATE loans 
SET 
  amount = 5899990,
  outstanding_balance = 5703323,
  principal_repaid = 196667,
  interest_collected = 59000,
  original_principal = 5899990
WHERE member_id = (SELECT id FROM members WHERE employee_id = '4044')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- ERIC RUGO MUGO (Payroll: 5187) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 9453000.00
WHERE m.employee_id = '5187'
  AND a.account_type = 'SHARES';

-- NDERITU CAROLINE NJERI (Payroll: 6106) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 13987221.00
WHERE m.employee_id = '6106'
  AND a.account_type = 'SHARES';

-- GITONGA TOBIAS MUGENDI (Payroll: 7139) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 3455000.00
WHERE m.employee_id = '7139'
  AND a.account_type = 'SHARES';

-- MBURU MONICA WAMBUI (Payroll: 7110) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 2365000.00
WHERE m.employee_id = '7110'
  AND a.account_type = 'SHARES';

-- MBURU MONICA WAMBUI (Payroll: 7110) - Normal Loan
UPDATE loans 
SET 
  amount = 2812500,
  outstanding_balance = 2718750,
  principal_repaid = 93750,
  interest_collected = 28125,
  original_principal = 2812500
WHERE member_id = (SELECT id FROM members WHERE employee_id = '7110')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- MACHARIA EDWIN MWANGI (Payroll: 9080) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 6055996.00
WHERE m.employee_id = '9080'
  AND a.account_type = 'SHARES';

-- NJERU WINCATE MUKAMI (Payroll: 9150) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1238000.00
WHERE m.employee_id = '9150'
  AND a.account_type = 'SHARES';

-- NJERU WINCATE MUKAMI (Payroll: 9150) - Normal Loan
UPDATE loans 
SET 
  amount = 1133700,
  outstanding_balance = 1077550,
  principal_repaid = 56150,
  interest_collected = 11337,
  original_principal = 1133700
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9150')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- KEGODE EDWIN AGALOMBA (Payroll: 9132) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 4145000.00
WHERE m.employee_id = '9132'
  AND a.account_type = 'SHARES';

-- KEGODE EDWIN AGALOMBA (Payroll: 9132) - Normal Loan
UPDATE loans 
SET 
  amount = 1932980,
  outstanding_balance = 1825592,
  principal_repaid = 107388,
  interest_collected = 19330,
  original_principal = 1932980
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9132')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- THUMBI JUDE NDUNG'U (Payroll: 9113) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1910000.00
WHERE m.employee_id = '9113'
  AND a.account_type = 'SHARES';

-- THUMBI JUDE NDUNG'U (Payroll: 9113) - Normal Loan
UPDATE loans 
SET 
  amount = 1255936,
  outstanding_balance = 1192634,
  principal_repaid = 63302,
  interest_collected = 12559,
  original_principal = 1255936
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9113')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- KIMANI ROBERT MURIUKI (Payroll: 1105) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1825000.00
WHERE m.employee_id = '1105'
  AND a.account_type = 'SHARES';

-- MUTUNGA KATEE (Payroll: 1204) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1360000.00
WHERE m.employee_id = '1204'
  AND a.account_type = 'SHARES';

-- MUTUNGA KATEE (Payroll: 1204) - Normal Loan
UPDATE loans 
SET 
  amount = 247541,
  outstanding_balance = 232980,
  principal_repaid = 14561,
  interest_collected = 2475,
  original_principal = 247541
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1204')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- AIRO JOHN OYAMO (Payroll: 1203) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 243000.00
WHERE m.employee_id = '1203'
  AND a.account_type = 'SHARES';

-- AIRO JOHN OYAMO (Payroll: 1203) - Normal Loan
UPDATE loans 
SET 
  amount = 656333,
  outstanding_balance = 641417,
  principal_repaid = 14917,
  interest_collected = 6563,
  original_principal = 656333
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1203')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- AIRO JOHN OYAMO (Payroll: 1203) - Emergency 1
UPDATE loans 
SET 
  amount = 37500,
  outstanding_balance = 33750,
  principal_repaid = 3750,
  interest_collected = 375,
  original_principal = 37500
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1203')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Emergency%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- MWANGI JAYNE NJERI (Payroll: 1235) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 298000.00
WHERE m.employee_id = '1235'
  AND a.account_type = 'SHARES';

-- MWANGI JAYNE NJERI (Payroll: 1235) - Normal Loan
UPDATE loans 
SET 
  amount = 245248,
  outstanding_balance = 241475,
  principal_repaid = 3773,
  interest_collected = 2452,
  original_principal = 245248
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1235')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- MATIVO IRENE (Payroll: 1247) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 601000.00
WHERE m.employee_id = '1247'
  AND a.account_type = 'SHARES';

-- MATIVO IRENE (Payroll: 1247) - Normal Loan
UPDATE loans 
SET 
  amount = 902778,
  outstanding_balance = 888889,
  principal_repaid = 13889,
  interest_collected = 9028,
  original_principal = 902778
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1247')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- MUTURI FELISTA IGOKI (Payroll: 1264) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 775000.00
WHERE m.employee_id = '1264'
  AND a.account_type = 'SHARES';

-- KIMANI ALICE NJERI (Payroll: 1280) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 497000.00
WHERE m.employee_id = '1280'
  AND a.account_type = 'SHARES';

-- ONANI MICHAEL OWALO (Payroll: 13010) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 3820700.00
WHERE m.employee_id = '13010'
  AND a.account_type = 'SHARES';

-- NAIVASHA JANE (Payroll: 13016) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 933000.00
WHERE m.employee_id = '13016'
  AND a.account_type = 'SHARES';

-- NAIVASHA JANE (Payroll: 13016) - Normal Loan
UPDATE loans 
SET 
  amount = 680000,
  outstanding_balance = 660000,
  principal_repaid = 20000,
  interest_collected = 6800,
  original_principal = 680000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13016')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- NYAORO ANDREW OJUANG' (Payroll: 13017) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 68000.00
WHERE m.employee_id = '13017'
  AND a.account_type = 'SHARES';

-- KARUKI ESTHER WAMUTIRA (Payroll: 13018) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 275000.00
WHERE m.employee_id = '13018'
  AND a.account_type = 'SHARES';

-- KARUKI ESTHER WAMUTIRA (Payroll: 13018) - Emergency 1
UPDATE loans 
SET 
  amount = 112500,
  outstanding_balance = 100000,
  principal_repaid = 12500,
  interest_collected = 1125,
  original_principal = 112500
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13018')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Emergency%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- MWANGI LENET (Payroll: 13019) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1004201.00
WHERE m.employee_id = '13019'
  AND a.account_type = 'SHARES';

-- WANJOHI IRENE WANJIKU (Payroll: 13021) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 486050.00
WHERE m.employee_id = '13021'
  AND a.account_type = 'SHARES';

-- WANJOHI IRENE WANJIKU (Payroll: 13021) - Normal Loan
UPDATE loans 
SET 
  amount = 989237,
  outstanding_balance = 962501,
  principal_repaid = 26736,
  interest_collected = 9892,
  original_principal = 989237
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13021')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- PERTET ESTHER SHIKU (Payroll: 13024) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 182000.00
WHERE m.employee_id = '13024'
  AND a.account_type = 'SHARES';

-- AWUOR EDNAH OKWIRI (Payroll: 13022) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 957745.00
WHERE m.employee_id = '13022'
  AND a.account_type = 'SHARES';

-- AWUOR EDNAH OKWIRI (Payroll: 13022) - Normal Loan
UPDATE loans 
SET 
  amount = 584295,
  outstanding_balance = 564819,
  principal_repaid = 19477,
  interest_collected = 5843,
  original_principal = 584295
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13022')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- BUNYALI JULIUS HABAKKUK (Payroll: 13026) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 2190000.00
WHERE m.employee_id = '13026'
  AND a.account_type = 'SHARES';

-- BUNYALI JULIUS HABAKKUK (Payroll: 13026) - Normal Loan
UPDATE loans 
SET 
  amount = 450000,
  outstanding_balance = 435000,
  principal_repaid = 15000,
  interest_collected = 4500,
  original_principal = 450000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13026')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- ANGWENYI GLADYS KERUBO (Payroll: 13028) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 316000.00
WHERE m.employee_id = '13028'
  AND a.account_type = 'SHARES';

-- ANGWENYI GLADYS KERUBO (Payroll: 13028) - Normal Loan
UPDATE loans 
SET 
  amount = 116667,
  outstanding_balance = 100000,
  principal_repaid = 16667,
  interest_collected = 1167,
  original_principal = 116667
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13028')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- IRENE MUTHONI MWANGI (Payroll: 13037) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 309000.00
WHERE m.employee_id = '13037'
  AND a.account_type = 'SHARES';

-- IRENE MUTHONI MWANGI (Payroll: 13037) - Normal Loan
UPDATE loans 
SET 
  amount = 763363,
  outstanding_balance = 751619,
  principal_repaid = 11744,
  interest_collected = 7634,
  original_principal = 763363
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13037')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- GENSON NJUE MBAE (Payroll: 13044) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 620000.00
WHERE m.employee_id = '13044'
  AND a.account_type = 'SHARES';

-- GENSON NJUE MBAE (Payroll: 13044) - Normal Loan
UPDATE loans 
SET 
  amount = 1698412,
  outstanding_balance = 1673063,
  principal_repaid = 25349,
  interest_collected = 16984,
  original_principal = 1698412
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13044')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- LIDIA AKELO (Payroll: 13043) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 281000.00
WHERE m.employee_id = '13043'
  AND a.account_type = 'SHARES';

-- LIDIA AKELO (Payroll: 13043) - Normal Loan
UPDATE loans 
SET 
  amount = 258882,
  outstanding_balance = 247626,
  principal_repaid = 11256,
  interest_collected = 2589,
  original_principal = 258882
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13043')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- BERITA JUDY MUMBE (Payroll: 13057) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 399000.00
WHERE m.employee_id = '13057'
  AND a.account_type = 'SHARES';

-- BERITA JUDY MUMBE (Payroll: 13057) - Normal Loan
UPDATE loans 
SET 
  amount = 189000,
  outstanding_balance = 182000,
  principal_repaid = 7000,
  interest_collected = 1890,
  original_principal = 189000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13057')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- CAROLINE KENDI ITONGA (Payroll: 13088) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1051000.00
WHERE m.employee_id = '13088'
  AND a.account_type = 'SHARES';

-- EVELYN NYAMBURA KIHARA (Payroll: 1313) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 419000.00
WHERE m.employee_id = '1313'
  AND a.account_type = 'SHARES';

-- MAKAU LYDIA RUGURU (Payroll: 1312) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 214000.00
WHERE m.employee_id = '1312'
  AND a.account_type = 'SHARES';

-- MAKAU LYDIA RUGURU (Payroll: 1312) - Normal Loan
UPDATE loans 
SET 
  amount = 198260,
  outstanding_balance = 186598,
  principal_repaid = 11662,
  interest_collected = 1983,
  original_principal = 198260
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1312')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- MAKAU LYDIA RUGURU (Payroll: 1312) - Emergency 1
UPDATE loans 
SET 
  amount = 9167,
  outstanding_balance = 8334,
  principal_repaid = 833,
  interest_collected = 92,
  original_principal = 9167
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1312')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Emergency%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- JANET NJERI NDUNGU (Payroll: 1316) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 594000.00
WHERE m.employee_id = '1316'
  AND a.account_type = 'SHARES';

-- IGNATIUS SHISOKA MUYONGA (Payroll: 1320) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 62000.00
WHERE m.employee_id = '1320'
  AND a.account_type = 'SHARES';

-- JOYCE NJOKI MUYA (Payroll: 15097) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 580000.00
WHERE m.employee_id = '15097'
  AND a.account_type = 'SHARES';

-- ROSE WANJA KINYATI (Payroll: 15124) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 261000.00
WHERE m.employee_id = '15124'
  AND a.account_type = 'SHARES';

-- SERAPHINE ANYANGA OKUMU (Payroll: 15146) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 318000.00
WHERE m.employee_id = '15146'
  AND a.account_type = 'SHARES';

-- NICKSON MASITA ONG'ERA (Payroll: 15248) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 599000.00
WHERE m.employee_id = '15248'
  AND a.account_type = 'SHARES';

-- NICKSON MASITA ONG'ERA (Payroll: 15248) - Normal Loan
UPDATE loans 
SET 
  amount = 1479167,
  outstanding_balance = 1458333,
  principal_repaid = 20833,
  interest_collected = 14792,
  original_principal = 1479167
WHERE member_id = (SELECT id FROM members WHERE employee_id = '15248')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- KABUGI ANNE JATIAGA (Payroll: 2068) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1160000.00
WHERE m.employee_id = '2068'
  AND a.account_type = 'SHARES';

-- KABUGI ANNE JATIAGA (Payroll: 2068) - Normal Loan
UPDATE loans 
SET 
  amount = 1353282,
  outstanding_balance = 1312274,
  principal_repaid = 41009,
  interest_collected = 13533,
  original_principal = 1353282
WHERE member_id = (SELECT id FROM members WHERE employee_id = '2068')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- OBONYO SYDNEY MATHEW (Payroll: 2072) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 724500.00
WHERE m.employee_id = '2072'
  AND a.account_type = 'SHARES';

-- OBONYO SYDNEY MATHEW (Payroll: 2072) - Normal Loan
UPDATE loans 
SET 
  amount = 1210000,
  outstanding_balance = 1193334,
  principal_repaid = 16667,
  interest_collected = 12100,
  original_principal = 1210000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '2072')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- MACHARIA RUTH WAITHIRA (Payroll: 2073) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 312000.00
WHERE m.employee_id = '2073'
  AND a.account_type = 'SHARES';

-- MUSAO LEONARD OKUMU (Payroll: 2069) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 938000.00
WHERE m.employee_id = '2069'
  AND a.account_type = 'SHARES';

-- MUSAO LEONARD OKUMU (Payroll: 2069) - Normal Loan
UPDATE loans 
SET 
  amount = 2407926,
  outstanding_balance = 2372409,
  principal_repaid = 35517,
  interest_collected = 24079,
  original_principal = 2407926
WHERE member_id = (SELECT id FROM members WHERE employee_id = '2069')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- MWAURA HANNAH NYAMBURA (Payroll: 4033) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 800000.00
WHERE m.employee_id = '4033'
  AND a.account_type = 'SHARES';

-- KIMANI GLADYS WANGARI (Payroll: 4064) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 235000.00
WHERE m.employee_id = '4064'
  AND a.account_type = 'SHARES';

-- KIMANI GLADYS WANGARI (Payroll: 4064) - Normal Loan
UPDATE loans 
SET 
  amount = 308334,
  outstanding_balance = 303889,
  principal_repaid = 4444,
  interest_collected = 3083,
  original_principal = 308334
WHERE member_id = (SELECT id FROM members WHERE employee_id = '4064')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- MUTHAURA ROSE WANJA (Payroll: 5178) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1084333.00
WHERE m.employee_id = '5178'
  AND a.account_type = 'SHARES';

-- MUTHAURA ROSE WANJA (Payroll: 5178) - Normal Loan
UPDATE loans 
SET 
  amount = 778333,
  outstanding_balance = 751667,
  principal_repaid = 26667,
  interest_collected = 7783,
  original_principal = 778333
WHERE member_id = (SELECT id FROM members WHERE employee_id = '5178')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- ESTHER WANJIKU WACHIRA (Payroll: 5190) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 865000.00
WHERE m.employee_id = '5190'
  AND a.account_type = 'SHARES';

-- ESTHER WANJIKU WACHIRA (Payroll: 5190) - Normal Loan
UPDATE loans 
SET 
  amount = 1454645,
  outstanding_balance = 1418155,
  principal_repaid = 36490,
  interest_collected = 14546,
  original_principal = 1454645
WHERE member_id = (SELECT id FROM members WHERE employee_id = '5190')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- AMASA FRANKLIN KIVARA (Payroll: 6152) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 177000.00
WHERE m.employee_id = '6152'
  AND a.account_type = 'SHARES';

-- AMASA FRANKLIN KIVARA (Payroll: 6152) - Normal Loan
UPDATE loans 
SET 
  amount = 122591,
  outstanding_balance = 115780,
  principal_repaid = 6811,
  interest_collected = 1226,
  original_principal = 122591
WHERE member_id = (SELECT id FROM members WHERE employee_id = '6152')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- ABDALLA ABDULMAJID MBARUK (Payroll: 8055) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1090000.00
WHERE m.employee_id = '8055'
  AND a.account_type = 'SHARES';

-- MWAKIO JEFFERSON MWAINGE (Payroll: 8036) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 979500.00
WHERE m.employee_id = '8036'
  AND a.account_type = 'SHARES';

-- MWAKIO JEFFERSON MWAINGE (Payroll: 8036) - Normal Loan
UPDATE loans 
SET 
  amount = 35000,
  outstanding_balance = 30000,
  principal_repaid = 5000,
  interest_collected = 350,
  original_principal = 35000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '8036')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- MACHI IRENE KITAWA (Payroll: 8028) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 738000.00
WHERE m.employee_id = '8028'
  AND a.account_type = 'SHARES';

-- KING'ARA LUCY WANJIRU (Payroll: 8054) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1109000.00
WHERE m.employee_id = '8054'
  AND a.account_type = 'SHARES';

-- KING'ARA LUCY WANJIRU (Payroll: 8054) - Normal Loan
UPDATE loans 
SET 
  amount = 560923,
  outstanding_balance = 498599,
  principal_repaid = 62325,
  interest_collected = 5609,
  original_principal = 560923
WHERE member_id = (SELECT id FROM members WHERE employee_id = '8054')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- AWITI GRACE LYNETTE (Payroll: 9037) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 378000.00
WHERE m.employee_id = '9037'
  AND a.account_type = 'SHARES';

-- Migwi CHARLOTTE WANJIKU (Payroll: 9041) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1042400.00
WHERE m.employee_id = '9041'
  AND a.account_type = 'SHARES';

-- Migwi CHARLOTTE WANJIKU (Payroll: 9041) - Normal Loan
UPDATE loans 
SET 
  amount = 960000,
  outstanding_balance = 940000,
  principal_repaid = 20000,
  interest_collected = 9600,
  original_principal = 960000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9041')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- ADELE MACHARIA LINDA WANGUI (Payroll: 9084) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 483000.00
WHERE m.employee_id = '9084'
  AND a.account_type = 'SHARES';

-- NGURE RACHAEL WANJIKU (Payroll: 9098) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 434000.00
WHERE m.employee_id = '9098'
  AND a.account_type = 'SHARES';

-- NGURE RACHAEL WANJIKU (Payroll: 9098) - Normal Loan
UPDATE loans 
SET 
  amount = 1186463,
  outstanding_balance = 1168210,
  principal_repaid = 18253,
  interest_collected = 11865,
  original_principal = 1186463
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9098')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- KINOTI LINDA GATWIRI (Payroll: 9105) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 532000.00
WHERE m.employee_id = '9105'
  AND a.account_type = 'SHARES';

-- KINOTI LINDA GATWIRI (Payroll: 9105) - Normal Loan
UPDATE loans 
SET 
  amount = 215417,
  outstanding_balance = 210833,
  principal_repaid = 4583,
  interest_collected = 2154,
  original_principal = 215417
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9105')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- ATIENO NAOMI JUDITH (Payroll: 9108) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 746000.00
WHERE m.employee_id = '9108'
  AND a.account_type = 'SHARES';

-- ATIENO NAOMI JUDITH (Payroll: 9108) - Normal Loan
UPDATE loans 
SET 
  amount = 1291311,
  outstanding_balance = 1263239,
  principal_repaid = 28072,
  interest_collected = 12913,
  original_principal = 1291311
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9108')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- GICHURI JERIOTH MUTHONI (Payroll: 9122) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 869000.00
WHERE m.employee_id = '9122'
  AND a.account_type = 'SHARES';

-- GICHURI JERIOTH MUTHONI (Payroll: 9122) - Normal Loan
UPDATE loans 
SET 
  amount = 445084,
  outstanding_balance = 398576,
  principal_repaid = 46508,
  interest_collected = 4451,
  original_principal = 445084
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9122')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- NDUNDA PERPETUA WANZA (Payroll: 9181) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1404000.00
WHERE m.employee_id = '9181'
  AND a.account_type = 'SHARES';

-- NDUNDA PERPETUA WANZA (Payroll: 9181) - Normal Loan
UPDATE loans 
SET 
  amount = 1470000,
  outstanding_balance = 1440000,
  principal_repaid = 30000,
  interest_collected = 14700,
  original_principal = 1470000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9181')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- WERE LORINE AKOTH (Payroll: 9183) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 504000.00
WHERE m.employee_id = '9183'
  AND a.account_type = 'SHARES';

-- WERE LORINE AKOTH (Payroll: 9183) - Normal Loan
UPDATE loans 
SET 
  amount = 408527,
  outstanding_balance = 395761,
  principal_repaid = 12766,
  interest_collected = 4085,
  original_principal = 408527
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9183')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- WERE LORINE AKOTH (Payroll: 9183) - Emergency 1
UPDATE loans 
SET 
  amount = 120000,
  outstanding_balance = 110000,
  principal_repaid = 10000,
  interest_collected = 1200,
  original_principal = 120000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9183')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Emergency%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- MATALANGA JOHN KAMAU (Payroll: 9198) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 748250.00
WHERE m.employee_id = '9198'
  AND a.account_type = 'SHARES';

-- ALWODI MARK MUNAVI (Payroll: 9212) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 251000.00
WHERE m.employee_id = '9212'
  AND a.account_type = 'SHARES';

-- ALWODI MARK MUNAVI (Payroll: 9212) - Normal Loan
UPDATE loans 
SET 
  amount = 580000,
  outstanding_balance = 570000,
  principal_repaid = 10000,
  interest_collected = 5800,
  original_principal = 580000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9212')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- RAPONGO COLLINS BWIRE (Payroll: 9291) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 255000.00
WHERE m.employee_id = '9291'
  AND a.account_type = 'SHARES';

-- MARY MUTINDI KIMANTHI (Payroll: 9303) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 613000.00
WHERE m.employee_id = '9303'
  AND a.account_type = 'SHARES';

-- MARY MUTINDI KIMANTHI (Payroll: 9303) - Normal Loan
UPDATE loans 
SET 
  amount = 969756,
  outstanding_balance = 930366,
  principal_repaid = 39390,
  interest_collected = 9698,
  original_principal = 969756
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9303')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- FLORENCE MWENDE WAMBU (Payroll: 9323) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 217000.00
WHERE m.employee_id = '9323'
  AND a.account_type = 'SHARES';

-- CHEGE SAMUEL KARARI (Payroll: 1146) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1644025.00
WHERE m.employee_id = '1146'
  AND a.account_type = 'SHARES';

-- CHEGE SAMUEL KARARI (Payroll: 1146) - Normal Loan
UPDATE loans 
SET 
  amount = 2661250,
  outstanding_balance = 2619668,
  principal_repaid = 41582,
  interest_collected = 26613,
  original_principal = 2661250
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1146')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- JOYCE GATWIRI KITHINJI (Payroll: 15253) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 308000.00
WHERE m.employee_id = '15253'
  AND a.account_type = 'SHARES';

-- MAINGA DANIEL LOTI (Payroll: 6109) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 4124000.00
WHERE m.employee_id = '6109'
  AND a.account_type = 'SHARES';

-- MAINGA DANIEL LOTI (Payroll: 6109) - Normal Loan
UPDATE loans 
SET 
  amount = 2335417,
  outstanding_balance = 2212500,
  principal_repaid = 122917,
  interest_collected = 23354,
  original_principal = 2335417
WHERE member_id = (SELECT id FROM members WHERE employee_id = '6109')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- ROTICH ROBERT ALEX (Payroll: 6075) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 6038400.00
WHERE m.employee_id = '6075'
  AND a.account_type = 'SHARES';

-- CHRISTINE C MUTHONI MURIITHI (Payroll: 15230) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 300500.00
WHERE m.employee_id = '15230'
  AND a.account_type = 'SHARES';

-- GITAU MARY WAIRIMU (Payroll: 6092) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1183000.00
WHERE m.employee_id = '6092'
  AND a.account_type = 'SHARES';

-- GITAU MARY WAIRIMU (Payroll: 6092) - Normal Loan
UPDATE loans 
SET 
  amount = 3535556,
  outstanding_balance = 3483562,
  principal_repaid = 51993,
  interest_collected = 35356,
  original_principal = 3535556
WHERE member_id = (SELECT id FROM members WHERE employee_id = '6092')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- MAINA STEPHEN IRUNGU (Payroll: 6114) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1111000.00
WHERE m.employee_id = '6114'
  AND a.account_type = 'SHARES';

-- MAINA STEPHEN IRUNGU (Payroll: 6114) - Normal Loan
UPDATE loans 
SET 
  amount = 791904,
  outstanding_balance = 750225,
  principal_repaid = 41679,
  interest_collected = 7919,
  original_principal = 791904
WHERE member_id = (SELECT id FROM members WHERE employee_id = '6114')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- BUURI PAMELA MWENDE (Payroll: 6143) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 255000.00
WHERE m.employee_id = '6143'
  AND a.account_type = 'SHARES';

-- MURIITHI ROSE WANGARI (Payroll: 6136) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 664000.00
WHERE m.employee_id = '6136'
  AND a.account_type = 'SHARES';

-- MURIITHI ROSE WANGARI (Payroll: 6136) - Normal Loan
UPDATE loans 
SET 
  amount = 60000,
  outstanding_balance = 53333,
  principal_repaid = 6667,
  interest_collected = 600,
  original_principal = 60000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '6136')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- NARANGWI PETER KIBUINE (Payroll: 12002) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 11919732.00
WHERE m.employee_id = '12002'
  AND a.account_type = 'SHARES';

-- MICHAEL NG'ANG'A KAMAU (Payroll: 6183) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 50000.00
WHERE m.employee_id = '6183'
  AND a.account_type = 'SHARES';

-- KINYUA FRIDAH NAITORE (Payroll: 12006) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1202000.00
WHERE m.employee_id = '12006'
  AND a.account_type = 'SHARES';

-- DENIS MOSE TANGASO (Payroll: 12007) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 172000.00
WHERE m.employee_id = '12007'
  AND a.account_type = 'SHARES';

-- DENIS MOSE TANGASO (Payroll: 12007) - Normal Loan
UPDATE loans 
SET 
  amount = 502667,
  outstanding_balance = 489439,
  principal_repaid = 13228,
  interest_collected = 5027,
  original_principal = 502667
WHERE member_id = (SELECT id FROM members WHERE employee_id = '12007')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- TEDDY AYODI (Payroll: 6095) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 9770000.00
WHERE m.employee_id = '6095'
  AND a.account_type = 'SHARES';

-- MWAGI JOSEPH ONYANGO (Payroll: 9125) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1816000.00
WHERE m.employee_id = '9125'
  AND a.account_type = 'SHARES';

-- CHARLES NJUNGE MWAURA (Payroll: 9308) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 480000.00
WHERE m.employee_id = '9308'
  AND a.account_type = 'SHARES';

-- CHARLES NJUNGE MWAURA (Payroll: 9308) - Normal Loan
UPDATE loans 
SET 
  amount = 328592,
  outstanding_balance = 316856,
  principal_repaid = 11735,
  interest_collected = 3286,
  original_principal = 328592
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9308')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- PATRICK MULI KALUMBA (Payroll: 5198) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 989500.00
WHERE m.employee_id = '5198'
  AND a.account_type = 'SHARES';

-- PATRICK MULI KALUMBA (Payroll: 5198) - Normal Loan
UPDATE loans 
SET 
  amount = 1327519,
  outstanding_balance = 1260208,
  principal_repaid = 67311,
  interest_collected = 13275,
  original_principal = 1327519
WHERE member_id = (SELECT id FROM members WHERE employee_id = '5198')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- MARCOS OTIENO ANYUMBA (Payroll: 1206) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1123000.00
WHERE m.employee_id = '1206'
  AND a.account_type = 'SHARES';

-- MARCOS OTIENO ANYUMBA (Payroll: 1206) - Normal Loan
UPDATE loans 
SET 
  amount = 1722222,
  outstanding_balance = 1694444,
  principal_repaid = 27778,
  interest_collected = 17222,
  original_principal = 1722222
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1206')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- NALWENJE ALEX OKOTH (Payroll: 1299) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 78000.00
WHERE m.employee_id = '1299'
  AND a.account_type = 'SHARES';

-- LUNG'ATSO ASIYA ESIEMINYI (Payroll: 13054) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 268000.00
WHERE m.employee_id = '13054'
  AND a.account_type = 'SHARES';

-- LUNG'ATSO ASIYA ESIEMINYI (Payroll: 13054) - Normal Loan
UPDATE loans 
SET 
  amount = 269899,
  outstanding_balance = 261790,
  principal_repaid = 8109,
  interest_collected = 2699,
  original_principal = 269899
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13054')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- EUNICE WAMBUI NJOGU (Payroll: 15260) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 252500.00
WHERE m.employee_id = '15260'
  AND a.account_type = 'SHARES';

-- Mr Simeon Odhiambo Owino (Payroll: 1327) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 158000.00
WHERE m.employee_id = '1327'
  AND a.account_type = 'SHARES';

-- Mr Andrew Gitau Githua Kaminja (Payroll: 9341) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 147000.00
WHERE m.employee_id = '9341'
  AND a.account_type = 'SHARES';

-- Mr Andrew Gitau Githua Kaminja (Payroll: 9341) - Normal Loan
UPDATE loans 
SET 
  amount = 113750,
  outstanding_balance = 110833,
  principal_repaid = 2917,
  interest_collected = 1137,
  original_principal = 113750
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9341')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Ms Ann Wandia Wangu (Payroll: 15037) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 103000.00
WHERE m.employee_id = '15037'
  AND a.account_type = 'SHARES';

-- Ms Ann Wandia Wangu (Payroll: 15037) - Normal Loan
UPDATE loans 
SET 
  amount = 230000,
  outstanding_balance = 226667,
  principal_repaid = 3333,
  interest_collected = 2300,
  original_principal = 230000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '15037')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Mr Edwin Were Saaya (Payroll: 5216) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 550000.00
WHERE m.employee_id = '5216'
  AND a.account_type = 'SHARES';

-- Mr Edwin Were Saaya (Payroll: 5216) - Normal Loan
UPDATE loans 
SET 
  amount = 612500,
  outstanding_balance = 600000,
  principal_repaid = 12500,
  interest_collected = 6125,
  original_principal = 612500
WHERE member_id = (SELECT id FROM members WHERE employee_id = '5216')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Mr Kelvin Mulinge Kyalo (Payroll: 15012) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 144000.00
WHERE m.employee_id = '15012'
  AND a.account_type = 'SHARES';

-- Ms Claire Wairimu Kinyanjui (Payroll: 1274) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 180000.00
WHERE m.employee_id = '1274'
  AND a.account_type = 'SHARES';

-- TITUS THUMBI MURIUKI (Payroll: 9296) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 655000.00
WHERE m.employee_id = '9296'
  AND a.account_type = 'SHARES';

-- JUWEIRIYA ABDALLLA (Payroll: 13113) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 275000.00
WHERE m.employee_id = '13113'
  AND a.account_type = 'SHARES';

-- MOSES KURIA (Payroll: 1332) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 423000.00
WHERE m.employee_id = '1332'
  AND a.account_type = 'SHARES';

-- MOSES KURIA (Payroll: 1332) - Normal Loan
UPDATE loans 
SET 
  amount = 1163720,
  outstanding_balance = 1145537,
  principal_repaid = 18183,
  interest_collected = 11637,
  original_principal = 1163720
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1332')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Mr George Mbugua Kariuki (Payroll: 9099) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 320000.00
WHERE m.employee_id = '9099'
  AND a.account_type = 'SHARES';

-- Mr George Mbugua Kariuki (Payroll: 9099) - Normal Loan
UPDATE loans 
SET 
  amount = 862500,
  outstanding_balance = 850000,
  principal_repaid = 12500,
  interest_collected = 8625,
  original_principal = 862500
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9099')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Ms Caroline Nekesa (Payroll: 4157) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 320000.00
WHERE m.employee_id = '4157'
  AND a.account_type = 'SHARES';

-- Ms Teresia Trizer Wamucii (Payroll: 9229) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 335000.00
WHERE m.employee_id = '9229'
  AND a.account_type = 'SHARES';

-- Peter Wanjohi Maina (Payroll: 12010) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1435000.00
WHERE m.employee_id = '12010'
  AND a.account_type = 'SHARES';

-- Peter Wanjohi Maina (Payroll: 12010) - Normal Loan
UPDATE loans 
SET 
  amount = 450000,
  outstanding_balance = 433333,
  principal_repaid = 16667,
  interest_collected = 4500,
  original_principal = 450000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '12010')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Ms Mercy Gathoni Muriithi (Payroll: 13068) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 93000.00
WHERE m.employee_id = '13068'
  AND a.account_type = 'SHARES';

-- Mercy Muringi Muthoga (Payroll: 9344) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 135000.00
WHERE m.employee_id = '9344'
  AND a.account_type = 'SHARES';

-- Mr Peter Mwiti Kamundi (Payroll: 13061) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 96500.00
WHERE m.employee_id = '13061'
  AND a.account_type = 'SHARES';

-- Mr Peter Mwiti Kamundi (Payroll: 13061) - Normal Loan
UPDATE loans 
SET 
  amount = 273963,
  outstanding_balance = 269471,
  principal_repaid = 4491,
  interest_collected = 2740,
  original_principal = 273963
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13061')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Winrose Miroyo Ondego (Payroll: 5226) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 47000.00
WHERE m.employee_id = '5226'
  AND a.account_type = 'SHARES';

-- Mr Stephen Ngugi Muriu (Payroll: 6117) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 553000.00
WHERE m.employee_id = '6117'
  AND a.account_type = 'SHARES';

-- Mr Stephen Ngugi Muriu (Payroll: 6117) - Normal Loan
UPDATE loans 
SET 
  amount = 1408000,
  outstanding_balance = 1364000,
  principal_repaid = 44000,
  interest_collected = 14080,
  original_principal = 1408000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '6117')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Mr Eric Atinda Orina (Payroll: 9289) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 82000.00
WHERE m.employee_id = '9289'
  AND a.account_type = 'SHARES';

-- Mr Eric Atinda Orina (Payroll: 9289) - Normal Loan
UPDATE loans 
SET 
  amount = 97734,
  outstanding_balance = 95407,
  principal_repaid = 2327,
  interest_collected = 977,
  original_principal = 97734
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9289')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Ms Jane Gakii Miriti (Payroll: 5159) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 78000.00
WHERE m.employee_id = '5159'
  AND a.account_type = 'SHARES';

-- Ms Jane Gakii Miriti (Payroll: 5159) - Normal Loan
UPDATE loans 
SET 
  amount = 90182,
  outstanding_balance = 87744,
  principal_repaid = 2438,
  interest_collected = 902,
  original_principal = 90182
WHERE member_id = (SELECT id FROM members WHERE employee_id = '5159')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Ms Elizabeth Nekesa Sitati (Payroll: 15045) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 209000.00
WHERE m.employee_id = '15045'
  AND a.account_type = 'SHARES';

-- Ms Elizabeth Nekesa Sitati (Payroll: 15045) - Normal Loan
UPDATE loans 
SET 
  amount = 241500,
  outstanding_balance = 236250,
  principal_repaid = 5250,
  interest_collected = 2415,
  original_principal = 241500
WHERE member_id = (SELECT id FROM members WHERE employee_id = '15045')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Pauline Waithira Mwaura (Payroll: 5223) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 175000.00
WHERE m.employee_id = '5223'
  AND a.account_type = 'SHARES';

-- Kenedy Kirimi Mati (Payroll: 15319) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 115000.00
WHERE m.employee_id = '15319'
  AND a.account_type = 'SHARES';

-- Gibson Oguda Mbaja (Payroll: 1339) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 63000.00
WHERE m.employee_id = '1339'
  AND a.account_type = 'SHARES';

-- Mr James Maina Kimani (Payroll: 5191) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 427000.00
WHERE m.employee_id = '5191'
  AND a.account_type = 'SHARES';

-- Mr James Maina Kimani (Payroll: 5191) - Normal Loan
UPDATE loans 
SET 
  amount = 400000,
  outstanding_balance = 391667,
  principal_repaid = 8333,
  interest_collected = 4000,
  original_principal = 400000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '5191')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Faith Atieno Nyaoro (Payroll: 1341) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 47000.00
WHERE m.employee_id = '1341'
  AND a.account_type = 'SHARES';

-- Faith Atieno Nyaoro (Payroll: 1341) - Normal Loan
UPDATE loans 
SET 
  amount = 35556,
  outstanding_balance = 33333,
  principal_repaid = 2222,
  interest_collected = 356,
  original_principal = 35556
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1341')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Mr Stephen Ndavuti Ndunda (Payroll: 9272) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 9000.00
WHERE m.employee_id = '9272'
  AND a.account_type = 'SHARES';

-- Jorim Ochieng Awuor (Payroll: 1338) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 63000.00
WHERE m.employee_id = '1338'
  AND a.account_type = 'SHARES';

-- Jorim Ochieng Awuor (Payroll: 1338) - Normal Loan
UPDATE loans 
SET 
  amount = 179277,
  outstanding_balance = 176431,
  principal_repaid = 2846,
  interest_collected = 1793,
  original_principal = 179277
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1338')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Mr Joram Katiwa Mutunga (Payroll: 6182) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 215000.00
WHERE m.employee_id = '6182'
  AND a.account_type = 'SHARES';

-- Mr Joram Katiwa Mutunga (Payroll: 6182) - Normal Loan
UPDATE loans 
SET 
  amount = 106345,
  outstanding_balance = 104683,
  principal_repaid = 1662,
  interest_collected = 1063,
  original_principal = 106345
WHERE member_id = (SELECT id FROM members WHERE employee_id = '6182')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Mr Walter Kipkurui Koech (Payroll: 1265) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 33000.00
WHERE m.employee_id = '1265'
  AND a.account_type = 'SHARES';

-- Mr Walter Kipkurui Koech (Payroll: 1265) - Normal Loan
UPDATE loans 
SET 
  amount = 38333,
  outstanding_balance = 37778,
  principal_repaid = 556,
  interest_collected = 383,
  original_principal = 38333
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1265')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Mr Andrew Muhia Kagiri (Payroll: 1331) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 253000.00
WHERE m.employee_id = '1331'
  AND a.account_type = 'SHARES';

-- Mr Andrew Muhia Kagiri (Payroll: 1331) - Normal Loan
UPDATE loans 
SET 
  amount = 550572,
  outstanding_balance = 537463,
  principal_repaid = 13109,
  interest_collected = 5506,
  original_principal = 550572
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1331')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Mr Dennis William Mukwanja (Payroll: 1326) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 60000.00
WHERE m.employee_id = '1326'
  AND a.account_type = 'SHARES';

-- Mr Dennis William Mukwanja (Payroll: 1326) - Normal Loan
UPDATE loans 
SET 
  amount = 31667,
  outstanding_balance = 30000,
  principal_repaid = 1667,
  interest_collected = 317,
  original_principal = 31667
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1326')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Miss Dorcas Wanjugu Maina (Payroll: 15016) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 57000.00
WHERE m.employee_id = '15016'
  AND a.account_type = 'SHARES';

-- Kevin Njenga Nguyai (Payroll: 5224) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 60000.00
WHERE m.employee_id = '5224'
  AND a.account_type = 'SHARES';

-- Kevin Njenga Nguyai (Payroll: 5224) - Normal Loan
UPDATE loans 
SET 
  amount = 66667,
  outstanding_balance = 58333,
  principal_repaid = 8333,
  interest_collected = 667,
  original_principal = 66667
WHERE member_id = (SELECT id FROM members WHERE employee_id = '5224')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Ms Naomi Wangui Nganga (Payroll: 9300) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 103000.00
WHERE m.employee_id = '9300'
  AND a.account_type = 'SHARES';

-- Ms Naomi Wangui Nganga (Payroll: 9300) - Normal Loan
UPDATE loans 
SET 
  amount = 201364,
  outstanding_balance = 197046,
  principal_repaid = 4318,
  interest_collected = 2014,
  original_principal = 201364
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9300')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Mrs Elizabeth Muthoni Karanja (Payroll: 13094) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 60000.00
WHERE m.employee_id = '13094'
  AND a.account_type = 'SHARES';

-- Ms Doreen Muthoni Mpangua (Payroll: 1277) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 63000.00
WHERE m.employee_id = '1277'
  AND a.account_type = 'SHARES';

-- Glenn Runanu Kabiru (Payroll: 6210) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 134000.00
WHERE m.employee_id = '6210'
  AND a.account_type = 'SHARES';

-- Glenn Runanu Kabiru (Payroll: 6210) - Normal Loan
UPDATE loans 
SET 
  amount = 311275,
  outstanding_balance = 306710,
  principal_repaid = 4565,
  interest_collected = 3113,
  original_principal = 311275
WHERE member_id = (SELECT id FROM members WHERE employee_id = '6210')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Glenn Runanu Kabiru (Payroll: 6210) - Emergency 1
UPDATE loans 
SET 
  amount = 9167,
  outstanding_balance = 8333,
  principal_repaid = 833,
  interest_collected = 92,
  original_principal = 9167
WHERE member_id = (SELECT id FROM members WHERE employee_id = '6210')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Emergency%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Mr Benson Waweru Muriuki (Payroll: 13109) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 86500.00
WHERE m.employee_id = '13109'
  AND a.account_type = 'SHARES';

-- Mr Benson Waweru Muriuki (Payroll: 13109) - Normal Loan
UPDATE loans 
SET 
  amount = 131736,
  outstanding_balance = 129854,
  principal_repaid = 1882,
  interest_collected = 1317,
  original_principal = 131736
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13109')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Joshua Wafula Kakai (Payroll: 9342) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 90000.00
WHERE m.employee_id = '9342'
  AND a.account_type = 'SHARES';

-- Mr Reynold Onyango Oketch (Payroll: 13093) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 54000.00
WHERE m.employee_id = '13093'
  AND a.account_type = 'SHARES';

-- Felix Ochieng Otieno (Payroll: 9352) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 51000.00
WHERE m.employee_id = '9352'
  AND a.account_type = 'SHARES';

-- Mr Anthony Musembi Ndambuki (Payroll: 6196) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 170000.00
WHERE m.employee_id = '6196'
  AND a.account_type = 'SHARES';

-- Mr Edwin Kimathi Mwenda (Payroll: 4152) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 539000.00
WHERE m.employee_id = '4152'
  AND a.account_type = 'SHARES';

-- Mr Edwin Kimathi Mwenda (Payroll: 4152) - Normal Loan
UPDATE loans 
SET 
  amount = 1225000,
  outstanding_balance = 1200000,
  principal_repaid = 25000,
  interest_collected = 12250,
  original_principal = 1225000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '4152')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Edward  Jenings Koganga (Payroll: 9350) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 60000.00
WHERE m.employee_id = '9350'
  AND a.account_type = 'SHARES';

-- Peter Mwangi Maina (Payroll: 1336) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 45000.00
WHERE m.employee_id = '1336'
  AND a.account_type = 'SHARES';

-- Peter Mwangi Maina (Payroll: 1336) - Normal Loan
UPDATE loans 
SET 
  amount = 51891,
  outstanding_balance = 49420,
  principal_repaid = 2471,
  interest_collected = 519,
  original_principal = 51891
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1336')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Mrs Rachel Nkatha Mwenda (Payroll: 13114) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 130000.00
WHERE m.employee_id = '13114'
  AND a.account_type = 'SHARES';

-- Mrs Rachel Nkatha Mwenda (Payroll: 13114) - Normal Loan
UPDATE loans 
SET 
  amount = 275000,
  outstanding_balance = 262500,
  principal_repaid = 12500,
  interest_collected = 2750,
  original_principal = 275000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13114')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Joseph Oduory Ouma (Payroll: 6205) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 120000.00
WHERE m.employee_id = '6205'
  AND a.account_type = 'SHARES';

-- Evans Kipkorir Yegon (Payroll: 15353) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 72000.00
WHERE m.employee_id = '15353'
  AND a.account_type = 'SHARES';

-- Miss Grace Wanjiru Muthii (Payroll: 15105) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 66000.00
WHERE m.employee_id = '15105'
  AND a.account_type = 'SHARES';

-- Miss Grace Wanjiru Muthii (Payroll: 15105) - Normal Loan
UPDATE loans 
SET 
  amount = 157551,
  outstanding_balance = 155050,
  principal_repaid = 2501,
  interest_collected = 1576,
  original_principal = 157551
WHERE member_id = (SELECT id FROM members WHERE employee_id = '15105')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Margaret Wanini Munji (Payroll: 5336) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 90000.00
WHERE m.employee_id = '5336'
  AND a.account_type = 'SHARES';

-- Mr Japheth Kanyoo Matheka (Payroll: 5207) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 85500.00
WHERE m.employee_id = '5207'
  AND a.account_type = 'SHARES';

-- Mr Japheth Kanyoo Matheka (Payroll: 5207) - Normal Loan
UPDATE loans 
SET 
  amount = 140000,
  outstanding_balance = 130000,
  principal_repaid = 10000,
  interest_collected = 1400,
  original_principal = 140000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '5207')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Maria Kalumu Isaac (Payroll: 9367) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 33000.00
WHERE m.employee_id = '9367'
  AND a.account_type = 'SHARES';

-- Mercy Mwende Kitivo (Payroll: 6208) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 50000.00
WHERE m.employee_id = '6208'
  AND a.account_type = 'SHARES';

-- Mr Robson Chege Mburu (Payroll: 1289) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 72000.00
WHERE m.employee_id = '1289'
  AND a.account_type = 'SHARES';

-- Mr Robson Chege Mburu (Payroll: 1289) - Emergency 1
UPDATE loans 
SET 
  amount = 25000,
  outstanding_balance = 22500,
  principal_repaid = 2500,
  interest_collected = 250,
  original_principal = 25000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1289')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Emergency%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Miss Josephine Kavata Kioko (Payroll: 9311) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 101000.00
WHERE m.employee_id = '9311'
  AND a.account_type = 'SHARES';

-- Ms Cecelina Gacheri Mwobobia (Payroll: 15128) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 690000.00
WHERE m.employee_id = '15128'
  AND a.account_type = 'SHARES';

-- Daniel Muasya Muasa (Payroll: 5231) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 35000.00
WHERE m.employee_id = '5231'
  AND a.account_type = 'SHARES';

-- Alex Wamae Gatua (Payroll: 13130) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 18000.00
WHERE m.employee_id = '13130'
  AND a.account_type = 'SHARES';

-- Levis Mwaniki Gaitho (Payroll: 1347) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 58000.00
WHERE m.employee_id = '1347'
  AND a.account_type = 'SHARES';

-- Levis Mwaniki Gaitho (Payroll: 1347) - Normal Loan
UPDATE loans 
SET 
  amount = 170000,
  outstanding_balance = 167639,
  principal_repaid = 2361,
  interest_collected = 1700,
  original_principal = 170000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1347')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Mr Alex Njenga Mwai (Payroll: 6171) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 59500.00
WHERE m.employee_id = '6171'
  AND a.account_type = 'SHARES';

-- Joseph Gituma (Payroll: 5225) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 150000.00
WHERE m.employee_id = '5225'
  AND a.account_type = 'SHARES';

-- Mr Joseph Muigai Wainaina (Payroll: 7103) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 438000.00
WHERE m.employee_id = '7103'
  AND a.account_type = 'SHARES';

-- Cyrus Mwaura Njoroge (Payroll: 5337) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 50000.00
WHERE m.employee_id = '5337'
  AND a.account_type = 'SHARES';

-- Ms Hadija Duba (Payroll: 9213) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 15000.00
WHERE m.employee_id = '9213'
  AND a.account_type = 'SHARES';

-- Mwihaki Kabura (Payroll: 6215) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 50000.00
WHERE m.employee_id = '6215'
  AND a.account_type = 'SHARES';

-- Miss Jackline Mwihaki Makumi (Payroll: 1330) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 12000.00
WHERE m.employee_id = '1330'
  AND a.account_type = 'SHARES';

-- Abigail Sakini Wabwoba (Payroll: 5243) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 20000.00
WHERE m.employee_id = '5243'
  AND a.account_type = 'SHARES';

-- Benard Muthiani Kasuni (Payroll: 1342) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 20000.00
WHERE m.employee_id = '1342'
  AND a.account_type = 'SHARES';

-- Mr Gideon Kipkirui Bii (Payroll: 9295) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 103000.00
WHERE m.employee_id = '9295'
  AND a.account_type = 'SHARES';

-- Loice Buyaki Momanyi (Payroll: 13125) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 40000.00
WHERE m.employee_id = '13125'
  AND a.account_type = 'SHARES';

-- Mr Alvin Mukubwa Kituyi (Payroll: 6222) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 9000.00
WHERE m.employee_id = '6222'
  AND a.account_type = 'SHARES';

-- Mr Alvin Mukubwa Kituyi (Payroll: 6222) - Normal Loan
UPDATE loans 
SET 
  amount = 66000,
  outstanding_balance = 55000,
  principal_repaid = 11000,
  interest_collected = 660,
  original_principal = 66000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '6222')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Lucy Njeri Njroge (Payroll: 1351) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 9000.00
WHERE m.employee_id = '1351'
  AND a.account_type = 'SHARES';

-- Miss Ann Maureen Kendi Murithi (Payroll: 1333) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 12000.00
WHERE m.employee_id = '1333'
  AND a.account_type = 'SHARES';

-- Mr Eric Stanley Ng'ethe (Payroll: 5160) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 31079.00
WHERE m.employee_id = '5160'
  AND a.account_type = 'SHARES';

-- Cynthia Jerobon Kiptanui (Payroll: 6221) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 20000.00
WHERE m.employee_id = '6221'
  AND a.account_type = 'SHARES';

-- David Makuto Mmata (Payroll: 1353) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 10000.00
WHERE m.employee_id = '1353'
  AND a.account_type = 'SHARES';

SET SQL_SAFE_UPDATES = 1;

-- Summary
SELECT 'Fix complete' AS Status;
SELECT COUNT(*) AS loans_with_data, SUM(outstanding_balance) AS total_outstanding 
FROM loans WHERE outstanding_balance > 0;
