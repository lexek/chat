delete userauth from userauth where user_id is null;
ALTER TABLE `userauth` CHANGE COLUMN `user_id` `user_id` BIGINT(20) NOT NULL AFTER `service`;
