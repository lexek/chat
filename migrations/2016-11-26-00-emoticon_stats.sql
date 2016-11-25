CREATE TABLE `emoticon_usage` (
  `emoticon_id` BIGINT(20) NOT NULL,
  `user_id`     BIGINT(20) NOT NULL,
  `date`        DATE       NOT NULL,
  `count`       BIGINT(20) NOT NULL,
  PRIMARY KEY (`emoticon_id`, `date`, `user_id`),
  INDEX `EMOTICON_STATS_USER` (`user_id`),
  INDEX `emoticon_id` (`emoticon_id`),
  CONSTRAINT `EMOTICON_STATS_ID` FOREIGN KEY (`emoticon_id`) REFERENCES `emoticon` (`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT `EMOTICON_STATS_USER` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
);
