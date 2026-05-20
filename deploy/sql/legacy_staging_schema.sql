-- legacy_staging_schema.sql
-- 用途：承接 piclass2 旧教学平台数据的中间清洗层
-- 注意：本文件只用于隔离库或独立前缀表，不可直接替换当前业务主表

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `legacy_staging_question_catalog`;
CREATE TABLE `legacy_staging_question_catalog` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source_system` varchar(32) NOT NULL DEFAULT 'piclass2',
  `source_id` bigint DEFAULT NULL,
  `source_catalog_id` bigint DEFAULT NULL,
  `source_chapter_code` varchar(16) DEFAULT NULL,
  `catalog_name` varchar(255) NOT NULL,
  `question_count` int DEFAULT 0,
  `question_type` varchar(32) DEFAULT NULL,
  `owner_username` varchar(64) DEFAULT NULL,
  `raw_type` varchar(32) DEFAULT NULL,
  `raw_limit` varchar(32) DEFAULT NULL,
  `raw_payload` longtext,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_lsqc_source_catalog` (`source_catalog_id`),
  KEY `idx_lsqc_chapter` (`source_chapter_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `legacy_staging_question_bank`;
CREATE TABLE `legacy_staging_question_bank` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source_system` varchar(32) NOT NULL DEFAULT 'piclass2',
  `source_question_id` bigint DEFAULT NULL,
  `source_catalog_id` bigint DEFAULT NULL,
  `source_question_no` bigint DEFAULT NULL,
  `question_type` varchar(32) NOT NULL,
  `course_name` varchar(255) DEFAULT NULL,
  `chapter_code` varchar(32) DEFAULT NULL,
  `chapter_name` varchar(255) DEFAULT NULL,
  `question_stem` longtext,
  `options_json` longtext,
  `standard_answer` longtext,
  `analysis` longtext,
  `knowledge_point` varchar(500) DEFAULT NULL,
  `program_language` varchar(64) DEFAULT NULL,
  `sample_input` longtext,
  `sample_output` longtext,
  `reference_code` longtext,
  `owner_username` varchar(64) DEFAULT NULL,
  `raw_content` longtext,
  `raw_payload` longtext,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_lsqb_source_question` (`source_question_id`),
  KEY `idx_lsqb_catalog_no` (`source_catalog_id`, `source_question_no`),
  KEY `idx_lsqb_type` (`question_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `legacy_staging_student_answer`;
CREATE TABLE `legacy_staging_student_answer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source_system` varchar(32) NOT NULL DEFAULT 'piclass2',
  `source_record_id` bigint DEFAULT NULL,
  `student_no` varchar(64) NOT NULL,
  `answer_type` varchar(32) NOT NULL,
  `source_catalog_id` bigint DEFAULT NULL,
  `source_question_no` bigint DEFAULT NULL,
  `source_question_id` bigint DEFAULT NULL,
  `assignment_source_id` bigint DEFAULT NULL,
  `answer_content` longtext,
  `raw_code` longtext,
  `standardized_score` decimal(10,2) DEFAULT NULL,
  `is_correct` tinyint(1) DEFAULT NULL,
  `teacher_feedback` longtext,
  `submit_time` datetime DEFAULT NULL,
  `raw_payload` longtext,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_lssa_student` (`student_no`),
  KEY `idx_lssa_question` (`source_question_id`),
  KEY `idx_lssa_assign` (`assignment_source_id`),
  KEY `idx_lssa_submit_time` (`submit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `legacy_staging_student_profile`;
CREATE TABLE `legacy_staging_student_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source_system` varchar(32) NOT NULL DEFAULT 'piclass2',
  `source_id` bigint DEFAULT NULL,
  `student_no` varchar(64) NOT NULL,
  `student_name` varchar(128) DEFAULT NULL,
  `major_name` varchar(128) DEFAULT NULL,
  `class_code` varchar(64) DEFAULT NULL,
  `class_code_ext` varchar(64) DEFAULT NULL,
  `seat_no` int DEFAULT NULL,
  `remark` varchar(1000) DEFAULT NULL,
  `raw_payload` longtext,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lssp_student_no` (`student_no`),
  KEY `idx_lssp_class_code` (`class_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `legacy_staging_class`;
CREATE TABLE `legacy_staging_class` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source_system` varchar(32) NOT NULL DEFAULT 'piclass2',
  `source_id` bigint DEFAULT NULL,
  `class_code` varchar(64) NOT NULL,
  `class_name` varchar(255) DEFAULT NULL,
  `teacher_no` varchar(64) DEFAULT NULL,
  `creator_no` varchar(64) DEFAULT NULL,
  `class_time` varchar(128) DEFAULT NULL,
  `class_address` varchar(255) DEFAULT NULL,
  `class_type` varchar(32) DEFAULT NULL,
  `notice` varchar(1000) DEFAULT NULL,
  `raw_payload` longtext,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lsc_class_code` (`class_code`),
  KEY `idx_lsc_teacher_no` (`teacher_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `legacy_staging_assignment`;
CREATE TABLE `legacy_staging_assignment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source_system` varchar(32) NOT NULL DEFAULT 'piclass2',
  `source_id` bigint DEFAULT NULL,
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
  `raw_limit` varchar(32) DEFAULT NULL,
  `raw_payload` longtext,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_lsa_source_id` (`source_id`),
  KEY `idx_lsa_class_code` (`class_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `legacy_staging_assignment_question`;
CREATE TABLE `legacy_staging_assignment_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source_system` varchar(32) NOT NULL DEFAULT 'piclass2',
  `source_id` bigint DEFAULT NULL,
  `assignment_source_id` bigint DEFAULT NULL,
  `question_source_id_1` bigint DEFAULT NULL,
  `question_source_id_2` bigint DEFAULT NULL,
  `question_type_code` varchar(32) DEFAULT NULL,
  `remark` varchar(2000) DEFAULT NULL,
  `peer_review_flag` tinyint(1) DEFAULT 0,
  `raw_payload` longtext,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_lsaq_assign_source` (`assignment_source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `legacy_staging_assignment_score`;
CREATE TABLE `legacy_staging_assignment_score` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source_system` varchar(32) NOT NULL DEFAULT 'piclass2',
  `source_id` bigint DEFAULT NULL,
  `student_no` varchar(64) NOT NULL,
  `assignment_source_id` bigint DEFAULT NULL,
  `program_score` decimal(10,2) DEFAULT 0,
  `choice_score` decimal(10,2) DEFAULT 0,
  `office_score` decimal(10,2) DEFAULT 0,
  `file_score` decimal(10,2) DEFAULT 0,
  `total_score` decimal(10,2) DEFAULT 0,
  `score_time` datetime DEFAULT NULL,
  `raw_payload` longtext,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_lsas_student` (`student_no`),
  KEY `idx_lsas_assign` (`assignment_source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `legacy_staging_user_bind`;
CREATE TABLE `legacy_staging_user_bind` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source_system` varchar(32) NOT NULL DEFAULT 'piclass2',
  `legacy_username` varchar(64) NOT NULL,
  `legacy_name` varchar(128) DEFAULT NULL,
  `legacy_role_code` varchar(32) DEFAULT NULL,
  `legacy_major_name` varchar(128) DEFAULT NULL,
  `current_user_id` bigint DEFAULT NULL,
  `current_user_name` varchar(128) DEFAULT NULL,
  `bind_status` varchar(32) DEFAULT 'pending',
  `remark` varchar(1000) DEFAULT NULL,
  `raw_payload` longtext,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lsub_legacy_username` (`legacy_username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
