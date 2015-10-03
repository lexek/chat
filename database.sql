CREATE TABLE `user` (
  `id`               BIGINT(20)   NOT NULL AUTO_INCREMENT,
  `banned`           BIT(1)       NOT NULL,
  `color`            VARCHAR(255) NOT NULL,
  `name`             VARCHAR(255) NOT NULL,
  `rename_available` BIT(1)       NOT NULL,
  `role`             VARCHAR(255) NOT NULL,
  `email`            VARCHAR(255) NULL     DEFAULT NULL,
  `email_verified`   BIT          NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `UNIQUE_NAME` (`name`),
  UNIQUE INDEX `UNIQUE_EMAIL` (`email`)
);

CREATE TABLE `userauth` (
  `id`        BIGINT(20)   NOT NULL AUTO_INCREMENT,
  `auth_id`   VARCHAR(255) NULL     DEFAULT NULL,
  `auth_key`  VARCHAR(255) NOT NULL,
  `service`   VARCHAR(255) NOT NULL,
  `user_id`   BIGINT(20)   NULL     DEFAULT NULL,
  `auth_name` VARCHAR(255) NULL     DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `UNIQUE_AUTHID_SERVICE` (`auth_id`, `service`),
  UNIQUE INDEX `UNIQUE_USER_SERVICE` (`service`, `user_id`),
  INDEX `FOREIGN_USERID` (`user_id`),
  CONSTRAINT `USER_ID` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON DELETE CASCADE
);

CREATE TABLE `room` (
  `id`    BIGINT(20)  NOT NULL AUTO_INCREMENT,
  `name`  VARCHAR(50) NOT NULL
  COLLATE 'utf8mb4_unicode_ci',
  `topic` TEXT        NOT NULL
  COLLATE 'utf8mb4_unicode_ci',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `NAME_UNIQUE` (`name`)
);

INSERT INTO `room` VALUES (NULL, '#main', 'main room');

CREATE TABLE `chatter` (
  `id`      BIGINT(20) NOT NULL AUTO_INCREMENT,
  `room_id` BIGINT(20) NOT NULL,
  `user_id` BIGINT(20) NOT NULL,
  `role`    TINYTEXT   NOT NULL
  COLLATE 'utf8mb4_unicode_ci',
  `banned`  BIT(1)     NOT NULL,
  `timeout` BIGINT(20) NULL     DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `UNIQUE_ROOM_PLUS_USER` (`room_id`, `user_id`),
  INDEX `FK_USER` (`user_id`),
  CONSTRAINT `FK_CHATTER_USER` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT `FK_CHATTER_ROOM` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
);

CREATE TABLE `announcement` (
  `id`      BIGINT(20) NOT NULL AUTO_INCREMENT,
  `room_id` BIGINT(20) NOT NULL,
  `active`  BIT(1)     NOT NULL,
  `text`    TEXT       NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `FK_ANNOUNCEMENT_ROOM` (`room_id`),
  CONSTRAINT `FK_ANNOUNCEMENT_ROOM` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
);

CREATE TABLE `emoticon` (
  `id`        BIGINT(20)   NOT NULL AUTO_INCREMENT,
  `code`      VARCHAR(255) NOT NULL,
  `file_name` VARCHAR(255) NOT NULL,
  `height`    INT(11)      NOT NULL,
  `width`     INT(11)      NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `CODE_UNIQUE` (`code`)
);

CREATE TABLE `history` (
  `id`        BIGINT(20)  NOT NULL AUTO_INCREMENT,
  `room_id`   BIGINT(20)  NOT NULL,
  `user_id`   BIGINT(20)  NOT NULL,
  `timestamp` BIGINT(11)  NOT NULL,
  `type`      VARCHAR(32) NOT NULL
  COLLATE 'utf8mb4_unicode_ci',
  `message`   MEDIUMTEXT  NOT NULL
  COLLATE 'utf8mb4_unicode_ci',
  `hidden`    BIT(1)      NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `FK_ROOM` (`room_id`),
  INDEX `FK_USER` (`user_id`),
  INDEX `timestamp` (`timestamp`),
  CONSTRAINT `FK_HISTORY_ROOM` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT `FK_HISTORY_USER` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
);

