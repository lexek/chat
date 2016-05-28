CREATE TABLE session_old LIKE session;
INSERT session_old SELECT * FROM session;

DELETE FROM `session`;

ALTER TABLE `session` DROP FOREIGN KEY `FK_SESSION_USERAUTH`;

ALTER TABLE `session` ALTER `userauth_id` DROP DEFAULT;

ALTER TABLE `session`
  CHANGE COLUMN `userauth_id` `user_id` BIGINT(11) NOT NULL AFTER `sid`,
  DROP INDEX `FK_SESSION_USERAUTH`,
  ADD INDEX `FK_SESSION_USER` (`user_id`),
  ADD CONSTRAINT `FK_SESSION_USER` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE;

INSERT INTO session
SELECT
  session_old.id,
  session_old.ip,
  session_old.sid,
  userauth.user_id,
  session_old.expires
FROM session_old JOIN userauth ON session_old.userauth_id = userauth.id;

DROP TABLE session_old;
