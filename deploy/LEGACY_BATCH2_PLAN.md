# 第二批数据接入计划

## 第二批目标

在第一批“选择题题库 + 选择题作答记录”已经接入的基础上，继续引入：

- 编程题作答记录
- 作业下发记录
- 作业题目关联
- 作业成绩与批改结果

用于增强以下能力：

- 教师端作业分析
- 教师端编程题学情分析
- 教师端 AI 讲评辅助
- 学生端编程题表现诊断
- 学生端更真实的学习规划输入特征

## 计划接入的旧表

来自 [piclass2.sql](/E:/education-platform/backend/sql/piclass2.sql)：

- `dt_program`
- `zy_assign`
- `zy_detail`
- `zy_score`

## 第二批建议新增的中间表

- `legacy_staging_program_answer`
- `legacy_staging_assignment`
- `legacy_staging_assignment_question`
- `legacy_staging_assignment_score`

## 第二批建议新增的正式业务表

- `edu_program_answer_record`
- `edu_assignment`
- `edu_assignment_question`
- `edu_assignment_score`

## 对应功能落点

### 教师端

- [grading.vue](/E:/education-platform/frontend/src/views/education/teacher/grading.vue)
  - 增加“历史编程题表现”维度
- [analysis.vue](/E:/education-platform/frontend/src/views/education/teacher/analysis.vue)
  - 增加作业完成率、编程题失分热点、班级作业表现
- [paper.vue](/E:/education-platform/frontend/src/views/education/teacher/paper.vue)
  - 后续支持按章节与高频错误反向组卷

### 学生端

- [report.vue](/E:/education-platform/frontend/src/views/education/student/report.vue)
  - 增加编程题表现与作业完成趋势
- [plan.vue](/E:/education-platform/frontend/src/views/education/student/plan.vue)
  - 使用真实作业与编程题表现做学习规划输入

## 第二批实施顺序

1. 先导原始旧表到隔离库
2. 建第二批中间表
3. 清洗编程题记录
4. 清洗作业记录与题目关联
5. 同步到正式业务表
6. 先接教师端分析页
7. 再接学生端诊断与学习规划

## 第二批风险控制

- 不直接导入当前主业务表
- 不覆盖现有作业和考试业务表
- 继续沿用“未绑定则兜底”的页面策略
- 编程题代码文本先原样保留，后续再做结构化分析
