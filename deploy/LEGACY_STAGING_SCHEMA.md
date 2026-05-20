# 旧教学数据中间清洗层设计

本文档定义 `piclass2.sql` 接入当前教育平台时的“中间清洗层”。

设计目标：

- 承接旧数据，不直接写入当前业务主表
- 保存必要的来源字段，便于回溯
- 统一字段风格，便于后续导入正式业务表
- 优先支持第一批功能：题库、选择题作答、学业诊断、智能刷题

关联文件：

- [LEGACY_DATA_EXECUTION_ROADMAP.md](/E:/education-platform/deploy/LEGACY_DATA_EXECUTION_ROADMAP.md)
- [LEGACY_DATA_MAPPING.md](/E:/education-platform/deploy/LEGACY_DATA_MAPPING.md)
- [piclass2.sql](/E:/education-platform/backend/sql/piclass2.sql)
- [legacy_staging_schema.sql](/E:/education-platform/deploy/sql/legacy_staging_schema.sql)

## 1. 中间层定位

建议单独建库或单独 schema：

- `education_legacy_piclass`

如果暂时不能新建数据库，则至少单独使用前缀：

- `legacy_staging_*`

本层职责：

- 接收旧库原始数据的清洗结果
- 不参与当前系统登录、权限、菜单
- 不直接给前端页面使用
- 只作为后续“正式业务表”的上游数据源

## 2. 第一批必建表

### 2.1 `legacy_staging_question_catalog`

来源：

- `tk_choice1`

作用：

- 保存课程/章节/题库分类

核心字段：

- `source_id`
- `source_catalog_id`
- `source_chapter_code`
- `catalog_name`
- `question_count`
- `question_type`
- `owner_username`
- `raw_type`
- `raw_limit`

用于功能：

- 题库分类
- 章节分析
- 智能组卷的章节维度

### 2.2 `legacy_staging_question_bank`

来源：

- `tk_choice2`
- `tk_program`

作用：

- 保存标准化后的题库题目

核心字段：

- `source_question_id`
- `source_catalog_id`
- `source_question_no`
- `question_type`
- `course_name`
- `chapter_code`
- `chapter_name`
- `question_stem`
- `options_json`
- `standard_answer`
- `analysis`
- `knowledge_point`
- `program_language`
- `sample_input`
- `sample_output`
- `reference_code`
- `raw_content`

用于功能：

- 智能刷题
- 教师题库
- RAG 题目问答
- 试卷生成

### 2.3 `legacy_staging_student_answer`

来源：

- `dt_choice`
- `dt_program`

作用：

- 保存学生答题行为

核心字段：

- `source_record_id`
- `student_no`
- `answer_type`
- `source_catalog_id`
- `source_question_no`
- `source_question_id`
- `assignment_source_id`
- `answer_content`
- `raw_code`
- `standardized_score`
- `is_correct`
- `teacher_feedback`
- `submit_time`

用于功能：

- 学业诊断
- 错题本
- 刷题推荐
- 教师学情分析

### 2.4 `legacy_staging_student_profile`

来源：

- `student`

作用：

- 保存旧系统学生基础档案

核心字段：

- `student_no`
- `student_name`
- `major_name`
- `class_code`
- `class_code_ext`
- `seat_no`
- `remark`

用于功能：

- 学生画像
- 班级绑定
- 导入后的人员映射

### 2.5 `legacy_staging_class`

来源：

- `class`

作用：

- 保存旧系统班级、教师信息

核心字段：

- `class_code`
- `class_name`
- `teacher_no`
- `creator_no`
- `class_time`
- `class_address`
- `class_type`
- `notice`

用于功能：

- 教师班级分析
- 学生归班
- 主控端统计

## 3. 第二批建议表

### 3.1 `legacy_staging_assignment`

来源：

- `zy_assign`

作用：

- 保存旧系统作业下发

### 3.2 `legacy_staging_assignment_question`

来源：

- `zy_detail`

作用：

- 保存作业与题目的关联

### 3.3 `legacy_staging_assignment_score`

来源：

- `zy_score`

作用：

- 保存学生作业总分及分题型得分

## 4. 字段清洗规则

### 题库题目

- `tk_choice2.question` 需要拆成：
  - `question_stem`
  - `options_json`
- `tk_choice2.ans` 保留原答案
- `tk_choice2.jx` 作为解析
- `tk_choice2.kb` 作为知识点

### 编程题

- `tk_program.lang` 需映射为语言标识
- `tk_program.input/output` 需拆为样例输入输出
- `dt_program.score` 由字符串转数值
- `dt_program.notice` 作为教师反馈或自动判题反馈

### 学生记录

- `score > 0` 可初步判为正确
- `score = 0` 不能简单删除，保留作失败样本
- 空代码、空答案也保留

### 学生与班级

- `student.username` 视为学号
- `class.class` 视为班级编码
- 旧 `user` 不进入当前若依账号表，只做绑定参考

## 5. 为什么要先建中间层

原因：

- 旧表命名和现系统业务命名不统一
- 旧数据字段存在历史兼容痕迹
- 直接入主表会把清洗逻辑和业务逻辑耦合在一起
- 中间层能保证后面反复调整时不伤主业务

## 6. 第一批接入后能增强的功能

### 学生端

- 智能习题生成与个性化刷题推荐
- 多维度学业诊断报告
- 学习规划中的章节薄弱项建议

### 教师端

- 班级章节正确率分析
- 高频错题分析
- 基于题库的组卷前置能力

## 7. 当前状态

已完成：

- 中间层结构设计
- 第一批、第二批表职责定义

未完成：

- 实际建表执行
- 清洗脚本
- 导入脚本
- 正式业务表同步

下一步推荐：

- 基于本设计生成中间层建表 SQL
- 再设计第一批导入脚本
