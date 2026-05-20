# 第一批后端查询逻辑设计

本文档定义第一批旧数据接入后，后端在查询层应如何组织数据。

适用范围：

- `edu_question_catalog`
- `edu_question_bank`
- `edu_student_answer_record`

服务页面：

- [practice.vue](/E:/education-platform/frontend/src/views/education/student/practice.vue)
- [report.vue](/E:/education-platform/frontend/src/views/education/student/report.vue)
- [analysis.vue](/E:/education-platform/frontend/src/views/education/teacher/analysis.vue)

设计目标：

- 先把查询口径和聚合逻辑固定
- 后续实现时避免“接口有了，但口径混乱”
- 确保学生、教师、管理者的权限边界不被打破

## 一、查询层分工建议

建议新增 3 组查询服务：

1. `EduQuestionQueryService`
2. `EduStudentDiagnosisService`
3. `EduTeacherAnalysisService`

职责划分：

- `EduQuestionQueryService`
  负责目录、题库、推荐题明细的基础查询
- `EduStudentDiagnosisService`
  负责学生本人画像、章节诊断、错题统计
- `EduTeacherAnalysisService`
  负责教师班级维度的学情分析

## 二、统一过滤规则

### 学生接口

后端必须自动绑定：

- 当前登录学生 `student_no`

禁止：

- 前端传学号查询他人数据

### 教师接口

后端必须自动绑定：

- 当前教师已管理的 `class_code`

禁止：

- 教师越权查其他班级

### 管理者

第一批不接入这些 Pad 端题库分析接口。

## 三、基础查询逻辑

### 1. 题库目录查询

数据源：

- `edu_question_catalog`

推荐口径：

- 只返回 `status='0'`
- 可按 `course_name` 过滤
- 可按 `question_type='choice'` 过滤

返回字段建议：

- `id`
- `catalogCode`
- `catalogName`
- `courseName`
- `chapterCode`
- `chapterName`
- `questionCount`

排序建议：

- 先按 `course_name`
- 再按 `chapter_code`

### 2. 题库题目查询

数据源：

- `edu_question_bank`

推荐口径：

- 只返回 `status='0'`
- 支持按：
  - `catalog_id`
  - `course_name`
  - `chapter_code`
  - `knowledge_point`
  - `difficulty_level`

返回字段建议：

- `id`
- `questionCode`
- `questionType`
- `courseName`
- `chapterCode`
- `chapterName`
- `questionStem`
- `optionsJson`
- `standardAnswer`
- `analysis`
- `knowledgePoint`
- `difficultyLevel`

## 四、学生端查询逻辑

### 1. 我的练习推荐

接口：

- `GET /education/student/recommendations/practice`

核心逻辑：

1. 查当前学生最近一段时间的答题记录
2. 按章节统计：
   - 作答数
   - 正确数
   - 正确率
3. 找出正确率最低的章节
4. 在 `edu_question_bank` 中优先选取该章节题目
5. 过滤学生已做过太多次的题目
6. 返回推荐题

推荐策略建议：

- 优先级 1：学生错过的题
- 优先级 2：同章节未做题
- 优先级 3：同知识点近似题

推荐题量建议：

- 正确率 < 60%：6-8 题
- 60% <= 正确率 < 80%：4-6 题
- 正确率 >= 80%：3-4 题

### 2. 我的诊断概览

接口：

- `GET /education/student/diagnosis/overview`

核心逻辑：

按学生维度聚合：

- 总作答数
- 总正确数
- 总正确率
- 最近 7 天正确率
- 最近 30 天正确率
- 低正确率章节数

风险分层建议：

- 高风险：
  - 总正确率 < 60%
  - 或最近 7 天正确率明显下滑
- 中风险：
  - 总正确率 60%-75%
- 低风险：
  - 总正确率 >= 75%

### 3. 我的章节诊断

接口：

- `GET /education/student/diagnosis/chapters`

核心逻辑：

分组键：

- `course_name`
- `chapter_code`
- `chapter_name`

统计值：

- `answerCount`
- `correctCount`
- `wrongCount`
- `correctRate`

风险等级建议：

