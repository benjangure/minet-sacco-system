USE minetsacco;
SET SQL_SAFE_UPDATES = 0;

-- ========================================
-- JANUARY 2026 - OPENING BALANCES
-- ========================================
-- Shares: B/F - 3000
-- Loans: C/D (Closing Down = Outstanding)
-- Generated: 08/07/2026 16:11:24

-- MBURU FREDRICK MAINA (1087) - Shares: 1699042 - 3000 = 1696042
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1696042
WHERE m.employee_id = '1087' AND a.account_type = 'SHARES';

-- MBURU FREDRICK MAINA (1087) - Normal Loan Outstanding: 2629100
UPDATE loans 
SET amount = 2629100, outstanding_balance = 2629100
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1087')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- WAITHAKA DAVID CHEGE (1191) - Shares: 7260000 - 3000 = 7257000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 7257000
WHERE m.employee_id = '1191' AND a.account_type = 'SHARES';

-- NDUTHU GABRIEL MAHUGU (1242) - Shares: 1773000 - 3000 = 1770000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1770000
WHERE m.employee_id = '1242' AND a.account_type = 'SHARES';

-- NDUTHU GABRIEL MAHUGU (1242) - Normal Loan Outstanding: 4959895
UPDATE loans 
SET amount = 4959895, outstanding_balance = 4959895
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1242')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- ONSANDO JOSEPH (1214) - Shares: 4343343 - 3000 = 4340343
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 4340343
WHERE m.employee_id = '1214' AND a.account_type = 'SHARES';

-- MUIRURI DAVID KAMAU (1297) - Shares: 1510500 - 3000 = 1507500
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1507500
WHERE m.employee_id = '1297' AND a.account_type = 'SHARES';

-- MUIRURI DAVID KAMAU (1297) - Normal Loan Outstanding: 3558333
UPDATE loans 
SET amount = 3558333, outstanding_balance = 3558333
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1297')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- ANASTASIA NYAMBURA KIMANI (13118) - Shares: 176000 - 3000 = 173000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 173000
WHERE m.employee_id = '13118' AND a.account_type = 'SHARES';

-- GANGLA JOHN OTIENO (2054) - Shares: 1165000 - 3000 = 1162000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1162000
WHERE m.employee_id = '2054' AND a.account_type = 'SHARES';

-- GANGLA JOHN OTIENO (2054) - Normal Loan Outstanding: 527346
UPDATE loans 
SET amount = 527346, outstanding_balance = 527346
WHERE member_id = (SELECT id FROM members WHERE employee_id = '2054')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- MAINA FRANCIS WACHIRA (2076) - Shares: 20134186 - 3000 = 20131186
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 20131186
WHERE m.employee_id = '2076' AND a.account_type = 'SHARES';

-- MUTHUI SAMMY (4044) - Shares: 4950000 - 3000 = 4947000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 4947000
WHERE m.employee_id = '4044' AND a.account_type = 'SHARES';

-- MUTHUI SAMMY (4044) - Normal Loan Outstanding: 5703323
UPDATE loans 
SET amount = 5703323, outstanding_balance = 5703323
WHERE member_id = (SELECT id FROM members WHERE employee_id = '4044')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- ERIC RUGO MUGO (5187) - Shares: 9003000 - 3000 = 9000000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 9000000
WHERE m.employee_id = '5187' AND a.account_type = 'SHARES';

-- NDERITU CAROLINE NJERI (6106) - Shares: 13587221 - 3000 = 13584221
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 13584221
WHERE m.employee_id = '6106' AND a.account_type = 'SHARES';

-- GITONGA TOBIAS MUGENDI (7139) - Shares: 3445000 - 3000 = 3442000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 3442000
WHERE m.employee_id = '7139' AND a.account_type = 'SHARES';

-- MBURU MONICA WAMBUI (7110) - Shares: 2355000 - 3000 = 2352000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 2352000
WHERE m.employee_id = '7110' AND a.account_type = 'SHARES';

-- MBURU MONICA WAMBUI (7110) - Normal Loan Outstanding: 2718750
UPDATE loans 
SET amount = 2718750, outstanding_balance = 2718750
WHERE member_id = (SELECT id FROM members WHERE employee_id = '7110')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- MACHARIA EDWIN MWANGI (9080) - Shares: 6055996 - 3000 = 6052996
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 6052996
WHERE m.employee_id = '9080' AND a.account_type = 'SHARES';

-- NJERU WINCATE MUKAMI (9150) - Shares: 1233000 - 3000 = 1230000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1230000
WHERE m.employee_id = '9150' AND a.account_type = 'SHARES';

-- NJERU WINCATE MUKAMI (9150) - Normal Loan Outstanding: 1077550
UPDATE loans 
SET amount = 1077550, outstanding_balance = 1077550
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9150')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- KEGODE EDWIN AGALOMBA (9132) - Shares: 4045000 - 3000 = 4042000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 4042000
WHERE m.employee_id = '9132' AND a.account_type = 'SHARES';

-- KEGODE EDWIN AGALOMBA (9132) - Normal Loan Outstanding: 1825592
UPDATE loans 
SET amount = 1825592, outstanding_balance = 1825592
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9132')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- THUMBI JUDE NDUNG'U (9113) - Shares: 1900000 - 3000 = 1897000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1897000
WHERE m.employee_id = '9113' AND a.account_type = 'SHARES';

-- THUMBI JUDE NDUNG'U (9113) - Normal Loan Outstanding: 1192634
UPDATE loans 
SET amount = 1192634, outstanding_balance = 1192634
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9113')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- KIMANI ROBERT MURIUKI (1105) - Shares: 1800000 - 3000 = 1797000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1797000
WHERE m.employee_id = '1105' AND a.account_type = 'SHARES';

-- MUTUNGA KATEE (1204) - Shares: 1350000 - 3000 = 1347000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1347000
WHERE m.employee_id = '1204' AND a.account_type = 'SHARES';

-- MUTUNGA KATEE (1204) - Normal Loan Outstanding: 232980
UPDATE loans 
SET amount = 232980, outstanding_balance = 232980
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1204')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- AIRO JOHN OYAMO (1203) - Shares: 240000 - 3000 = 237000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 237000
WHERE m.employee_id = '1203' AND a.account_type = 'SHARES';

