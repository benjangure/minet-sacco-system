USE minetsacco;
SET SQL_SAFE_UPDATES = 0;

-- ========================================
-- FIX LOAN MATCHING
-- ========================================

-- Mr Sammy Muthui  (CSV: MUTHUI SAMMY)
UPDATE loans 
SET 
  amount = 5899990,
  outstanding_balance = 5703323,
  principal_repaid = 196667,
  interest_collected = 59000,
  original_principal = 5899990
WHERE member_id = 267
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- KEGODE EDWIN AGALOMBA  (CSV: KEGODE EDWIN AGALOMBA)
UPDATE loans 
SET 
  amount = 1932980,
  outstanding_balance = 1825592,
  principal_repaid = 107388,
  interest_collected = 19330,
  original_principal = 1932980
WHERE member_id = 275
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Mr Katee Mutunga  (CSV: MUTUNGA KATEE)
UPDATE loans 
SET 
  amount = 247541,
  outstanding_balance = 232980,
  principal_repaid = 14561,
  interest_collected = 2475,
  original_principal = 247541
WHERE member_id = 278
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Ms Irene Mativo  (CSV: MATIVO IRENE)
UPDATE loans 
SET 
  amount = 902778,
  outstanding_balance = 888889,
  principal_repaid = 13889,
  interest_collected = 9028,
  original_principal = 902778
WHERE member_id = 282
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Ms Jane Naivasha  (CSV: NAIVASHA JANE)
UPDATE loans 
SET 
  amount = 680000,
  outstanding_balance = 660000,
  principal_repaid = 20000,
  interest_collected = 6800,
  original_principal = 680000
WHERE member_id = 286
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- BUNYALI JULIUS HABAKKUK  (CSV: BUNYALI JULIUS HABAKKUK)
UPDATE loans 
SET 
  amount = 450000,
  outstanding_balance = 435000,
  principal_repaid = 15000,
  interest_collected = 4500,
  original_principal = 450000
WHERE member_id = 293
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Mrs Irene Muthoni Mwangi  (CSV: IRENE MUTHONI MWANGI)
UPDATE loans 
SET 
  amount = 763363,
  outstanding_balance = 751619,
  principal_repaid = 11744,
  interest_collected = 7634,
  original_principal = 763363
WHERE member_id = 296
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Mr Genson Njue Mbae  (CSV: GENSON NJUE MBAE)
UPDATE loans 
SET 
  amount = 1698412,
  outstanding_balance = 1673063,
  principal_repaid = 25349,
  interest_collected = 16984,
  original_principal = 1698412
WHERE member_id = 297
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- LIDIA AKELO  (CSV: LIDIA AKELO)
UPDATE loans 
SET 
  amount = 258882,
  outstanding_balance = 247626,
  principal_repaid = 11256,
  interest_collected = 2589,
  original_principal = 258882
WHERE member_id = 298
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- MAKAU LYDIA RUGURU  (CSV: MAKAU LYDIA RUGURU)
UPDATE loans 
SET 
  amount = 198260,
  outstanding_balance = 186598,
  principal_repaid = 11662,
  interest_collected = 1983,
  original_principal = 198260
WHERE member_id = 302
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- MAKAU LYDIA RUGURU  Emergency 1 (CSV: MAKAU LYDIA RUGURU)
UPDATE loans 
SET 
  amount = 9167,
  outstanding_balance = 8334,
  principal_repaid = 833,
  interest_collected = 92,
  original_principal = 9167
WHERE member_id = 302
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Emergency%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Mr Nickson Masita Ong'era  (CSV: NICKSON MASITA ONG'ERA)
UPDATE loans 
SET 
  amount = 1479167,
  outstanding_balance = 1458333,
  principal_repaid = 20833,
  interest_collected = 14792,
  original_principal = 1479167
WHERE member_id = 310
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Ms Esther Wanjiku Wachira  (CSV: ESTHER WANJIKU WACHIRA)
UPDATE loans 
SET 
  amount = 1454645,
  outstanding_balance = 1418155,
  principal_repaid = 36490,
  interest_collected = 14546,
  original_principal = 1454645
WHERE member_id = 320
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Ms Mary Mutindi Kimanthi  (CSV: MARY MUTINDI KIMANTHI)
UPDATE loans 
SET 
  amount = 969756,
  outstanding_balance = 930366,
  principal_repaid = 39390,
  interest_collected = 9698,
  original_principal = 969756
