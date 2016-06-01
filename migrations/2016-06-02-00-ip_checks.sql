ALTER TABLE `user` ADD COLUMN `check_ip` BIT NOT NULL DEFAULT TRUE AFTER `email_verified`;