-- AIRO JOHN OYAMO (1203) - Normal Loan Outstanding: 641417
UPDATE loans 
SET amount = 641417, outstanding_balance = 641417
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1203')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- AIRO JOHN OYAMO (1203) - Emergency 1: 33750
UPDATE loans 
SET amount = 33750, outstanding_balance = 33750
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1203')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Emergency%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- MWANGI JAYNE NJERI (1235) - Shares: 295000 - 3000 = 292000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 292000
WHERE m.employee_id = '1235' AND a.account_type = 'SHARES';

-- MWANGI JAYNE NJERI (1235) - Normal Loan Outstanding: 241475
UPDATE loans 
SET amount = 241475, outstanding_balance = 241475
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1235')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- MATIVO IRENE (1247) - Shares: 588000 - 3000 = 585000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 585000
WHERE m.employee_id = '1247' AND a.account_type = 'SHARES';

-- MATIVO IRENE (1247) - Normal Loan Outstanding: 888889
UPDATE loans 
SET amount = 888889, outstanding_balance = 888889
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1247')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- MUTURI FELISTA IGOKI (1264) - Shares: 772000 - 3000 = 769000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 769000
WHERE m.employee_id = '1264' AND a.account_type = 'SHARES';

-- KIMANI ALICE NJERI (1280) - Shares: 494000 - 3000 = 491000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 491000
WHERE m.employee_id = '1280' AND a.account_type = 'SHARES';

-- ONANI MICHAEL OWALO (13010) - Shares: 3804000 - 3000 = 3801000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 3801000
WHERE m.employee_id = '13010' AND a.account_type = 'SHARES';

-- NAIVASHA JANE (13016) - Shares: 923000 - 3000 = 920000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 920000
WHERE m.employee_id = '13016' AND a.account_type = 'SHARES';

-- NAIVASHA JANE (13016) - Normal Loan Outstanding: 660000
UPDATE loans 
SET amount = 660000, outstanding_balance = 660000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13016')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- NYAORO ANDREW OJUANG' (13017) - Shares: 68000 - 3000 = 65000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 65000
WHERE m.employee_id = '13017' AND a.account_type = 'SHARES';

-- KARUKI ESTHER WAMUTIRA (13018) - Shares: 265000 - 3000 = 262000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 262000
WHERE m.employee_id = '13018' AND a.account_type = 'SHARES';

-- KARUKI ESTHER WAMUTIRA (13018) - Emergency 1: 100000
UPDATE loans 
SET amount = 100000, outstanding_balance = 100000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13018')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Emergency%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- MWANGI LENET (13019) - Shares: 1001201 - 3000 = 998201
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 998201
WHERE m.employee_id = '13019' AND a.account_type = 'SHARES';

-- WANJOHI IRENE WANJIKU (13021) - Shares: 483050 - 3000 = 480050
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 480050
WHERE m.employee_id = '13021' AND a.account_type = 'SHARES';

-- WANJOHI IRENE WANJIKU (13021) - Normal Loan Outstanding: 962501
UPDATE loans 
SET amount = 962501, outstanding_balance = 962501
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13021')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- PERTET ESTHER SHIKU (13024) - Shares: 179000 - 3000 = 176000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 176000
WHERE m.employee_id = '13024' AND a.account_type = 'SHARES';

-- AWUOR EDNAH OKWIRI (13022) - Shares: 954745 - 3000 = 951745
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 951745
WHERE m.employee_id = '13022' AND a.account_type = 'SHARES';

-- AWUOR EDNAH OKWIRI (13022) - Normal Loan Outstanding: 564819
UPDATE loans 
SET amount = 564819, outstanding_balance = 564819
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13022')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- BUNYALI JULIUS HABAKKUK (13026) - Shares: 2175000 - 3000 = 2172000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 2172000
WHERE m.employee_id = '13026' AND a.account_type = 'SHARES';

-- BUNYALI JULIUS HABAKKUK (13026) - Normal Loan Outstanding: 435000
UPDATE loans 
SET amount = 435000, outstanding_balance = 435000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13026')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- ANGWENYI GLADYS KERUBO (13028) - Shares: 306000 - 3000 = 303000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 303000
WHERE m.employee_id = '13028' AND a.account_type = 'SHARES';

-- ANGWENYI GLADYS KERUBO (13028) - Normal Loan Outstanding: 100000
UPDATE loans 
SET amount = 100000, outstanding_balance = 100000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13028')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- IRENE MUTHONI MWANGI (13037) - Shares: 306000 - 3000 = 303000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 303000
WHERE m.employee_id = '13037' AND a.account_type = 'SHARES';

-- IRENE MUTHONI MWANGI (13037) - Normal Loan Outstanding: 751619
UPDATE loans 
SET amount = 751619, outstanding_balance = 751619
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13037')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- GENSON NJUE MBAE (13044) - Shares: 615000 - 3000 = 612000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 612000
WHERE m.employee_id = '13044' AND a.account_type = 'SHARES';

-- GENSON NJUE MBAE (13044) - Normal Loan Outstanding: 1673063
UPDATE loans 
SET amount = 1673063, outstanding_balance = 1673063
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13044')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- LIDIA AKELO (13043) - Shares: 278000 - 3000 = 275000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 275000
WHERE m.employee_id = '13043' AND a.account_type = 'SHARES';

-- LIDIA AKELO (13043) - Normal Loan Outstanding: 247626
UPDATE loans 
SET amount = 247626, outstanding_balance = 247626
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13043')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- BERITA JUDY MUMBE (13057) - Shares: 396000 - 3000 = 393000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 393000
WHERE m.employee_id = '13057' AND a.account_type = 'SHARES';

-- BERITA JUDY MUMBE (13057) - Normal Loan Outstanding: 182000
UPDATE loans 
SET amount = 182000, outstanding_balance = 182000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13057')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- CAROLINE KENDI ITONGA (13088) - Shares: 1044000 - 3000 = 1041000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1041000
WHERE m.employee_id = '13088' AND a.account_type = 'SHARES';

-- EVELYN NYAMBURA KIHARA (1313) - Shares: 409000 - 3000 = 406000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 406000
WHERE m.employee_id = '1313' AND a.account_type = 'SHARES';

-- MAKAU LYDIA RUGURU (1312) - Shares: 211000 - 3000 = 208000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 208000
WHERE m.employee_id = '1312' AND a.account_type = 'SHARES';

