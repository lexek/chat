CREATE TABLE `proxy_auth` (
  `id`            BIGINT(20)   NOT NULL AUTO_INCREMENT,
  `service`       VARCHAR(255) NOT NULL
  COLLATE 'utf8_unicode_ci',
  `external_id`   VARCHAR(255) NOT NULL
  COLLATE 'utf8_unicode_ci',
  `external_name` VARCHAR(255) NOT NULL
  COLLATE 'utf8_unicode_ci',
  `owner_id`      BIGINT(20)   NOT NULL,
  `key`           TEXT         NOT NULL
  COLLATE 'utf8_unicode_ci',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `service_external_id` (`service`, `external_id`),
  INDEX `PROXY_AUTH_OWNER` (`owner_id`),
  CONSTRAINT `PROXY_AUTH_OWNER` FOREIGN KEY (`owner_id`) REFERENCES `user` (`id`)
    ON UPDATE CASCADE
);

ALTER TABLE `chat_proxy`
ALTER `enable_outbound` DROP DEFAULT;
ALTER TABLE `chat_proxy`
ADD COLUMN `auth_id` BIGINT NULL DEFAULT NULL
AFTER `remote_room`,
CHANGE COLUMN `enable_outbound` `enable_outbound` BIT(1) NOT NULL
AFTER `auth_id`,
DROP COLUMN `auth_name`,
DROP COLUMN `auth_key`,
ADD INDEX `auth_id` (`auth_id`),
ADD CONSTRAINT `PROXY_AUTH` FOREIGN KEY (`auth_id`) REFERENCES `proxy_auth` (`id`)
  ON UPDATE CASCADE
  ON DELETE RESTRICT;