CREATE TABLE `journal` (
  `id`                 BIGINT(20)   NOT NULL AUTO_INCREMENT,
  `time`               TIMESTAMP    NOT NULL,
  `user_id`            BIGINT(20)   NULL,
  `admin_id`           BIGINT(20)   NULL     DEFAULT NULL,
  `action`             VARCHAR(255) NOT NULL
  COLLATE 'utf8mb4_unicode_ci',
  `action_description` TEXT         NULL
  COLLATE 'utf8mb4_unicode_ci',
  `room_id`            BIGINT(20)   NULL     DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `FK_JOURNAL_USER` (`user_id`),
  INDEX `FK_JOURNAL_ROOM` (`room_id`),
  INDEX `FK_JOURNAL_ADMIN_USER` (`admin_id`),
  CONSTRAINT `FK_JOURNAL_USER` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT `FK_JOURNAL_ROOM` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT `FK_JOURNAL_ADMIN_USER` FOREIGN KEY (`admin_id`) REFERENCES `user` (`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
);

CREATE TABLE `metric` (
  `id`    BIGINT(20)   NOT NULL AUTO_INCREMENT,
  `name`  VARCHAR(255) NOT NULL,
  `time`  BIGINT(20)   NOT NULL,
  `value` DOUBLE       NULL     DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `time` (`time`)
);

CREATE TABLE `pending_confirmation` (
  `id`      BIGINT(20) NOT NULL AUTO_INCREMENT,
  `code`    TEXT       NOT NULL
  COLLATE 'utf8mb4_unicode_ci',
  `user_id` BIGINT(20) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `FK_CONFIRMATION_USER` (`user_id`),
  UNIQUE INDEX `UNIQUE_USER_ID` (`user_id`),
  CONSTRAINT `FK_CONFIRMATION_USER` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
);

CREATE TABLE `poll` (
  `id`       BIGINT(20) NOT NULL AUTO_INCREMENT,
  `room_id`  BIGINT(20) NOT NULL,
  `question` TEXT       NOT NULL
  COLLATE 'utf8mb4_unicode_ci',
  `open`     BIT(1)     NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `FK_POLL_ROOM` (`room_id`),
  CONSTRAINT `FK_POLL_ROOM` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
);

CREATE TABLE `poll_option` (
  `poll_id` BIGINT(20) NOT NULL,
  `option`  INT(20)    NOT NULL,
  `text`    TEXT       NOT NULL
  COLLATE 'utf8mb4_unicode_ci',
  PRIMARY KEY (`poll_id`, `option`),
  CONSTRAINT `FK_POLL_OPTION_POLL` FOREIGN KEY (`poll_id`) REFERENCES `poll` (`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
);

CREATE TABLE `poll_answer` (
  `poll_id`         BIGINT(20) NOT NULL,
  `user_id`         BIGINT(20) NOT NULL,
  `selected_option` INT(20)    NOT NULL,
  PRIMARY KEY (`poll_id`, `user_id`),
  INDEX `FK_POLL_ANSWER_USER` (`user_id`),
  INDEX `FK_POLL_ANSWER_OPTION` (`poll_id`, `selected_option`),
  CONSTRAINT `FK_POLL_ANSWER_OPTION` FOREIGN KEY (`poll_id`, `selected_option`) REFERENCES `poll_option` (`poll_id`, `option`)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT `FK_POLL_ANSWER_POLL` FOREIGN KEY (`poll_id`) REFERENCES `poll` (`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT `FK_POLL_ANSWER_USER` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
);

CREATE TABLE `session` (
  `id`          BIGINT(11)   NOT NULL AUTO_INCREMENT,
  `sid`         VARCHAR(255) NOT NULL,
  `userauth_id` BIGINT(11)   NOT NULL,
  `expires`     BIGINT(20)   NOT NULL,
  `ip`          VARCHAR(255) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `FK_SESSION_USERAUTH` (`userauth_id`),
  INDEX `sid` (`sid`),
  CONSTRAINT `FK_SESSION_USERAUTH` FOREIGN KEY (`userauth_id`) REFERENCES `userauth` (`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
);

CREATE TABLE `stream` (
  `id`      BIGINT(20)   NOT NULL,
  `started` TIMESTAMP    NOT NULL DEFAULT '0000-00-00 00:00:00',
  `ended`   TIMESTAMP    NOT NULL DEFAULT '0000-00-00 00:00:00',
  `title`   VARCHAR(255) NOT NULL
  COLLATE 'utf8mb4_unicode_ci',
  PRIMARY KEY (`id`)
);

CREATE TABLE `ticket` (
  `id`              BIGINT(20) NOT NULL AUTO_INCREMENT,
  `timestamp`       BIGINT(20) NOT NULL,
  `user`            BIGINT(20) NOT NULL,
  `is_open`         BIT(1)     NOT NULL,
  `category`        TEXT       NOT NULL
  COLLATE 'utf8mb4_unicode_ci',
  `text`            TEXT       NOT NULL
  COLLATE 'utf8mb4_unicode_ci',
  `admin_reply`     TEXT       NULL
  COLLATE 'utf8mb4_unicode_ci',
  `closed_by`       BIGINT(20) NULL     DEFAULT NULL,
  `reply_delivered` BIT(1)     NULL     DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `CLOSED_BY_FK` (`closed_by`),
  INDEX `USER_FK` (`user`),
  CONSTRAINT `CLOSED_BY_FK` FOREIGN KEY (`closed_by`) REFERENCES `user` (`id`),
  CONSTRAINT `USER_FK` FOREIGN KEY (`user`) REFERENCES `user` (`id`)
);

CREATE TABLE `pending_notification` (
  `id`      BIGINT(20) NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT(20) NOT NULL,
  `text`    TEXT       NOT NULL
  COLLATE 'utf8mb4_unicode_ci',
  PRIMARY KEY (`id`),
  INDEX `user_id` (`user_id`),
  CONSTRAINT `FK_NOTIFICATION_USER` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
);

CREATE TABLE `steam_game` (
  `id`   BIGINT NOT NULL,
  `name` TEXT   NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `chat_proxy` (
  `id`              BIGINT(20)  NOT NULL AUTO_INCREMENT,
  `room_id`         BIGINT(20)  NOT NULL,
  `provider_name`   VARCHAR(64) NOT NULL
  COLLATE 'utf8mb4_unicode_ci',
  `auth_name`       TINYTEXT    NULL
  COLLATE 'utf8mb4_unicode_ci',
  `auth_key`        TEXT        NULL
  COLLATE 'utf8mb4_unicode_ci',
  `remote_room`     VARCHAR(50) NOT NULL
  COLLATE 'utf8mb4_unicode_ci',
  `enable_outbound` BIT(1)      NULL     DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `provider_name_remote_room` (`provider_name`, `remote_room`),
  INDEX `room_id` (`room_id`),
  CONSTRAINT `PROXY_ROOM` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
);
