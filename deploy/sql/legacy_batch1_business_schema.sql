-- legacy_batch1_business_schema.sql
-- 用途：第一批旧数据接入后的正式业务表
-- 说明：本文件面向当前教育平台业务使用，不保存全部旧数据原貌

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `edu_question_catalog`;
CREATE TABLE `edu_question_catalog` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `catalog_code` varchar(64) NOT NULL,
  `catalog_name` varchar(255) NOT NULL,
  `course_name` varchar(255) DEFAULT NULL,
  `chapter_code` varchar(32) DEFAULT NULL,
  `chapter_name` varchar(255) DEFAULT NULL,
  `question_type` varchar(32) NOT NULL DEFAULT 'choice',
  `question_count` int NOT NULL DEFAULT 0,
  `source_system` varchar(32) NOT NULL DEFAULT 'piclass2',
  `source_catalog_id` bigint DEFAULT NULL,
  `status` char(1) NOT NULL DEFAULT '0',
  `remark` varchar(1000) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eqc_catalog_code` (`catalog_code`),
  KEY `idx_eqc_course_chapter` (`course_name`, `chapter_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `edu_question_bank`;
CREATE TABLE `edu_question_bank` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `catalog_id` bigint DEFAULT NULL,
  `question_code` varchar(64) NOT NULL,
  `question_type` varchar(32) NOT NULL DEFAULT 'choice',
  `course_name` varchar(255) DEFAULT NULL,
  `chapter_code` varchar(32) DEFAULT NULL,
  `chapter_name` varchar(255) DEFAULT NULL,
  `question_stem` longtext,
  `options_json` longtext,
  `standard_answer` longtext,
  `analysis` longtext,
  `knowledge_point` varchar(500) DEFAULT NULL,
  `difficulty_level` varchar(32) DEFAULT 'medium',
  `source_system` varchar(32) NOT NULL DEFAULT 'piclass2',
  `source_question_id` bigint DEFAULT NULL,
  `status` char(1) NOT NULL DEFAULT '0',
  `remark` varchar(1000) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eqb_question_code` (`question_code`),
  KEY `idx_eqb_catalog_id` (`catalog_id`),
  KEY `idx_eqb_course_chapter` (`course_name`, `chapter_code`),
  CONSTRAINT `fk_eqb_catalog_id` FOREIGN KEY (`catalog_id`) REFERENCES `edu_question_catalog` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `edu_student_answer_record`;
CREATE TABLE `edu_student_answer_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_no` varchar(64) NOT NULL,
  `question_id` bigint DEFAULT NULL,
  `question_type` varchar(32) NOT NULL DEFAULT 'choice',
  `assignment_id` bigint DEFAULT NULL,
  `answer_content` longtext,
  `score` decimal(10,2) DEFAULT 0,
  `is_correct` char(1) NOT NULL DEFAULT '0',
  `submit_time` datetime DEFAULT NULL,
  `source_system` varchar(32) NOT NULL DEFAULT 'piclass2',
  `source_record_id` bigint DEFAULT NULL,
  `remark` varchar(1000) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_esar_student_no` (`student_no`),
  KEY `idx_esar_question_id` (`question_id`),
  KEY `idx_esar_submit_time` (`submit_time`),
  CONSTRAINT `fk_esar_question_id` FOREIGN KEY (`question_id`) REFERENCES `edu_question_bank` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