-- MAKAU LYDIA RUGURU (1312) - Normal Loan Outstanding: 186598
UPDATE loans 
SET amount = 186598, outstanding_balance = 186598
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1312')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- MAKAU LYDIA RUGURU (1312) - Emergency 1: 8334
UPDATE loans 
SET amount = 8334, outstanding_balance = 8334
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1312')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Emergency%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- JANET NJERI NDUNGU (1316) - Shares: 591000 - 3000 = 588000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 588000
WHERE m.employee_id = '1316' AND a.account_type = 'SHARES';

-- IGNATIUS SHISOKA MUYONGA (1320) - Shares: 59000 - 3000 = 56000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 56000
WHERE m.employee_id = '1320' AND a.account_type = 'SHARES';

-- JOYCE NJOKI MUYA (15097) - Shares: 580000 - 3000 = 577000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 577000
WHERE m.employee_id = '15097' AND a.account_type = 'SHARES';

-- ROSE WANJA KINYATI (15124) - Shares: 258000 - 3000 = 255000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 255000
WHERE m.employee_id = '15124' AND a.account_type = 'SHARES';

-- SERAPHINE ANYANGA OKUMU (15146) - Shares: 315000 - 3000 = 312000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 312000
WHERE m.employee_id = '15146' AND a.account_type = 'SHARES';

-- NICKSON MASITA ONG'ERA (15248) - Shares: 594500 - 3000 = 591500
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 591500
WHERE m.employee_id = '15248' AND a.account_type = 'SHARES';

-- NICKSON MASITA ONG'ERA (15248) - Normal Loan Outstanding: 1458333
UPDATE loans 
SET amount = 1458333, outstanding_balance = 1458333
WHERE member_id = (SELECT id FROM members WHERE employee_id = '15248')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- KABUGI ANNE JATIAGA (2068) - Shares: 1155000 - 3000 = 1152000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1152000
WHERE m.employee_id = '2068' AND a.account_type = 'SHARES';

-- KABUGI ANNE JATIAGA (2068) - Normal Loan Outstanding: 1312274
UPDATE loans 
SET amount = 1312274, outstanding_balance = 1312274
WHERE member_id = (SELECT id FROM members WHERE employee_id = '2068')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- OBONYO SYDNEY MATHEW (2072) - Shares: 721500 - 3000 = 718500
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 718500
WHERE m.employee_id = '2072' AND a.account_type = 'SHARES';

-- OBONYO SYDNEY MATHEW (2072) - Normal Loan Outstanding: 1193334
UPDATE loans 
SET amount = 1193334, outstanding_balance = 1193334
WHERE member_id = (SELECT id FROM members WHERE employee_id = '2072')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- MACHARIA RUTH WAITHIRA (2073) - Shares: 309000 - 3000 = 306000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 306000
WHERE m.employee_id = '2073' AND a.account_type = 'SHARES';

-- MUSAO LEONARD OKUMU (2069) - Shares: 935000 - 3000 = 932000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 932000
WHERE m.employee_id = '2069' AND a.account_type = 'SHARES';

-- MUSAO LEONARD OKUMU (2069) - Normal Loan Outstanding: 2372409
UPDATE loans 
SET amount = 2372409, outstanding_balance = 2372409
WHERE member_id = (SELECT id FROM members WHERE employee_id = '2069')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- MWAURA HANNAH NYAMBURA (4033) - Shares: 780000 - 3000 = 777000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 777000
WHERE m.employee_id = '4033' AND a.account_type = 'SHARES';

-- KIMANI GLADYS WANGARI (4064) - Shares: 231000 - 3000 = 228000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 228000
WHERE m.employee_id = '4064' AND a.account_type = 'SHARES';

-- KIMANI GLADYS WANGARI (4064) - Normal Loan Outstanding: 303889
UPDATE loans 
SET amount = 303889, outstanding_balance = 303889
WHERE member_id = (SELECT id FROM members WHERE employee_id = '4064')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- MUTHAURA ROSE WANJA (5178) - Shares: 1079333 - 3000 = 1076333
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1076333
WHERE m.employee_id = '5178' AND a.account_type = 'SHARES';

-- MUTHAURA ROSE WANJA (5178) - Normal Loan Outstanding: 751667
UPDATE loans 
SET amount = 751667, outstanding_balance = 751667
WHERE member_id = (SELECT id FROM members WHERE employee_id = '5178')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- ESTHER WANJIKU WACHIRA (5190) - Shares: 860000 - 3000 = 857000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 857000
WHERE m.employee_id = '5190' AND a.account_type = 'SHARES';

-- ESTHER WANJIKU WACHIRA (5190) - Normal Loan Outstanding: 1418155
UPDATE loans 
SET amount = 1418155, outstanding_balance = 1418155
WHERE member_id = (SELECT id FROM members WHERE employee_id = '5190')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- AMASA FRANKLIN KIVARA (6152) - Shares: 174000 - 3000 = 171000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 171000
WHERE m.employee_id = '6152' AND a.account_type = 'SHARES';

-- AMASA FRANKLIN KIVARA (6152) - Normal Loan Outstanding: 115780
UPDATE loans 
SET amount = 115780, outstanding_balance = 115780
WHERE member_id = (SELECT id FROM members WHERE employee_id = '6152')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- ABDALLA ABDULMAJID MBARUK (8055) - Shares: 1080000 - 3000 = 1077000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1077000
WHERE m.employee_id = '8055' AND a.account_type = 'SHARES';

-- MWAKIO JEFFERSON MWAINGE (8036) - Shares: 974500 - 3000 = 971500
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 971500
WHERE m.employee_id = '8036' AND a.account_type = 'SHARES';

-- MWAKIO JEFFERSON MWAINGE (8036) - Normal Loan Outstanding: 30000
UPDATE loans 
SET amount = 30000, outstanding_balance = 30000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '8036')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- MACHI IRENE KITAWA (8028) - Shares: 735000 - 3000 = 732000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 732000
WHERE m.employee_id = '8028' AND a.account_type = 'SHARES';

-- KING'ARA LUCY WANJIRU (8054) - Shares: 1106000 - 3000 = 1103000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1103000
WHERE m.employee_id = '8054' AND a.account_type = 'SHARES';

