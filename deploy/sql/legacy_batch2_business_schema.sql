-- legacy_batch2_business_schema.sql
-- 用途：承接旧平台第二批数据（编程题、作业、作业成绩）的正式业务表

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `edu_assignment_score`;
DROP TABLE IF EXISTS `edu_assignment_question`;
DROP TABLE IF EXISTS `edu_assignment`;
DROP TABLE IF EXISTS `edu_program_answer_record`;

CREATE TABLE `edu_program_answer_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_no` varchar(64) NOT NULL,
  `question_id` bigint DEFAULT NULL,
  `assignment_source_id` bigint DEFAULT NULL,
  `answer_content` longtext,
  `raw_code` longtext,
  `score` decimal(10,2) DEFAULT 0,
  `is_correct` char(1) DEFAULT '0',
  `teacher_feedback` longtext,
  `submit_time` datetime DEFAULT NULL,
  `program_language` varchar(64) DEFAULT NULL,
  `source_system` varchar(32) NOT NULL DEFAULT 'piclass2',
  `source_record_id` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_epar_student` (`student_no`),
  KEY `idx_epar_question` (`question_id`),
  KEY `idx_epar_assign` (`assignment_source_id`),
  KEY `idx_epar_submit_time` (`submit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `edu_assignment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `assignment_code` varchar(64) NOT NULL,
  `teacher_no` varchar(64) DEFAULT NULL,
  `class_code` varchar(64) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `description` varchar(2000) DEFAULT NULL,
  `program_count` int DEFAULT 0,
  `choice_count` int DEFAULT 0,
  `office_count` int DEFAULT 0,
  `file_count` int DEFAULT 0,
  `deadline` datetime DEFAULT NULL,
  `status` varchar(32) DEFAULT NULL,
  `source_system` varchar(32) NOT NULL DEFAULT 'piclass2',
  `source_assignment_id` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ea_code` (`assignment_code`),
  KEY `idx_ea_class_code` (`class_code`),
  KEY `idx_ea_source` (`source_assignment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `edu_assignment_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `assignment_id` bigint NOT NULL,
  `question_id` bigint DEFAULT NULL,
  `source_question_id_1` bigint DEFAULT NULL,
  `source_question_id_2` bigint DEFAULT NULL,
  `question_type_code` varchar(32) DEFAULT NULL,
  `remark` varchar(2000) DEFAULT NULL,
  `peer_review_flag` tinyint(1) DEFAULT 0,
  `source_system` varchar(32) NOT NULL DEFAULT 'piclass2',
  `source_record_id` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_eaq_assignment` (`assignment_id`),
  KEY `idx_eaq_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `edu_assignment_score` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_no` varchar(64) NOT NULL,
  `assignment_id` bigint DEFAULT NULL,
  `program_score` decimal(10,2) DEFAULT 0,
  `choice_score` decimal(10,2) DEFAULT 0,
  `office_score` decimal(10,2) DEFAULT 0,
  `file_score` decimal(10,2) DEFAULT 0,
  `total_score` decimal(10,2) DEFAULT 0,
  `score_time` datetime DEFAULT NULL,
  `source_system` varchar(32) NOT NULL DEFAULT 'piclass2',
  `source_record_id` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_eas_student` (`student_no`),
  KEY `idx_eas_assignment` (`assignment_id`),
  KEY `idx_eas_score_time` (`score_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
