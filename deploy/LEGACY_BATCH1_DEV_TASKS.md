# 第一批实际开发任务清单

本文档将第一批旧数据接入工作拆解成真正可执行的开发任务。

第一批范围固定为：

- `tk_choice1`
- `tk_choice2`
- `dt_choice`

目标功能固定为：

- 学生端智能刷题推荐增强
- 学生端学业诊断增强
- 教师端班级学情分析增强

执行原则：

- 先基础设施，后业务逻辑，最后页面接入
- 每一步都可单独验证
- 不直接操作当前主业务库历史表
- 所有数据先经过中间清洗层

## 一、任务分阶段

### 阶段 A：数据库基础设施

#### A1. 建立隔离库导入方案

目标：

- 明确旧库导入到哪里

任务：

- 确认隔离库命名
- 确认隔离库字符集
- 确认导入方式

交付：

- 隔离库导入操作说明

#### A2. 执行中间层建表

目标：

- 建立 `legacy_staging_*` 表

依据文件：

- [legacy_staging_schema.sql](/E:/education-platform/deploy/sql/legacy_staging_schema.sql)

交付：

- 中间层表可查询

#### A3. 执行第一批正式业务表建表

目标：

- 建立第一批 `edu_*` 表

依据文件：

- [legacy_batch1_business_schema.sql](/E:/education-platform/deploy/sql/legacy_batch1_business_schema.sql)

交付：

- `edu_question_catalog`
- `edu_question_bank`
- `edu_student_answer_record`

### 阶段 B：数据同步与清洗

#### B1. 编写目录清洗脚本

输入：

- `tk_choice1`

输出：

- `legacy_staging_question_catalog`

重点：

- 目录主键稳定
- 章节编号保留
- 目录名清洗

#### B2. 编写题库清洗脚本

输入：

- `tk_choice2`

输出：

- `legacy_staging_question_bank`

重点：

- 题干和选项拆分
- 答案保留
- 解析保留
- 知识点保留

#### B3. 编写作答记录清洗脚本

输入：

- `dt_choice`

输出：

- `legacy_staging_student_answer`

重点：

- 作答记录主键稳定
- 学号保留
- 分数数值化
- 正误标记生成

#### B4. 编写中间层到正式表同步脚本

输入：

- `legacy_staging_question_catalog`
- `legacy_staging_question_bank`
- `legacy_staging_student_answer`

输出：

- `edu_question_catalog`
- `edu_question_bank`
- `edu_student_answer_record`

重点：

- 幂等 upsert
- 题目与目录关联
- 作答记录与题目关联

### 阶段 C：后端查询实现

#### C1. 题库查询 Mapper / Service

目标：

- 支撑目录查询和按章节查题

建议模块：

- `EduQuestionMapper`
- `EduQuestionQueryService`

#### C2. 学生诊断 Mapper / Service

目标：

- 支撑学生刷题推荐、章节诊断、错题统计

建议模块：

- `EduStudentDiagnosisMapper`
- `EduStudentDiagnosisService`

#### C3. 教师分析 Mapper / Service

目标：

- 支撑教师班级章节分析和高频错题分析

建议模块：

- `EduTeacherAnalysisMapper`
- `EduTeacherAnalysisService`

### 阶段 D：后端接口实现

#### D1. 学生端接口

实现：

- 练习推荐
- 诊断概览
- 章节诊断
- 高频错题
- 薄弱知识点

#### D2. 教师端接口

实现：

- 班级概览
- 章节分析
- 高频错题
- 知识点热力

#### D3. 通用题库接口

实现：

- 目录查询
- 题目查询

### 阶段 E：前端 API 接入

#### E1. 学生 API 模块追加

文件：

- [student.js](/E:/education-platform/frontend/src/api/education/student.js)

#### E2. 教师 API 模块追加

文件：

- [teacher.js](/E:/education-platform/frontend/src/api/education/teacher.js)

### 阶段 F：页面渐进改造

#### F1. 学生刷题页

文件：

- [practice.vue](/E:/education-platform/frontend/src/views/education/student/practice.vue)

目标：

- 推荐题来源改为真实数据

#### F2. 学生诊断页

文件：

- [report.vue](/E:/education-platform/frontend/src/views/education/student/report.vue)

目标：

- 新增章节诊断和错题维度

#### F3. 教师分析页

文件：

- [analysis.vue](/E:/education-platform/frontend/src/views/education/teacher/analysis.vue)

目标：

- 新增章节分析和高频错题维度

## 二、推荐执行顺序

真正开发时建议按这个顺序执行：

1. `A2` 中间层建表
2. `A3` 正式业务表建表
3. `B1` 目录清洗
4. `B2` 题库清洗
5. `B3` 作答清洗
6. `B4` 中间层同步
7. `C1` 题库查询
8. `C2` 学生诊断查询
9. `C3` 教师分析查询
10. `D1` 学生接口
11. `D2` 教师接口
12. `D3` 通用题库接口
13. `E1` 学生 API
14. `E2` 教师 API
15. `F1` 刷题页
16. `F2` 诊断页
17. `F3` 教师分析页

## 三、每一步的最小验收

### 数据库阶段

- 表建成功
- 索引存在
- 字段可查询

### 清洗阶段

- 随机抽取 10 条目录核对
- 随机抽取 10 条题目核对
- 随机抽取 10 条作答记录核对

### 查询阶段

- 同一个学生重复查，结果稳定
- 同一个教师查自己班级，不会串班

### 页面阶段

- 接口有值时显示真实数据
- 接口无值时页面仍可回退旧逻辑，不白屏

## 四、第一批完成后的效果

完成第一批后，你的项目会发生的实际变化：

- 学生刷题不再只是模板生成
- 学生诊断有真实章节数据支撑
- 教师学情分析有真实做题记录支撑
- 项目从“演示功能”往“真实教学数据驱动平台”迈进一步

## 五、当前状态

本文件仍然只是实施清单，不包含：

- 实际建表执行
- 实际脚本开发
- 实际接口开发
- 实际页面改造

下一步推荐：

- 输出第一批清洗脚本设计
- 再进入真正代码实现阶段