-- KING'ARA LUCY WANJIRU (8054) - Normal Loan Outstanding: 498599
UPDATE loans 
SET amount = 498599, outstanding_balance = 498599
WHERE member_id = (SELECT id FROM members WHERE employee_id = '8054')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- AWITI GRACE LYNETTE (9037) - Shares: 363000 - 3000 = 360000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 360000
WHERE m.employee_id = '9037' AND a.account_type = 'SHARES';

-- Migwi CHARLOTTE WANJIKU (9041) - Shares: 1037400 - 3000 = 1034400
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1034400
WHERE m.employee_id = '9041' AND a.account_type = 'SHARES';

-- Migwi CHARLOTTE WANJIKU (9041) - Normal Loan Outstanding: 940000
UPDATE loans 
SET amount = 940000, outstanding_balance = 940000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9041')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- ADELE MACHARIA LINDA WANGUI (9084) - Shares: 478000 - 3000 = 475000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 475000
WHERE m.employee_id = '9084' AND a.account_type = 'SHARES';

-- NGURE RACHAEL WANJIKU (9098) - Shares: 429000 - 3000 = 426000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 426000
WHERE m.employee_id = '9098' AND a.account_type = 'SHARES';

-- NGURE RACHAEL WANJIKU (9098) - Normal Loan Outstanding: 1168210
UPDATE loans 
SET amount = 1168210, outstanding_balance = 1168210
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9098')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- KINOTI LINDA GATWIRI (9105) - Shares: 522000 - 3000 = 519000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 519000
WHERE m.employee_id = '9105' AND a.account_type = 'SHARES';

-- KINOTI LINDA GATWIRI (9105) - Normal Loan Outstanding: 210833
UPDATE loans 
SET amount = 210833, outstanding_balance = 210833
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9105')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- ATIENO NAOMI JUDITH (9108) - Shares: 742000 - 3000 = 739000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 739000
WHERE m.employee_id = '9108' AND a.account_type = 'SHARES';

-- ATIENO NAOMI JUDITH (9108) - Normal Loan Outstanding: 1263239
UPDATE loans 
SET amount = 1263239, outstanding_balance = 1263239
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9108')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- GICHURI JERIOTH MUTHONI (9122) - Shares: 864000 - 3000 = 861000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 861000
WHERE m.employee_id = '9122' AND a.account_type = 'SHARES';

-- GICHURI JERIOTH MUTHONI (9122) - Normal Loan Outstanding: 398576
UPDATE loans 
SET amount = 398576, outstanding_balance = 398576
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9122')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- NDUNDA PERPETUA WANZA (9181) - Shares: 1394000 - 3000 = 1391000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1391000
WHERE m.employee_id = '9181' AND a.account_type = 'SHARES';

-- NDUNDA PERPETUA WANZA (9181) - Normal Loan Outstanding: 1440000
UPDATE loans 
SET amount = 1440000, outstanding_balance = 1440000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9181')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- WERE LORINE AKOTH (9183) - Shares: 499000 - 3000 = 496000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 496000
WHERE m.employee_id = '9183' AND a.account_type = 'SHARES';

-- WERE LORINE AKOTH (9183) - Normal Loan Outstanding: 395761
UPDATE loans 
SET amount = 395761, outstanding_balance = 395761
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9183')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- WERE LORINE AKOTH (9183) - Emergency 1: 110000
UPDATE loans 
SET amount = 110000, outstanding_balance = 110000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9183')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Emergency%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- MATALANGA JOHN KAMAU (9198) - Shares: 723250 - 3000 = 720250
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 720250
WHERE m.employee_id = '9198' AND a.account_type = 'SHARES';

-- ALWODI MARK MUNAVI (9212) - Shares: 248000 - 3000 = 245000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 245000
WHERE m.employee_id = '9212' AND a.account_type = 'SHARES';

-- ALWODI MARK MUNAVI (9212) - Normal Loan Outstanding: 570000
UPDATE loans 
SET amount = 570000, outstanding_balance = 570000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9212')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- RAPONGO COLLINS BWIRE (9291) - Shares: 252000 - 3000 = 249000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 249000
WHERE m.employee_id = '9291' AND a.account_type = 'SHARES';

-- MARY MUTINDI KIMANTHI (9303) - Shares: 610000 - 3000 = 607000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 607000
WHERE m.employee_id = '9303' AND a.account_type = 'SHARES';

-- MARY MUTINDI KIMANTHI (9303) - Normal Loan Outstanding: 930366
UPDATE loans 
SET amount = 930366, outstanding_balance = 930366
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9303')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- FLORENCE MWENDE WAMBU (9323) - Shares: 214000 - 3000 = 211000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 211000
WHERE m.employee_id = '9323' AND a.account_type = 'SHARES';

-- CHEGE SAMUEL KARARI (1146) - Shares: 1641025 - 3000 = 1638025
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1638025
WHERE m.employee_id = '1146' AND a.account_type = 'SHARES';

-- CHEGE SAMUEL KARARI (1146) - Normal Loan Outstanding: 2619668
UPDATE loans 
SET amount = 2619668, outstanding_balance = 2619668
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1146')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- JOYCE GATWIRI KITHINJI (15253) - Shares: 308000 - 3000 = 305000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 305000
WHERE m.employee_id = '15253' AND a.account_type = 'SHARES';

-- MAINGA DANIEL LOTI (6109) - Shares: 4099000 - 3000 = 4096000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 4096000
WHERE m.employee_id = '6109' AND a.account_type = 'SHARES';

-- MAINGA DANIEL LOTI (6109) - Normal Loan Outstanding: 2212500
UPDATE loans 
SET amount = 2212500, outstanding_balance = 2212500
WHERE member_id = (SELECT id FROM members WHERE employee_id = '6109')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- ROTICH ROBERT ALEX (6075) - Shares: 6038400 - 3000 = 6035400
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 6035400
WHERE m.employee_id = '6075' AND a.account_type = 'SHARES';

-- CHRISTINE C MUTHONI MURIITHI (15230) - Shares: 297000 - 3000 = 294000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 294000
WHERE m.employee_id = '15230' AND a.account_type = 'SHARES';

-- GITAU MARY WAIRIMU (6092) - Shares: 1180000 - 3000 = 1177000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1177000
WHERE m.employee_id = '6092' AND a.account_type = 'SHARES';

-- GITAU MARY WAIRIMU (6092) - Normal Loan Outstanding: 3483562
UPDATE loans 
SET amount = 3483562, outstanding_balance = 3483562
WHERE member_id = (SELECT id FROM members WHERE employee_id = '6092')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- MAINA STEPHEN IRUNGU (6114) - Shares: 1106000 - 3000 = 1103000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1103000
WHERE m.employee_id = '6114' AND a.account_type = 'SHARES';

