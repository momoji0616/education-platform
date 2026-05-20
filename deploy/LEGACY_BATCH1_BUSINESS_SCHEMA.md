# 第一批正式业务表设计

本文档定义第一批旧数据接入完成后，当前教育平台应该新增的正式业务表。

第一批范围只覆盖：

- `tk_choice1`
- `tk_choice2`
- `dt_choice`

设计目标：

- 让第一批数据能够被当前业务模块直接消费
- 与中间清洗层职责分离
- 不破坏当前主控端、教师 Pad、学生 Pad 的角色边界
- 为学生智能刷题、学业诊断、教师学情分析提供稳定数据源

关联文件：

- [LEGACY_BATCH1_PLAN.md](/E:/education-platform/deploy/LEGACY_BATCH1_PLAN.md)
- [LEGACY_STAGING_SCHEMA.md](/E:/education-platform/deploy/LEGACY_STAGING_SCHEMA.md)
- [legacy_batch1_business_schema.sql](/E:/education-platform/deploy/sql/legacy_batch1_business_schema.sql)

## 一、第一批正式业务表清单

建议新增 3 张正式业务表：

1. `edu_question_catalog`
2. `edu_question_bank`
3. `edu_student_answer_record`

## 二、表职责说明

### 1. `edu_question_catalog`

职责：

- 存放课程、章节、题库目录信息
- 为题库筛选、按章节分析、试卷生成提供目录层支撑

数据来源：

- `legacy_staging_question_catalog`

主要使用端：

- 教师端题库与组卷
- 学生端按章节刷题
- 教师端章节学情分析

核心字段建议：

- `id`
- `catalog_code`
- `catalog_name`
- `course_name`
- `chapter_code`
- `chapter_name`
- `question_type`
- `question_count`
- `source_system`
- `source_catalog_id`
- `status`

### 2. `edu_question_bank`

职责：

- 存放标准化题库题目
- 供推荐刷题、错题复练、题目问答、教师组卷使用

数据来源：

- `legacy_staging_question_bank`

主要使用端：

- 学生端刷题页
- 学生端学业诊断页
- 教师端题库/试卷页
- RAG 教学问答增强

核心字段建议：

- `id`
- `catalog_id`
- `question_code`
- `question_type`
- `course_name`
- `chapter_code`
- `chapter_name`
- `question_stem`
- `options_json`
- `standard_answer`
- `analysis`
- `knowledge_point`
- `difficulty_level`
- `source_system`
- `source_question_id`
- `status`

### 3. `edu_student_answer_record`

职责：

- 存放学生答题记录
- 供错题分析、学业诊断、推荐算法、教师学情分析使用

数据来源：

- `legacy_staging_student_answer`

主要使用端：

- 学生端学业诊断
- 学生端智能刷题
- 教师端学情分析

核心字段建议：

- `id`
- `student_no`
- `question_id`
- `question_type`
- `assignment_id`
- `answer_content`
- `score`
- `is_correct`
- `submit_time`
- `source_system`
- `source_record_id`

## 三、与现有页面的对应关系

### 学生端 [practice.vue](/E:/education-platform/frontend/src/views/education/student/practice.vue)

建议使用表：

- `edu_question_bank`
- `edu_student_answer_record`
- `edu_question_catalog`

用于生成：

- 错题重练
- 同章节强化推荐
- 同知识点巩固题
- 推荐练习数量与练习方向

### 学生端 [report.vue](/E:/education-platform/frontend/src/views/education/student/report.vue)

建议使用表：

- `edu_student_answer_record`
- `edu_question_bank`
- `edu_question_catalog`

用于生成：

- 课程维度正确率
- 章节薄弱点
- 知识点风险项
- 高频错题方向

### 教师端 [analysis.vue](/E:/education-platform/frontend/src/views/education/teacher/analysis.vue)

建议使用表：

- `edu_student_answer_record`
- `edu_question_bank`
- `edu_question_catalog`

用于生成：

- 班级章节正确率
- 高频错题排行
- 知识点热力分布
- 班级整体风险判断

## 四、与中间层的关系

中间层负责：

- 保留原始来源
- 数据清洗
- 字段标准化

正式业务表负责：

- 被当前系统业务逻辑直接消费
- 提供稳定接口查询
- 保持字段语义清晰

关系建议：

- `legacy_staging_question_catalog` -> `edu_question_catalog`
- `legacy_staging_question_bank` -> `edu_question_bank`
- `legacy_staging_student_answer` -> `edu_student_answer_record`

## 五、字段设计原则

### 目录表

- 尽量保存课程、章节双维度
- 不把旧系统的 `id1/chapter` 直接暴露给前端
- 保留 `source_catalog_id` 方便回溯

### 题库表

- 统一选择题字段格式
- 题干和选项分离
- 解析、答案、知识点必须保留
- `difficulty_level` 第一批可先规则赋值，后续再优化

### 作答表

- 一个学生一次作答一条记录
- `is_correct` 和 `score` 都保留
- `submit_time` 用于趋势分析
- `assignment_id` 第一批可以为空，后续第二批补全

## 六、第一批后端接口建议

建议新增接口方向：

### 学生端

- 我的章节正确率
- 我的薄弱知识点
- 我的推荐练习题
- 我的错题列表

### 教师端

- 班级章节正确率
- 班级高频错题
- 章节题库分布

### 通用

- 题库目录查询
- 按章节查询题目
- 按知识点查询题目

## 七、第一批权限边界

必须继续遵守：

- 学生只能看本人答题记录
- 教师只能看自己班级学生统计
- 管理者不进入 Pad 端做题功能
- 主控端只看全局汇总，不直接看学生原始明细

## 八、第一批验收口径

第一批正式业务表落地后，至少要满足：

1. 一个学生可以查到自己的历史选择题作答情况
2. 一个学生可以按章节看到薄弱项
3. 学生刷题推荐不再是纯静态模板
4. 教师能看到班级章节正确率统计
5. 教师能看到高频错误题目或知识点

## 九、下一步推荐

后续继续顺序：

1. 生成第一批正式业务表 SQL
2. 设计第一批数据同步逻辑
3. 设计第一批接口清单
4. 再开始真正开发
