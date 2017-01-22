CREATE TABLE `proxy_emoticon` (
  `id`        BIGINT(20)   NOT NULL AUTO_INCREMENT,
  `provider`  VARCHAR(255) NOT NULL,
  `code`      TINYTEXT     NOT NULL,
  `file_name` VARCHAR(255) NOT NULL,
  `width`     INT(11)      NOT NULL,
  `height`    INT(11)      NOT NULL,
  `extra`     TEXT         NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `provider_file_name` (`provider`, `file_name`)
);

ALTER TABLE `proxy_emoticon`
  DROP COLUMN `width`,
  DROP COLUMN `height`;