- `high`：正确率 < 60%
- `medium`：60% <= 正确率 < 80%
- `low`：正确率 >= 80%

排序建议：

- 按正确率升序
- 再按作答数降序

### 4. 我的高频错题

接口：

- `GET /education/student/diagnosis/wrong-questions`

核心逻辑：

分组键：

- `question_id`

统计值：

- 错误次数
- 最近错误时间

回连：

- `edu_question_bank`

返回重点：

- 题干
- 章节
- 知识点
- 错误次数
- 标准答案
- 解析

### 5. 我的知识点薄弱项

接口：

- `GET /education/student/diagnosis/knowledge-points`

核心逻辑：

前提：

- `knowledge_point` 不能为空

分组键：

- `knowledge_point`

统计值：

- 作答数
- 错误数
- 正确率

排序建议：

- 按正确率升序
- 再按错误数降序

## 五、教师端查询逻辑

### 1. 班级总体诊断概览

接口：

- `GET /education/teacher/analysis/overview`

核心逻辑：

1. 先查教师所管理学生范围
2. 在 `edu_student_answer_record` 中按学生范围做聚合

返回建议：

- 班级人数
- 作答总数
- 总体正确率
- 低正确率章节数
- 高风险知识点数

### 2. 班级章节正确率

接口：

- `GET /education/teacher/analysis/chapters`

核心逻辑：

1. 根据教师绑定班级获取学生列表
2. 将这些学生的作答记录按章节聚合

分组键：

- `course_name`
- `chapter_code`
- `chapter_name`

统计值：

- `studentCount`
- `answerCount`
- `correctCount`
- `correctRate`

风险分层建议：

- `high`：正确率 < 60%
- `medium`：60% <= 正确率 < 75%
- `low`：正确率 >= 75%

### 3. 班级高频错题

接口：

- `GET /education/teacher/analysis/hot-wrong-questions`

核心逻辑：

分组键：

- `question_id`

统计值：

- 错误人数
- 错误次数

回连：

- `edu_question_bank`

排序建议：

- 先按错误人数降序
- 再按错误次数降序

### 4. 班级知识点热力

接口：

- `GET /education/teacher/analysis/knowledge-points`

核心逻辑：

分组键：

- `knowledge_point`

统计值：

- 作答数
- 错误数
- 正确率
- 涉及学生数

返回用途：

- 给教师端分析页做知识点薄弱分布

## 六、推荐的数据时间窗

为了避免第一批接口太重，建议默认查询时间窗：

- 学生推荐：最近 90 天
- 学生诊断：最近 90 天
- 教师分析：最近 90 天

如无时间窗参数，默认按最近 90 天。

## 七、建议的 Mapper / SQL 组织方式

建议新增 mapper：

- `EduQuestionMapper`
- `EduStudentDiagnosisMapper`
- `EduTeacherAnalysisMapper`

建议职责：

- `EduQuestionMapper`
  负责目录、题库、推荐候选题
- `EduStudentDiagnosisMapper`
  负责学生本人聚合统计
- `EduTeacherAnalysisMapper`
  负责班级维度聚合统计

避免：

- 把所有统计 SQL 堆到单个大 Mapper
- 在 Controller 里拼接统计逻辑

## 八、性能建议

第一批就应考虑索引：

- `edu_student_answer_record(student_no, submit_time)`
- `edu_student_answer_record(question_id, is_correct)`
- `edu_question_bank(course_name, chapter_code)`
- `edu_question_bank(knowledge_point)`

原因：

- 学生维度查本人诊断会频繁走 `student_no`
- 教师维度统计错题会频繁走 `question_id`

## 九、验收口径

查询层完成后，应能保证：

1. 同一学生重复请求诊断结果口径一致
2. 教师看到的班级统计不包含其他班学生
3. 同一章节正确率在学生端和教师端统计口径一致
4. 推荐题确实来自题库表，而不是前端静态模板

## 十、下一步建议

后续继续顺序：

1. 第一批前端 API 模块设计
2. 第一批页面字段映射设计
3. 再开始真正建表和写同步脚本

这样能继续保持“先定规则，再动代码”的节奏。
