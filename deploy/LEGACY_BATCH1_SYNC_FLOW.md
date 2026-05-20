# 第一批旧数据同步逻辑设计

本文档定义第一批旧数据从旧库进入当前教育平台的同步流程。

第一批范围仍然只包含：

- `tk_choice1`
- `tk_choice2`
- `dt_choice`

目标：

- 把“怎么导、怎么洗、怎么入业务表”写清楚
- 保证每一步都可回滚、可复查
- 为后续真正实施提供稳定操作顺序

关联文件：

- [LEGACY_BATCH1_PLAN.md](/E:/education-platform/deploy/LEGACY_BATCH1_PLAN.md)
- [LEGACY_STAGING_SCHEMA.md](/E:/education-platform/deploy/LEGACY_STAGING_SCHEMA.md)
- [LEGACY_BATCH1_BUSINESS_SCHEMA.md](/E:/education-platform/deploy/LEGACY_BATCH1_BUSINESS_SCHEMA.md)
- [piclass2.sql](/E:/education-platform/backend/sql/piclass2.sql)

## 一、总流程

第一批必须严格按下面顺序执行：

1. 旧库抽取 `tk_choice1`
2. 清洗并写入 `legacy_staging_question_catalog`
3. 旧库抽取 `tk_choice2`
4. 清洗并写入 `legacy_staging_question_bank`
5. 建立目录表与题库表映射关系
6. 旧库抽取 `dt_choice`
7. 清洗并写入 `legacy_staging_student_answer`
8. 同步目录到 `edu_question_catalog`
9. 同步题库到 `edu_question_bank`
10. 同步作答记录到 `edu_student_answer_record`
11. 做结果核验

注意：

- 不允许跳过中间层直接从旧库写正式业务表
- 不允许先导 `dt_choice` 再导题库

## 二、同步粒度

建议第一批采用：

- 全量初始化同步
- 后续按时间增量同步

推荐增量字段：

- `time`

适用表：

- `tk_choice1.time`
- `dt_choice.time`

说明：

- `tk_choice2` 本身没有 `time` 字段时，可通过主键范围或与目录表配合做增量

## 三、步骤拆解

### 步骤 1：目录表抽取

来源：

- 旧表 `tk_choice1`

写入：

- `legacy_staging_question_catalog`

清洗规则：

- `id1` -> `source_catalog_id`
- `chapter` -> `source_chapter_code`
- `title` -> `catalog_name`
- `count` -> `question_count`
- 第一批固定 `question_type='choice'`
- `username/type/limit` 原样保留

校验点：

- 同一个 `id1 + chapter` 不应重复写入多次
- `catalog_name` 不能为空
- `question_count` 为空时置 0

### 步骤 2：题库表抽取

来源：

- 旧表 `tk_choice2`

写入：

- `legacy_staging_question_bank`

清洗规则：

- `id` -> `source_question_id`
- `id1` -> `source_catalog_id`
- `id2` -> `source_question_no`
- `chapter` -> `chapter_code`
- `question` 拆为：
  - `question_stem`
  - `options_json`
- `ans` -> `standard_answer`
- `jx` -> `analysis`
- `kb` -> `knowledge_point`
- `question_type='choice'`

题目拆分规则建议：

- 识别 `A:`、`B:`、`C:`、`D:` 为选项
- 选项解析失败时：
  - `question_stem` 保存原始全文
  - `options_json` 置空
  - `raw_content` 保留原始题目

校验点：

- `source_question_id` 唯一
- `standard_answer` 不为空
- `question_stem` 不为空

### 步骤 3：目录与题库对齐

作用：

- 把 `tk_choice2` 的题目回连到 `tk_choice1` 的目录

匹配键：

- `source_catalog_id`
- `chapter_code`

对齐结果：

- 补齐 `course_name`
- 补齐 `chapter_name`

异常处理：

- 找不到目录的题目，不丢弃
- 标记为“未匹配目录”，后续人工复核

### 步骤 4：作答记录抽取

来源：

- 旧表 `dt_choice`

写入：

- `legacy_staging_student_answer`

清洗规则：

- `id` -> `source_record_id`
- `username` -> `student_no`
- `id1` -> `source_catalog_id`
- `id2` -> `source_question_no`
- `answer_type='choice'`
- `ans` -> `answer_content`
- `score` -> `standardized_score`
- `score > 0` -> `is_correct=1`
- `score = 0` -> `is_correct=0`
- `time` -> `submit_time`
- `zy_id` -> `assignment_source_id`

校验点：

- `student_no` 不为空
- `source_catalog_id/source_question_no` 不为空
- `submit_time` 能被正常解析

## 四、中间层到正式业务表同步

### 1. `legacy_staging_question_catalog -> edu_question_catalog`

同步规则：

- 生成稳定 `catalog_code`
  建议格式：`PICLASS-C-{source_catalog_id}-{source_chapter_code}`
- `catalog_name` 原样带入
- `course_name/chapter_name` 可先都取目录名称，后续细化
- `question_count` 原样带入
- `source_catalog_id` 保留

幂等规则：

- 按 `catalog_code` 做 upsert

### 2. `legacy_staging_question_bank -> edu_question_bank`

同步规则：

- 先按 `source_catalog_id + chapter_code` 找到 `edu_question_catalog`
- 生成稳定 `question_code`
  建议格式：`PICLASS-Q-{source_question_id}`
- 写入题干、选项、答案、解析、知识点
- `difficulty_level` 第一批可统一设为 `medium`

幂等规则：

- 按 `question_code` 做 upsert

### 3. `legacy_staging_student_answer -> edu_student_answer_record`

同步规则：

- 先按 `source_question_id` 或 `source_catalog_id + source_question_no` 找到 `edu_question_bank`
- 写入学生答题记录
- `assignment_id` 第一批可以为空
- `source_record_id` 保留

幂等规则：

- 按 `source_system + source_record_id` 做唯一同步

## 五、推荐的唯一键策略

### 中间层

- 目录：`source_system + source_catalog_id + source_chapter_code`
- 题目：`source_system + source_question_id`
- 作答：`source_system + source_record_id`

### 正式业务表

- 目录：`catalog_code`
- 题目：`question_code`
- 作答：`source_system + source_record_id`

## 六、异常处理策略

### 目录缺失

表现：

- 题目找不到对应目录

处理：

- 题目先入中间层
- 不立刻写入正式业务表
- 输出异常清单

### 选项解析失败

表现：

- 题干中选项格式不标准

处理：

- 保留原题全文
- `options_json` 为空
- 不中断整批同步

### 学生作答找不到题库

表现：

- `dt_choice` 中题目在题库表中不存在

处理：

- 保留到中间层
- 不同步进正式作答表
- 输出未匹配记录

## 七、第一批验收清单

同步逻辑实施后，要能核对：

1. 目录总量是否与旧库目录总量接近
2. 题目总量是否与旧库题目总量接近
3. 学生作答总量是否与旧库作答总量接近
4. 随机抽查题目，题干/选项/答案是否一致
5. 随机抽查学生作答，答案/分数/时间是否一致
6. 学生页能按真实题目给出推荐
7. 教师页能按章节统计班级正确率

## 八、后续实施建议

真正开始执行时，推荐继续按这个顺序：

1. 先写第一批数据清洗脚本
2. 再写中间层到正式表同步脚本
3. 再接后端查询接口
4. 最后接前端页面

## 九、当前状态

本文件仅完成同步逻辑设计，不包含：

- 可运行脚本
- 真正导库动作
- 后端接口实现
- 页面改造实现

下一步最适合继续：

- 输出第一批接口设计
- 或输出第一批数据清洗脚本方案