-- MAINA STEPHEN IRUNGU (6114) - Normal Loan Outstanding: 750225
UPDATE loans 
SET amount = 750225, outstanding_balance = 750225
WHERE member_id = (SELECT id FROM members WHERE employee_id = '6114')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- BUURI PAMELA MWENDE (6143) - Shares: 240000 - 3000 = 237000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 237000
WHERE m.employee_id = '6143' AND a.account_type = 'SHARES';

-- MURIITHI ROSE WANGARI (6136) - Shares: 646000 - 3000 = 643000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 643000
WHERE m.employee_id = '6136' AND a.account_type = 'SHARES';

-- MURIITHI ROSE WANGARI (6136) - Normal Loan Outstanding: 53333
UPDATE loans 
SET amount = 53333, outstanding_balance = 53333
WHERE member_id = (SELECT id FROM members WHERE employee_id = '6136')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- NARANGWI PETER KIBUINE (12002) - Shares: 11884732 - 3000 = 11881732
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 11881732
WHERE m.employee_id = '12002' AND a.account_type = 'SHARES';

-- MICHAEL NG'ANG'A KAMAU (6183) - Shares: 35000 - 3000 = 32000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 32000
WHERE m.employee_id = '6183' AND a.account_type = 'SHARES';

-- KINYUA FRIDAH NAITORE (12006) - Shares: 1182000 - 3000 = 1179000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1179000
WHERE m.employee_id = '12006' AND a.account_type = 'SHARES';

-- DENIS MOSE TANGASO (12007) - Shares: 169000 - 3000 = 166000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 166000
WHERE m.employee_id = '12007' AND a.account_type = 'SHARES';

-- DENIS MOSE TANGASO (12007) - Normal Loan Outstanding: 489439
UPDATE loans 
SET amount = 489439, outstanding_balance = 489439
WHERE member_id = (SELECT id FROM members WHERE employee_id = '12007')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- TEDDY AYODI (6095) - Shares: 9770000 - 3000 = 9767000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 9767000
WHERE m.employee_id = '6095' AND a.account_type = 'SHARES';

-- MWAGI JOSEPH ONYANGO (9125) - Shares: 1816000 - 3000 = 1813000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1813000
WHERE m.employee_id = '9125' AND a.account_type = 'SHARES';

-- CHARLES NJUNGE MWAURA (9308) - Shares: 475000 - 3000 = 472000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 472000
WHERE m.employee_id = '9308' AND a.account_type = 'SHARES';

-- CHARLES NJUNGE MWAURA (9308) - Normal Loan Outstanding: 316856
UPDATE loans 
SET amount = 316856, outstanding_balance = 316856
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9308')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- PATRICK MULI KALUMBA (5198) - Shares: 979500 - 3000 = 976500
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 976500
WHERE m.employee_id = '5198' AND a.account_type = 'SHARES';

-- PATRICK MULI KALUMBA (5198) - Normal Loan Outstanding: 1260208
UPDATE loans 
SET amount = 1260208, outstanding_balance = 1260208
WHERE member_id = (SELECT id FROM members WHERE employee_id = '5198')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- MARCOS OTIENO ANYUMBA (1206) - Shares: 1103000 - 3000 = 1100000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1100000
WHERE m.employee_id = '1206' AND a.account_type = 'SHARES';

-- MARCOS OTIENO ANYUMBA (1206) - Normal Loan Outstanding: 1694444
UPDATE loans 
SET amount = 1694444, outstanding_balance = 1694444
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1206')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- NALWENJE ALEX OKOTH (1299) - Shares: 78000 - 3000 = 75000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 75000
WHERE m.employee_id = '1299' AND a.account_type = 'SHARES';

-- LUNG'ATSO ASIYA ESIEMINYI (13054) - Shares: 263000 - 3000 = 260000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 260000
WHERE m.employee_id = '13054' AND a.account_type = 'SHARES';

-- LUNG'ATSO ASIYA ESIEMINYI (13054) - Normal Loan Outstanding: 261790
UPDATE loans 
SET amount = 261790, outstanding_balance = 261790
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13054')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- EUNICE WAMBUI NJOGU (15260) - Shares: 247500 - 3000 = 244500
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 244500
WHERE m.employee_id = '15260' AND a.account_type = 'SHARES';

-- Mr Simeon Odhiambo Owino (1327) - Shares: 158000 - 3000 = 155000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 155000
WHERE m.employee_id = '1327' AND a.account_type = 'SHARES';

-- Mr Andrew Gitau Githua Kaminja (9341) - Shares: 141000 - 3000 = 138000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 138000
WHERE m.employee_id = '9341' AND a.account_type = 'SHARES';

-- Mr Andrew Gitau Githua Kaminja (9341) - Normal Loan Outstanding: 110833
UPDATE loans 
SET amount = 110833, outstanding_balance = 110833
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9341')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Ms Ann Wandia Wangu (15037) - Shares: 98000 - 3000 = 95000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 95000
WHERE m.employee_id = '15037' AND a.account_type = 'SHARES';

-- Ms Ann Wandia Wangu (15037) - Normal Loan Outstanding: 226667
UPDATE loans 
SET amount = 226667, outstanding_balance = 226667
WHERE member_id = (SELECT id FROM members WHERE employee_id = '15037')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Mr Edwin Were Saaya (5216) - Shares: 540000 - 3000 = 537000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 537000
WHERE m.employee_id = '5216' AND a.account_type = 'SHARES';

-- Mr Edwin Were Saaya (5216) - Normal Loan Outstanding: 600000
UPDATE loans 
SET amount = 600000, outstanding_balance = 600000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '5216')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Mr Kelvin Mulinge Kyalo (15012) - Shares: 140000 - 3000 = 137000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 137000
WHERE m.employee_id = '15012' AND a.account_type = 'SHARES';

-- Ms Claire Wairimu Kinyanjui (1274) - Shares: 175000 - 3000 = 172000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 172000
WHERE m.employee_id = '1274' AND a.account_type = 'SHARES';

-- TITUS THUMBI MURIUKI (9296) - Shares: 630000 - 3000 = 627000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 627000
WHERE m.employee_id = '9296' AND a.account_type = 'SHARES';

