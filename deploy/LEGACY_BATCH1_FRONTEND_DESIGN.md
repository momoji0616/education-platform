# 第一批前端接入设计

本文档定义第一批旧数据接入后，前端 API 模块与页面字段如何落地。

适用页面：

- [practice.vue](/E:/education-platform/frontend/src/views/education/student/practice.vue)
- [report.vue](/E:/education-platform/frontend/src/views/education/student/report.vue)
- [analysis.vue](/E:/education-platform/frontend/src/views/education/teacher/analysis.vue)

设计目标：

- 不在页面内散写请求
- 继续遵守 `frontend/src/api` 集中管理
- 让现有页面从“静态规则生成”平滑过渡到“真实数据驱动”

## 一、前端 API 模块建议

建议在现有 API 模块上追加，不新建散乱文件。

### 学生端 API

文件：

- [student.js](/E:/education-platform/frontend/src/api/education/student.js)

建议新增方法：

- `getStudentPracticeRecommendations(params)`
- `getStudentDiagnosisOverview()`
- `getStudentDiagnosisChapters(params)`
- `getStudentWrongQuestions(params)`
- `getStudentWeakKnowledgePoints(params)`

### 教师端 API

文件：

- [teacher.js](/E:/education-platform/frontend/src/api/education/teacher.js)

建议新增方法：

- `getTeacherAnalysisOverview(params)`
- `getTeacherAnalysisChapters(params)`
- `getTeacherHotWrongQuestions(params)`
- `getTeacherKnowledgePoints(params)`

### 通用题库 API

建议位置：

- 可放在 [student.js](/E:/education-platform/frontend/src/api/education/student.js) 和 [teacher.js](/E:/education-platform/frontend/src/api/education/teacher.js) 中各自封装
- 或后续单独抽出 `question.js`

第一批建议先不拆独立文件，避免变动过大。

建议新增方法：

- `listQuestionCatalogs(params)`
- `listQuestionBank(params)`

## 二、页面改造原则

### 不直接推翻现有页面

第一批不建议整页重写，建议：

- 保留现有版式
- 先替换数据来源
- 再逐步去掉静态模板逻辑

这样最稳。

### 改造顺序

推荐顺序：

1. [practice.vue](/E:/education-platform/frontend/src/views/education/student/practice.vue)
2. [report.vue](/E:/education-platform/frontend/src/views/education/student/report.vue)
3. [analysis.vue](/E:/education-platform/frontend/src/views/education/teacher/analysis.vue)

原因：

- `practice` 最容易先从静态推荐切到真实推荐
- `report` 第二步接真实章节诊断
- `analysis` 最后接班级聚合统计

## 三、学生刷题页字段映射

页面：

- [practice.vue](/E:/education-platform/frontend/src/views/education/student/practice.vue)

### 当前问题

当前主要问题：

- 推荐题是模板生成
- 推荐强度和题量是前端规则推导
- 没有真实错题和章节数据驱动

### 第一批目标

改成由接口直接返回：

- 推荐强度
- 推荐题量
- 推荐方向
- 推荐题列表

### 建议字段映射

页面状态：

- `overview.level` <- `response.level`
- `overview.questionCount` <- `response.questionCount`
- `overview.focus` <- `response.focus`
- `overview.levelTip` <- `response.reason`
- `practiceList` <- `response.questions`

题目列表建议字段：

- `item.no` <- 前端序号
- `item.type` <- `questionType`
- `item.question` <- `questionStem`
- `item.difficulty` <- `difficultyLevel`
- `item.tip` <- `analysis` 或 `knowledgePoint`

### 第一批建议保留的前端逻辑

- 页面布局
- 练习参数表单
- 返回按钮与联动入口

### 第一批建议删除或降级的前端逻辑

- `practiceTemplates`
- 本地拼题逻辑
- 纯前端随机推荐逻辑

## 四、学生诊断页字段映射

页面：

- [report.vue](/E:/education-platform/frontend/src/views/education/student/report.vue)

### 当前问题

当前主要依赖：

- 作业完成率
- 考试均分
- 历史成绩

还没有真正使用题库作答数据。

### 第一批目标

让页面优先展示：

- 章节正确率
- 高频错题
- 薄弱知识点

### 建议字段映射

诊断总览：

- `overviewCards` <- `/diagnosis/overview`

风险列表：

- `riskItems` <- 由 `/diagnosis/chapters` 中的高风险章节生成

建议项：

- `adviceItems` <- 由 `overview + weakKnowledgePoints` 组合生成

题目/章节表格：

- `examRows` 这一块后续可逐步替换为：
  - 章节诊断列表
  - 错题排行

### 第一批建议新增展示

- 最薄弱 3 个章节
- 高频错题前 5
- 知识点薄弱项前 5

### 第一批建议暂保留

- 现有考试和作业统计卡片

原因：

- 第一批先增强，不直接丢掉现有数据链

## 五、教师分析页字段映射

页面：

- [analysis.vue](/E:/education-platform/frontend/src/views/education/teacher/analysis.vue)

### 当前问题

当前分析主要基于：

- 教师可见成绩
- 作业提交
- 考试分数

还没有真正基于题库作答记录做班级错题分析。

### 第一批目标

增加真实题库作答维度：

- 章节正确率
- 高频错题
- 知识点热力

### 建议字段映射

概览卡片：

- `overviewCards` <- `/analysis/overview`

风险诊断：

- `riskItems` <- `/analysis/chapters`

成绩表现表：

- 第一批可先替换成“章节表现表”或“高频错题表”
- 不建议一次性完全推翻现有结构

教学建议：

- `adviceItems` <- 根据：
  - 总体正确率
  - 高风险章节
  - 高频错题
  自动生成

## 六、页面改造的最小安全策略

第一批实施时建议遵守：

1. 先加新接口调用
2. 保留旧字段兜底
3. 新接口没数据时不让页面空白
4. 先替换局部卡片和列表，不一次性替换整页

这样可以保证：

- 旧逻辑还能兜底
- 新数据逐步接入
- 页面不会因为某个接口暂时没数据就整体坏掉

## 七、建议的数据兜底方式

### 学生刷题页

- 接口成功：用真实推荐
- 接口为空：退回原本模板推荐

### 学生诊断页

- 接口成功：优先显示章节诊断
- 接口为空：保留作业/考试统计

### 教师分析页

- 接口成功：优先显示章节和错题分析
- 接口为空：保留现有班级成绩分析

## 八、前端实施顺序

第一批前端建议按这个顺序改：

1. 给 [student.js](/E:/education-platform/frontend/src/api/education/student.js) 加学生题库分析接口
2. 给 [teacher.js](/E:/education-platform/frontend/src/api/education/teacher.js) 加教师题库分析接口
3. 改 [practice.vue](/E:/education-platform/frontend/src/views/education/student/practice.vue)
4. 改 [report.vue](/E:/education-platform/frontend/src/views/education/student/report.vue)
5. 改 [analysis.vue](/E:/education-platform/frontend/src/views/education/teacher/analysis.vue)

## 九、当前状态

本文件只完成前端接入设计，不包含：

- 实际 API 代码
- 页面代码改造
- 真实接口联调

下一步最适合继续：

- 设计第一批数据清洗脚本方案
- 或开始整理第一批实际开发任务清单
