ALTER TABLE `history`
  ADD COLUMN `legacy` BIT NOT NULL DEFAULT b'0'
  AFTER `hidden`;
UPDATE history
SET legacy = 1;