-- JUWEIRIYA ABDALLLA (13113) - Shares: 265000 - 3000 = 262000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 262000
WHERE m.employee_id = '13113' AND a.account_type = 'SHARES';

-- MOSES KURIA (1332) - Shares: 420000 - 3000 = 417000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 417000
WHERE m.employee_id = '1332' AND a.account_type = 'SHARES';

-- MOSES KURIA (1332) - Normal Loan Outstanding: 1145537
UPDATE loans 
SET amount = 1145537, outstanding_balance = 1145537
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1332')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Mr George Mbugua Kariuki (9099) - Shares: 315000 - 3000 = 312000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 312000
WHERE m.employee_id = '9099' AND a.account_type = 'SHARES';

-- Mr George Mbugua Kariuki (9099) - Normal Loan Outstanding: 850000
UPDATE loans 
SET amount = 850000, outstanding_balance = 850000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9099')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Ms Caroline Nekesa (4157) - Shares: 310000 - 3000 = 307000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 307000
WHERE m.employee_id = '4157' AND a.account_type = 'SHARES';

-- Ms Teresia Trizer Wamucii (9229) - Shares: 320000 - 3000 = 317000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 317000
WHERE m.employee_id = '9229' AND a.account_type = 'SHARES';

-- Peter Wanjohi Maina (12010) - Shares: 1385000 - 3000 = 1382000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 1382000
WHERE m.employee_id = '12010' AND a.account_type = 'SHARES';

-- Peter Wanjohi Maina (12010) - Normal Loan Outstanding: 433333
UPDATE loans 
SET amount = 433333, outstanding_balance = 433333
WHERE member_id = (SELECT id FROM members WHERE employee_id = '12010')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Ms Mercy Gathoni Muriithi (13068) - Shares: 88000 - 3000 = 85000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 85000
WHERE m.employee_id = '13068' AND a.account_type = 'SHARES';

-- Mercy Muringi Muthoga (9344) - Shares: 130000 - 3000 = 127000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 127000
WHERE m.employee_id = '9344' AND a.account_type = 'SHARES';

-- Mr Peter Mwiti Kamundi (13061) - Shares: 91500 - 3000 = 88500
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 88500
WHERE m.employee_id = '13061' AND a.account_type = 'SHARES';

-- Mr Peter Mwiti Kamundi (13061) - Normal Loan Outstanding: 269471
UPDATE loans 
SET amount = 269471, outstanding_balance = 269471
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13061')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Winrose Miroyo Ondego (5226) - Shares: 41000 - 3000 = 38000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 38000
WHERE m.employee_id = '5226' AND a.account_type = 'SHARES';

-- Mr Stephen Ngugi Muriu (6117) - Shares: 548000 - 3000 = 545000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 545000
WHERE m.employee_id = '6117' AND a.account_type = 'SHARES';

-- Mr Stephen Ngugi Muriu (6117) - Normal Loan Outstanding: 1364000
UPDATE loans 
SET amount = 1364000, outstanding_balance = 1364000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '6117')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Mr Eric Atinda Orina (9289) - Shares: 79000 - 3000 = 76000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 76000
WHERE m.employee_id = '9289' AND a.account_type = 'SHARES';

-- Mr Eric Atinda Orina (9289) - Normal Loan Outstanding: 95407
UPDATE loans 
SET amount = 95407, outstanding_balance = 95407
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9289')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Ms Jane Gakii Miriti (5159) - Shares: 75000 - 3000 = 72000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 72000
WHERE m.employee_id = '5159' AND a.account_type = 'SHARES';

-- Ms Jane Gakii Miriti (5159) - Normal Loan Outstanding: 87744
UPDATE loans 
SET amount = 87744, outstanding_balance = 87744
WHERE member_id = (SELECT id FROM members WHERE employee_id = '5159')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Ms Elizabeth Nekesa Sitati (15045) - Shares: 206000 - 3000 = 203000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 203000
WHERE m.employee_id = '15045' AND a.account_type = 'SHARES';

-- Ms Elizabeth Nekesa Sitati (15045) - Normal Loan Outstanding: 236250
UPDATE loans 
SET amount = 236250, outstanding_balance = 236250
WHERE member_id = (SELECT id FROM members WHERE employee_id = '15045')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Pauline Waithira Mwaura (5223) - Shares: 170000 - 3000 = 167000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 167000
WHERE m.employee_id = '5223' AND a.account_type = 'SHARES';

-- Kenedy Kirimi Mati (15319) - Shares: 110000 - 3000 = 107000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 107000
WHERE m.employee_id = '15319' AND a.account_type = 'SHARES';

-- Gibson Oguda Mbaja (1339) - Shares: 60000 - 3000 = 57000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 57000
WHERE m.employee_id = '1339' AND a.account_type = 'SHARES';

-- Mr James Maina Kimani (5191) - Shares: 402000 - 3000 = 399000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 399000
WHERE m.employee_id = '5191' AND a.account_type = 'SHARES';

-- Mr James Maina Kimani (5191) - Normal Loan Outstanding: 391667
UPDATE loans 
SET amount = 391667, outstanding_balance = 391667
WHERE member_id = (SELECT id FROM members WHERE employee_id = '5191')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Faith Atieno Nyaoro (1341) - Shares: 44000 - 3000 = 41000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 41000
WHERE m.employee_id = '1341' AND a.account_type = 'SHARES';

-- Faith Atieno Nyaoro (1341) - Normal Loan Outstanding: 33333
UPDATE loans 
SET amount = 33333, outstanding_balance = 33333
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1341')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Mr Stephen Ndavuti Ndunda (9272) - Shares: 9000 - 3000 = 6000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 6000
WHERE m.employee_id = '9272' AND a.account_type = 'SHARES';

-- Jorim Ochieng Awuor (1338) - Shares: 60000 - 3000 = 57000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 57000
WHERE m.employee_id = '1338' AND a.account_type = 'SHARES';

-- Jorim Ochieng Awuor (1338) - Normal Loan Outstanding: 176431
UPDATE loans 
SET amount = 176431, outstanding_balance = 176431
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1338')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Mr Joram Katiwa Mutunga (6182) - Shares: 205000 - 3000 = 202000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 202000
WHERE m.employee_id = '6182' AND a.account_type = 'SHARES';

