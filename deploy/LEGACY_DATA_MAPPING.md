# 旧教学数据融合映射表

本文档针对 [piclass2.sql](/E:/education-platform/backend/sql/piclass2.sql) 中可复用的数据，给出与当前教育平台的业务映射方案。

适用原则：

- 不直接覆盖现有若依用户、权限、业务主表
- 先保留旧数据原貌，再做清洗映射
- 主控端、教师 Pad、学生 Pad 继续遵循现有角色隔离
- Spring Boot 负责业务表接入，FastAPI 只消费清洗后的知识数据和行为特征

## 1. 数据价值判断

`piclass2.sql` 不是单一成绩文件，而是一整套旧教学平台数据库，核心可复用价值主要来自三类数据：

- 题库层：选择题、编程题、章节、课程、答案、解析
- 行为层：学生答题、代码提交、得分、时间轨迹
- 教学任务层：作业下发、作业题目关联、班级、学生关系

建议优先接入表：

- `tk_choice1`
- `tk_choice2`
- `dt_choice`
- `dt_program`
- `zy_assign`
- `zy_detail`
- `zy_score`
- `student`
- `class`

暂不建议直接接入表：

- `django_session`
- `celery_taskmeta`
- `blog_*`
- `message*`
- `news`
- `files`
- 旧 `user` 表直接替换当前账号体系

## 2. 旧表到新业务域映射

### 2.1 题库域

#### 旧表 `tk_choice1`

用途判断：

- 题库主分类
- 课程/章节目录
- 题库拥有者与题量信息

关键字段：

- `id1`：题库主编号
- `chapter`：章节编号
- `title`：章节或题库标题
- `count`：题目数量
- `type`：题库类型

建议映射到新表：

- `edu_question_catalog`

建议字段映射：

- `source_id` <- `id1`
- `chapter_code` <- `chapter`
- `catalog_name` <- `title`
- `question_count` <- `count`
- `source_system` <- `'piclass2'`

#### 旧表 `tk_choice2`

用途判断：

- 选择题明细
- 标准答案
- 题目解析
- 知识点/备注

关键字段：

- `id1`：题库编号
- `id2`：题目序号
- `chapter`
- `question`
- `ans`
- `jx`
- `kb`

建议映射到新表：

- `edu_question_bank`

建议字段映射：

- `source_id` <- `id`
- `catalog_source_id` <- `id1`
- `question_no` <- `id2`
- `chapter_code` <- `chapter`
- `question_type` <- `'choice'`
- `question_stem` <- `question`
- `standard_answer` <- `ans`
- `analysis` <- `jx`
- `knowledge_point` <- `kb`
- `source_system` <- `'piclass2'`

补充处理建议：

- `question` 字段内包含题干和选项，需拆出 `options_json`
- `chapter_code='*'` 的数据，视为未分类题目，单独标注

#### 旧表 `tk_program`

用途判断：

- 编程题题库
- 题干、标准代码或评测关联

建议映射到新表：

- `edu_question_bank`

建议字段映射：

- `question_type` <- `'program'`
- 其余字段按选择题相同思路映射

备注：

- 由于当前未完整展开字段，正式实施前需再读取结构并确认编程题标准答案、语言、测试规则字段

### 2.2 学生行为域

#### 旧表 `dt_choice`

用途判断：

- 学生选择题作答记录
- 实际答案
- 实时得分
- 作答时间

关键字段：

- `id1`
- `id2`
- `username`
- `ans`
- `score`
- `time`
- `zy_id`

建议映射到新表：

- `edu_student_answer_record`

建议字段映射：

- `source_id` <- `id`
- `catalog_source_id` <- `id1`
- `question_no` <- `id2`
- `student_no` <- `username`
- `answer_type` <- `'choice'`
- `answer_content` <- `ans`
- `score` <- `score`
- `is_correct` <- `score > 0`
- `submit_time` <- `time`
- `assignment_source_id` <- `zy_id`
- `source_system` <- `'piclass2'`

#### 旧表 `dt_program`

用途判断：

- 学生编程题提交记录
- 学生代码
- 分数
- 评语/判题反馈

关键字段：

- `id1`
- `username`
- `score`
- `notice`
- `code`
- `zy_id`
- `time`

建议映射到新表：

- `edu_student_answer_record`

建议字段映射：

- `source_id` <- `id`
- `catalog_source_id` <- `id1`
- `student_no` <- `username`
- `answer_type` <- `'program'`
- `answer_content` <- `code`
- `raw_code` <- `code`
- `score` <- 数值化后的 `score`
- `teacher_feedback` <- `notice`
- `submit_time` <- `time`
- `assignment_source_id` <- `zy_id`
- `source_system` <- `'piclass2'`

补充处理建议：

- `score` 目前是字符型，需统一转成数值
- 空代码与零分记录要保留，作为学情分析的重要负样本

### 2.3 教学任务域

#### 旧表 `zy_assign`

用途判断：

- 教师布置作业
- 班级作业范围
- 截止时间
- 题型数量

关键字段：

