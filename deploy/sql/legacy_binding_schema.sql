-- legacy_binding_schema.sql
-- 用途：当前系统账号/班级 与 piclass 旧数据之间的桥接映射表

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `edu_legacy_class_map`;
CREATE TABLE `edu_legacy_class_map` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `current_class_name` varchar(64) NOT NULL,
  `current_grade_no` int DEFAULT NULL,
  `current_class_no` int DEFAULT NULL,
  `legacy_class_code` varchar(64) DEFAULT NULL,
  `legacy_class_name` varchar(255) DEFAULT NULL,
  `source_system` varchar(32) NOT NULL DEFAULT 'piclass2',
  `map_status` char(1) NOT NULL DEFAULT '0',
  `remark` varchar(1000) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_elcm_current_class_name` (`current_class_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `edu_legacy_user_map`;
CREATE TABLE `edu_legacy_user_map` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `current_user_id` bigint NOT NULL,
  `current_user_name` varchar(64) NOT NULL,
  `current_role_key` varchar(32) DEFAULT NULL,
  `current_class_name` varchar(64) DEFAULT NULL,
  `legacy_user_name` varchar(64) DEFAULT NULL,
  `legacy_student_no` varchar(64) DEFAULT NULL,
  `legacy_student_name` varchar(128) DEFAULT NULL,
  `legacy_class_code` varchar(64) DEFAULT NULL,
  `source_system` varchar(32) NOT NULL DEFAULT 'piclass2',
  `map_status` char(1) NOT NULL DEFAULT '0',
  `remark` varchar(1000) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_elum_current_user_id` (`current_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