-- Mr Joram Katiwa Mutunga (6182) - Normal Loan Outstanding: 104683
UPDATE loans 
SET amount = 104683, outstanding_balance = 104683
WHERE member_id = (SELECT id FROM members WHERE employee_id = '6182')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Mr Walter Kipkurui Koech (1265) - Shares: 30000 - 3000 = 27000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 27000
WHERE m.employee_id = '1265' AND a.account_type = 'SHARES';

-- Mr Walter Kipkurui Koech (1265) - Normal Loan Outstanding: 37778
UPDATE loans 
SET amount = 37778, outstanding_balance = 37778
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1265')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Mr Andrew Muhia Kagiri (1331) - Shares: 250000 - 3000 = 247000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 247000
WHERE m.employee_id = '1331' AND a.account_type = 'SHARES';

-- Mr Andrew Muhia Kagiri (1331) - Normal Loan Outstanding: 537463
UPDATE loans 
SET amount = 537463, outstanding_balance = 537463
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1331')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Mr Dennis William Mukwanja (1326) - Shares: 57000 - 3000 = 54000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 54000
WHERE m.employee_id = '1326' AND a.account_type = 'SHARES';

-- Mr Dennis William Mukwanja (1326) - Normal Loan Outstanding: 30000
UPDATE loans 
SET amount = 30000, outstanding_balance = 30000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1326')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Miss Dorcas Wanjugu Maina (15016) - Shares: 57000 - 3000 = 54000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 54000
WHERE m.employee_id = '15016' AND a.account_type = 'SHARES';

-- Kevin Njenga Nguyai (5224) - Shares: 57000 - 3000 = 54000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 54000
WHERE m.employee_id = '5224' AND a.account_type = 'SHARES';

-- Kevin Njenga Nguyai (5224) - Normal Loan Outstanding: 58333
UPDATE loans 
SET amount = 58333, outstanding_balance = 58333
WHERE member_id = (SELECT id FROM members WHERE employee_id = '5224')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Ms Naomi Wangui Nganga (9300) - Shares: 98000 - 3000 = 95000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 95000
WHERE m.employee_id = '9300' AND a.account_type = 'SHARES';

-- Ms Naomi Wangui Nganga (9300) - Normal Loan Outstanding: 197046
UPDATE loans 
SET amount = 197046, outstanding_balance = 197046
WHERE member_id = (SELECT id FROM members WHERE employee_id = '9300')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Mrs Elizabeth Muthoni Karanja (13094) - Shares: 57000 - 3000 = 54000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 54000
WHERE m.employee_id = '13094' AND a.account_type = 'SHARES';

-- Ms Doreen Muthoni Mpangua (1277) - Shares: 60000 - 3000 = 57000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 57000
WHERE m.employee_id = '1277' AND a.account_type = 'SHARES';

-- Glenn Runanu Kabiru (6210) - Shares: 131000 - 3000 = 128000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 128000
WHERE m.employee_id = '6210' AND a.account_type = 'SHARES';

-- Glenn Runanu Kabiru (6210) - Normal Loan Outstanding: 306710
UPDATE loans 
SET amount = 306710, outstanding_balance = 306710
WHERE member_id = (SELECT id FROM members WHERE employee_id = '6210')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Glenn Runanu Kabiru (6210) - Emergency 1: 8333
UPDATE loans 
SET amount = 8333, outstanding_balance = 8333
WHERE member_id = (SELECT id FROM members WHERE employee_id = '6210')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Emergency%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Mr Benson Waweru Muriuki (13109) - Shares: 83500 - 3000 = 80500
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 80500
WHERE m.employee_id = '13109' AND a.account_type = 'SHARES';

-- Mr Benson Waweru Muriuki (13109) - Normal Loan Outstanding: 129854
UPDATE loans 
SET amount = 129854, outstanding_balance = 129854
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13109')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Joshua Wafula Kakai (9342) - Shares: 85000 - 3000 = 82000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 82000
WHERE m.employee_id = '9342' AND a.account_type = 'SHARES';

-- Mr Reynold Onyango Oketch (13093) - Shares: 51000 - 3000 = 48000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 48000
WHERE m.employee_id = '13093' AND a.account_type = 'SHARES';

-- Felix Ochieng Otieno (9352) - Shares: 48000 - 3000 = 45000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 45000
WHERE m.employee_id = '9352' AND a.account_type = 'SHARES';

-- Mr Anthony Musembi Ndambuki (6196) - Shares: 160000 - 3000 = 157000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 157000
WHERE m.employee_id = '6196' AND a.account_type = 'SHARES';

-- Mr Edwin Kimathi Mwenda (4152) - Shares: 536000 - 3000 = 533000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 533000
WHERE m.employee_id = '4152' AND a.account_type = 'SHARES';

-- Mr Edwin Kimathi Mwenda (4152) - Normal Loan Outstanding: 1200000
UPDATE loans 
SET amount = 1200000, outstanding_balance = 1200000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '4152')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Edward  Jenings Koganga (9350) - Shares: 60000 - 3000 = 57000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 57000
WHERE m.employee_id = '9350' AND a.account_type = 'SHARES';

-- Peter Mwangi Maina (1336) - Shares: 41500 - 3000 = 38500
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 38500
WHERE m.employee_id = '1336' AND a.account_type = 'SHARES';

-- Peter Mwangi Maina (1336) - Normal Loan Outstanding: 49420
UPDATE loans 
SET amount = 49420, outstanding_balance = 49420
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1336')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Mrs Rachel Nkatha Mwenda (13114) - Shares: 120000 - 3000 = 117000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 117000
WHERE m.employee_id = '13114' AND a.account_type = 'SHARES';

-- Mrs Rachel Nkatha Mwenda (13114) - Normal Loan Outstanding: 262500
UPDATE loans 
SET amount = 262500, outstanding_balance = 262500
WHERE member_id = (SELECT id FROM members WHERE employee_id = '13114')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Joseph Oduory Ouma (6205) - Shares: 110000 - 3000 = 107000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 107000
WHERE m.employee_id = '6205' AND a.account_type = 'SHARES';

-- Evans Kipkorir Yegon (15353) - Shares: 72000 - 3000 = 69000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 69000
WHERE m.employee_id = '15353' AND a.account_type = 'SHARES';

-- Miss Grace Wanjiru Muthii (15105) - Shares: 60000 - 3000 = 57000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 57000
WHERE m.employee_id = '15105' AND a.account_type = 'SHARES';

