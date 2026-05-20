# 第一批旧数据接入实施清单

本文档定义 `piclass2` 旧数据接入的第一批范围，目标是：

- 只接入最有价值、风险最低的数据
- 尽快增强当前已有功能
- 保持主控端、教师 Pad、学生 Pad 角色边界不变
- 不直接碰当前主业务表，先走中间清洗层

关联文件：

- [LEGACY_DATA_EXECUTION_ROADMAP.md](/E:/education-platform/deploy/LEGACY_DATA_EXECUTION_ROADMAP.md)
- [LEGACY_STAGING_SCHEMA.md](/E:/education-platform/deploy/LEGACY_STAGING_SCHEMA.md)
- [legacy_staging_schema.sql](/E:/education-platform/deploy/sql/legacy_staging_schema.sql)
- [piclass2.sql](/E:/education-platform/backend/sql/piclass2.sql)

## 一、第一批范围

第一批只接入下面 3 张旧表：

- `tk_choice1`
- `tk_choice2`
- `dt_choice`

对应中间表：

- `legacy_staging_question_catalog`
- `legacy_staging_question_bank`
- `legacy_staging_student_answer`

这批数据的价值最高，原因是：

- 能立即形成题库能力
- 能立即形成学生错题画像
- 能立即支撑诊断、刷题和教师学情分析
- 不依赖复杂的旧账号体系才能先跑通数据价值

## 二、第一批要解决的问题

### 学生端

先增强：

- 智能习题生成与个性化刷题推荐
- 多维度学业诊断报告

具体效果：

- 学生可以看到按章节/知识点的薄弱项
- 学生可以收到基于历史错题的推荐题
- 学生可以看到最近答题正确率变化

### 教师端

先增强：

- 班级学情智能分析

具体效果：

- 教师可以看到班级章节正确率
- 教师可以看到高频错题
- 教师可以看到知识点薄弱分布

### 暂不接入

第一批先不接：

- 编程题
- 作业明细
- 作业总分
- 班级和旧账号绑定

原因：

- 这些依赖关系更复杂
- 容易把第一批范围拉大
- 不利于“先稳后快”

## 三、第一批导入路径

### 第一步：导入隔离库

从旧库中抽取：

- `tk_choice1`
- `tk_choice2`
- `dt_choice`

来源：

- 隔离库 `education_legacy_piclass`

### 第二步：写入中间层

导入至：

- `legacy_staging_question_catalog`
- `legacy_staging_question_bank`
- `legacy_staging_student_answer`

### 第三步：同步到正式业务表

建议第一批新增正式业务表：

- `edu_question_catalog`
- `edu_question_bank`
- `edu_student_answer_record`

### 第四步：页面消费

先接入现有页面：

- 学生端 [practice.vue](/E:/education-platform/frontend/src/views/education/student/practice.vue)
- 学生端 [report.vue](/E:/education-platform/frontend/src/views/education/student/report.vue)
- 教师端 [analysis.vue](/E:/education-platform/frontend/src/views/education/teacher/analysis.vue)

## 四、字段清洗规则

### 1. `tk_choice1 -> legacy_staging_question_catalog`

处理规则：

- `id1` 作为目录主来源编号
- `chapter` 作为章节编码
- `title` 作为目录名
- `count` 作为题量
- `type` 和 `limit` 原样保留到 `raw_*`
- `question_type` 第一批统一标记为 `choice`

### 2. `tk_choice2 -> legacy_staging_question_bank`

处理规则：

- `question` 拆分成：
  - `question_stem`
  - `options_json`
- `ans` -> `standard_answer`
- `jx` -> `analysis`
- `kb` -> `knowledge_point`
- `id1 + id2` 作为旧题目唯一来源组合键
- 章节和目录信息回连 `tk_choice1`

题目选项拆分建议：

- 保留原始文本到 `raw_content`
- 再解析 `A:`、`B:`、`C:`、`D:` 为 JSON
- 解析失败时，仍保留题干全文，不丢数据

### 3. `dt_choice -> legacy_staging_student_answer`

处理规则：

- `username` -> `student_no`
- `ans` -> `answer_content`
- `score` -> `standardized_score`
- `score > 0` 初步判为 `is_correct=1`
- `score = 0` 判为 `is_correct=0`
- `id1/id2` 用于回连题库
- `zy_id` 保留为作业来源字段，后续第二批再用

## 五、第一批正式业务表建议

### 1. `edu_question_catalog`

作用：

- 供试卷生成、章节统计、知识点分组使用

### 2. `edu_question_bank`

作用：

- 供刷题、RAG 题目问答、组卷使用

### 3. `edu_student_answer_record`

作用：

- 供诊断报告、推荐算法、教师学情分析使用

## 六、第一批页面改造点

### 学生端 [practice.vue](/E:/education-platform/frontend/src/views/education/student/practice.vue)

建议改造：

- 当前推荐逻辑多为规则生成
- 改为读取真实错题和章节统计

建议新增展示：

- 最近错误最多章节
- 推荐练习题列表
- 同知识点强化练习
- 最近正确率趋势

### 学生端 [report.vue](/E:/education-platform/frontend/src/views/education/student/report.vue)

建议改造：

- 输出课程/章节维度的诊断
- 输出高频错题类型
- 输出知识点薄弱排名

### 教师端 [analysis.vue](/E:/education-platform/frontend/src/views/education/teacher/analysis.vue)

建议改造：

- 接入真实题库作答统计
- 班级维度输出章节正确率
- 题目维度输出错误热度
- 知识点维度输出薄弱分布

## 七、第一批后端改造点

建议新增后端模块：

- `legacy` 数据同步 service
- `question` 题库 service
- `analysis` 学情分析 service

建议新增接口方向：

- 学生错题推荐接口
- 学生章节正确率接口
- 教师班级章节分析接口
- 教师错题热点接口

注意：

- 这些接口仍然放在当前业务模块中
- 不把逻辑写进若依核心模块

## 八、第一批验收标准

做到下面这些，第一批就算成功：

1. 旧选择题题库能被导入并标准化保存
2. 学生历史选择题作答能按学生查询
3. 学生端刷题推荐来自真实历史错题
4. 学生端诊断报告能展示章节薄弱点
5. 教师端分析页能展示真实班级答题统计

## 九、第一批实施顺序

推荐顺序：

1. 建隔离库
2. 建中间层表
3. 设计清洗脚本
4. 导入 `tk_choice1`
5. 导入 `tk_choice2`
6. 导入 `dt_choice`
7. 建正式业务表
8. 写同步逻辑
9. 改学生刷题页
10. 改学生诊断页
11. 改教师分析页

## 十、当前状态

本文件是第一批实施方案，尚未执行：

- 隔离库导入
- 清洗脚本编写
- 正式业务表建表
- 接口开发
- 页面接入

下一步推荐：

- 输出第一批“正式业务表”建表设计
- 或者直接输出“第一批清洗脚本方案”