- `username`
- `class`
- `title`
- `notice`
- `program`
- `choice`
- `office`
- `file`
- `deadline`
- `state`

建议映射到新表：

- `edu_assignment`

建议字段映射：

- `source_id` <- `id`
- `teacher_no` <- `username`
- `class_code` <- `class`
- `title` <- `title`
- `description` <- `notice`
- `choice_count` <- `choice`
- `program_count` <- `program`
- `office_count` <- `office`
- `file_count` <- `file`
- `deadline` <- `deadline`
- `status` <- `state`
- `source_system` <- `'piclass2'`

#### 旧表 `zy_detail`

用途判断：

- 作业与题目关联明细
- 题型标记
- 作业附加说明

关键字段：

- `zy_id`
- `id1`
- `id2`
- `type`
- `notice`

建议映射到新表：

- `edu_assignment_question`

建议字段映射：

- `assignment_source_id` <- `zy_id`
- `question_source_id` <- `id1` 或 `id2`
- `question_type_code` <- `type`
- `remark` <- `notice`
- `source_system` <- `'piclass2'`

字段解释建议：

- `type=1` 初步判断是题库题
- `type=4` 初步判断是文件型作业
- 正式实施前需从样本进一步确认完整枚举

#### 旧表 `zy_score`

用途判断：

- 学生作业汇总得分
- 按题型统计成绩

关键字段：

- `username`
- `zy_id`
- `program`
- `choice`
- `office`
- `file`

建议映射到新表：

- `edu_assignment_score`

建议字段映射：

- `student_no` <- `username`
- `assignment_source_id` <- `zy_id`
- `program_score` <- `program`
- `choice_score` <- `choice`
- `office_score` <- `office`
- `file_score` <- `file`
- `total_score` <- 四类得分汇总
- `source_system` <- `'piclass2'`

### 2.4 学生与班级域

#### 旧表 `student`

用途判断：

- 学生基础档案
- 学号、姓名、专业、班级

建议映射到新表：

- `edu_student_profile`

建议字段映射：

- `source_id` <- `id`
- `student_no` <- `username`
- `student_name` <- `name`
- `major_name` <- `major`
- `class_code` <- `class`
- `class_code_ext` <- `class2`
- `seat_no` <- `seat`
- `remark` <- `notice`
- `source_system` <- `'piclass2'`

#### 旧表 `class`

用途判断：

- 班级与教师关系
- 班级时间、地点、班级编号

建议映射到新表：

- `edu_class`

建议字段映射：

- `source_id` <- `id`
- `teacher_no` <- `teacher`
- `creator_no` <- `username`
- `class_name` <- `class_name`
- `class_time` <- `class_time`
- `class_address` <- `class_address`
- `class_code` <- `class`
- `class_type` <- `type`
- `notice` <- `notice`
- `source_system` <- `'piclass2'`

#### 旧表 `user`

用途判断：

- 旧平台账号信息

处理建议：

- 不直接导入当前若依账号主表
- 仅抽取 `username/name/major/role` 做临时对照映射
- 若要关联当前系统账号，必须走“账号绑定表”而不是覆盖现有用户体系

建议映射到新表：

- `edu_legacy_user_map`

## 3. 与当前功能的对应关系

### 学生端

可直接增强：

- 个性化学习规划
- 多维度学业诊断报告
- 智能习题生成与个性化刷题推荐
- RAG 学业问答

主要依赖表：

- `edu_question_bank`
- `edu_student_answer_record`
- `edu_assignment_score`
- `edu_student_profile`

### 教师端

可直接增强：

- 班级学情智能分析
- 智能题库与试卷生成
- 作业讲评助手
- AI 教学问答中的班级分析能力

主要依赖表：

- `edu_assignment`
- `edu_assignment_question`
- `edu_assignment_score`
- `edu_student_answer_record`
- `edu_class`

### 主控端

可增强：

- 课程级教学数据看板
- 班级学业统计总览
- 多课程题库覆盖情况分析

## 4. 推荐优先级

### P0

- `tk_choice1`
- `tk_choice2`
- `dt_choice`

价值：

- 可以最快形成“题库 + 学生错题 + 推荐刷题”

### P1

- `dt_program`
- `zy_assign`
- `zy_detail`
- `zy_score`

价值：

- 能补齐教师端学情分析和作业讲评

### P2

- `student`
- `class`
- `user`

价值：

- 完整建立班级与用户映射
- 为后续权限绑定做准备

## 5. 风险提示

- 旧库字段命名不规范，实施前必须加中间清洗层
- `user` 表与当前系统账号体系冲突风险高，不能直接覆盖
- 编程题和文件题的 `type` 含义需二次确认
- 旧数据编码存在历史中文乱码，导入时需要统一 UTF-8 清洗
- 大体量 SQL 不适合直接在当前生产库执行，应先在隔离库验证

## 6. 当前状态

本文件仅完成数据审阅和设计映射，不包含：

- 真实导库
- 数据清洗脚本
- 新表建表 SQL
- 业务代码接入

后续可继续产出：

- 中间层建表 SQL
- 分阶段导入脚本
- 功能接入优先级开发清单