WHERE member_id = 342
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- CHEGE SAMUEL KARARI  (CSV: CHEGE SAMUEL KARARI)
UPDATE loans 
SET 
  amount = 2661250,
  outstanding_balance = 2619668,
  principal_repaid = 41582,
  interest_collected = 26613,
  original_principal = 2661250
WHERE member_id = 345
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- DENIS MOSE TANGASO  (CSV: DENIS MOSE TANGASO)
UPDATE loans 
SET 
  amount = 502667,
  outstanding_balance = 489439,
  principal_repaid = 13228,
  interest_collected = 5027,
  original_principal = 502667
WHERE member_id = 361
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Mr Charles Njunge Mwaura  (CSV: CHARLES NJUNGE MWAURA)
UPDATE loans 
SET 
  amount = 328592,
  outstanding_balance = 316856,
  principal_repaid = 11735,
  interest_collected = 3286,
  original_principal = 328592
WHERE member_id = 364
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Mr Patrick Muli Kalumba  (CSV: PATRICK MULI KALUMBA)
UPDATE loans 
SET 
  amount = 1327519,
  outstanding_balance = 1260208,
  principal_repaid = 67311,
  interest_collected = 13275,
  original_principal = 1327519
WHERE member_id = 366
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Mr Marcos Otieno Anyumba  (CSV: MARCOS OTIENO ANYUMBA)
UPDATE loans 
SET 
  amount = 1722222,
  outstanding_balance = 1694444,
  principal_repaid = 27778,
  interest_collected = 17222,
  original_principal = 1722222
WHERE member_id = 368
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- LUNG'ATSO ASIYA ESIEMINYI  (CSV: LUNG'ATSO ASIYA ESIEMINYI)
UPDATE loans 
SET 
  amount = 269899,
  outstanding_balance = 261790,
  principal_repaid = 8109,
  interest_collected = 2699,
  original_principal = 269899
WHERE member_id = 370
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Peter Wanjohi Maina  (CSV: Peter Wanjohi Maina)
UPDATE loans 
SET 
  amount = 450000,
  outstanding_balance = 433333,
  principal_repaid = 16667,
  interest_collected = 4500,
  original_principal = 450000
WHERE member_id = 389
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Faith Atieno Nyaoro  (CSV: Faith Atieno Nyaoro)
UPDATE loans 
SET 
  amount = 35556,
  outstanding_balance = 33333,
  principal_repaid = 2222,
  interest_collected = 356,
  original_principal = 35556
WHERE member_id = 404
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Jorim Ochieng Awuor  (CSV: Jorim Ochieng Awuor)
UPDATE loans 
SET 
  amount = 179277,
  outstanding_balance = 176431,
  principal_repaid = 2846,
  interest_collected = 1793,
  original_principal = 179277
WHERE member_id = 406
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Kevin Njenga Nguyai  (CSV: Kevin Njenga Nguyai)
UPDATE loans 
SET 
  amount = 66667,
  outstanding_balance = 58333,
  principal_repaid = 8333,
  interest_collected = 667,
  original_principal = 66667
WHERE member_id = 412
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Glenn Runanu Kabiru  (CSV: Glenn Runanu Kabiru)
UPDATE loans 
SET 
  amount = 311275,
  outstanding_balance = 306710,
  principal_repaid = 4565,
  interest_collected = 3113,
  original_principal = 311275
WHERE member_id = 417
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Glenn Runanu Kabiru  Emergency 1 (CSV: Glenn Runanu Kabiru)
UPDATE loans 
SET 
  amount = 9167,
  outstanding_balance = 8333,
  principal_repaid = 833,
  interest_collected = 92,
  original_principal = 9167
WHERE member_id = 417
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Emergency%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Peter Mwangi Maina  (CSV: Peter Mwangi Maina)
UPDATE loans 
SET 
  amount = 51891,
  outstanding_balance = 49420,
  principal_repaid = 2471,
  interest_collected = 519,
  original_principal = 51891
WHERE member_id = 426
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

-- Levis Mwaniki Gaitho  (CSV: Levis Mwaniki Gaitho)
UPDATE loans 
SET 
  amount = 170000,
  outstanding_balance = 167639,
  principal_repaid = 2361,
  interest_collected = 1700,
  original_principal = 170000
WHERE member_id = 443
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

SET SQL_SAFE_UPDATES = 1;

-- Summary
SELECT 'Fix complete' AS Status;
SELECT COUNT(*) AS loans_updated, SUM(outstanding_balance) AS total_outstanding 
FROM loans WHERE outstanding_balance > 0;
