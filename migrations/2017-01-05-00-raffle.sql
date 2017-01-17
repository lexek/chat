CREATE TABLE `raffle` (
  `id`   BIGINT NOT NULL AUTO_INCREMENT,
  `name` TEXT   NOT NULL,
  PRIMARY KEY (`id`)
);
CREATE TABLE `raffle_winner` (
  `id`               BIGINT(20) NOT NULL AUTO_INCREMENT,
  `raffle_id`        BIGINT(20) NOT NULL,
  `internal_id`      BIGINT(20) NULL     DEFAULT NULL,
  `external_service` TINYTEXT   NULL,
  `external_id`      TINYTEXT   NULL,
  `external_name`    TINYTEXT   NULL,
  `messages`         LONGTEXT   NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `FK_RAFFLE_WINNER_RAFFLE` (`raffle_id`),
  INDEX `FK_RAFFLE_WINNER_USER` (`internal_id`),
  CONSTRAINT `FK_RAFFLE_WINNER_RAFFLE` FOREIGN KEY (`raffle_id`) REFERENCES `raffle` (`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT `FK_RAFFLE_WINNER_USER` FOREIGN KEY (`internal_id`) REFERENCES `user` (`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
);