-- Miss Grace Wanjiru Muthii (15105) - Normal Loan Outstanding: 155050
UPDATE loans 
SET amount = 155050, outstanding_balance = 155050
WHERE member_id = (SELECT id FROM members WHERE employee_id = '15105')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Margaret Wanini Munji (5336) - Shares: 86000 - 3000 = 83000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 83000
WHERE m.employee_id = '5336' AND a.account_type = 'SHARES';

-- Mr Japheth Kanyoo Matheka (5207) - Shares: 80500 - 3000 = 77500
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 77500
WHERE m.employee_id = '5207' AND a.account_type = 'SHARES';

-- Mr Japheth Kanyoo Matheka (5207) - Normal Loan Outstanding: 130000
UPDATE loans 
SET amount = 130000, outstanding_balance = 130000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '5207')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Maria Kalumu Isaac (9367) - Shares: 30000 - 3000 = 27000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 27000
WHERE m.employee_id = '9367' AND a.account_type = 'SHARES';

-- Mercy Mwende Kitivo (6208) - Shares: 45000 - 3000 = 42000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 42000
WHERE m.employee_id = '6208' AND a.account_type = 'SHARES';

-- Mr Robson Chege Mburu (1289) - Shares: 64000 - 3000 = 61000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 61000
WHERE m.employee_id = '1289' AND a.account_type = 'SHARES';

-- Mr Robson Chege Mburu (1289) - Emergency 1: 22500
UPDATE loans 
SET amount = 22500, outstanding_balance = 22500
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1289')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Emergency%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Miss Josephine Kavata Kioko (9311) - Shares: 96000 - 3000 = 93000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 93000
WHERE m.employee_id = '9311' AND a.account_type = 'SHARES';

-- Ms Cecelina Gacheri Mwobobia (15128) - Shares: 680000 - 3000 = 677000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 677000
WHERE m.employee_id = '15128' AND a.account_type = 'SHARES';

-- Daniel Muasya Muasa (5231) - Shares: 30000 - 3000 = 27000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 27000
WHERE m.employee_id = '5231' AND a.account_type = 'SHARES';

-- Alex Wamae Gatua (13130) - Shares: 15000 - 3000 = 12000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 12000
WHERE m.employee_id = '13130' AND a.account_type = 'SHARES';

-- Levis Mwaniki Gaitho (1347) - Shares: 55000 - 3000 = 52000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 52000
WHERE m.employee_id = '1347' AND a.account_type = 'SHARES';

-- Levis Mwaniki Gaitho (1347) - Normal Loan Outstanding: 167639
UPDATE loans 
SET amount = 167639, outstanding_balance = 167639
WHERE member_id = (SELECT id FROM members WHERE employee_id = '1347')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Mr Alex Njenga Mwai (6171) - Shares: 49500 - 3000 = 46500
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 46500
WHERE m.employee_id = '6171' AND a.account_type = 'SHARES';

-- Joseph Gituma (5225) - Shares: 125000 - 3000 = 122000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 122000
WHERE m.employee_id = '5225' AND a.account_type = 'SHARES';

-- Mr Joseph Muigai Wainaina (7103) - Shares: 403000 - 3000 = 400000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 400000
WHERE m.employee_id = '7103' AND a.account_type = 'SHARES';

-- Cyrus Mwaura Njoroge (5337) - Shares: 40000 - 3000 = 37000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 37000
WHERE m.employee_id = '5337' AND a.account_type = 'SHARES';

-- Ms Hadija Duba (9213) - Shares: 12000 - 3000 = 9000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 9000
WHERE m.employee_id = '9213' AND a.account_type = 'SHARES';

-- Mwihaki Kabura (6215) - Shares: 40000 - 3000 = 37000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 37000
WHERE m.employee_id = '6215' AND a.account_type = 'SHARES';

-- Miss Jackline Mwihaki Makumi (1330) - Shares: 9000 - 3000 = 6000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 6000
WHERE m.employee_id = '1330' AND a.account_type = 'SHARES';

-- Abigail Sakini Wabwoba (5243) - Shares: 15000 - 3000 = 12000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 12000
WHERE m.employee_id = '5243' AND a.account_type = 'SHARES';

-- Benard Muthiani Kasuni (1342) - Shares: 15000 - 3000 = 12000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 12000
WHERE m.employee_id = '1342' AND a.account_type = 'SHARES';

-- Mr Gideon Kipkirui Bii (9295) - Shares: 78000 - 3000 = 75000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 75000
WHERE m.employee_id = '9295' AND a.account_type = 'SHARES';

-- Loice Buyaki Momanyi (13125) - Shares: 30000 - 3000 = 27000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 27000
WHERE m.employee_id = '13125' AND a.account_type = 'SHARES';

-- Mr Alvin Mukubwa Kituyi (6222) - Shares: 6000 - 3000 = 3000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 3000
WHERE m.employee_id = '6222' AND a.account_type = 'SHARES';

-- Mr Alvin Mukubwa Kituyi (6222) - Normal Loan Outstanding: 55000
UPDATE loans 
SET amount = 55000, outstanding_balance = 55000
WHERE member_id = (SELECT id FROM members WHERE employee_id = '6222')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

-- Lucy Njeri Njroge (1351) - Shares: 6000 - 3000 = 3000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 3000
WHERE m.employee_id = '1351' AND a.account_type = 'SHARES';

-- Miss Ann Maureen Kendi Murithi (1333) - Shares: 9000 - 3000 = 6000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 6000
WHERE m.employee_id = '1333' AND a.account_type = 'SHARES';

-- Mr Eric Stanley Ng'ethe (5160) - Shares: 28079 - 3000 = 25079
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 25079
WHERE m.employee_id = '5160' AND a.account_type = 'SHARES';

-- Cynthia Jerobon Kiptanui (6221) - Shares: 10000 - 3000 = 7000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = 7000
WHERE m.employee_id = '6221' AND a.account_type = 'SHARES';

SET SQL_SAFE_UPDATES = 1;

SELECT 'January Import Complete' as Status;
SELECT COUNT(*) as loans_updated, SUM(outstanding_balance) as total_outstanding 
FROM loans WHERE outstanding_balance > 0;
SELECT COUNT(*) as shares_updated, SUM(balance) as total_shares 
FROM accounts WHERE account_type = 'SHARES' AND balance > 0;
