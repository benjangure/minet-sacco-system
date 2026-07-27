SET FOREIGN_KEY_CHECKS=0;
DELETE a1 FROM accounts a1 INNER JOIN accounts a2 WHERE a1.id < a2.id AND a1.member_id = a2.member_id AND a1.account_type = a2.account_type;
SET FOREIGN_KEY_CHECKS=1;
