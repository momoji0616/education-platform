# 第一批数据清洗脚本设计

本文档定义第一批旧数据清洗脚本的职责、输入输出和处理规则。

第一批输入表：

- `tk_choice1`
- `tk_choice2`
- `dt_choice`

第一批输出表：

- `legacy_staging_question_catalog`
- `legacy_staging_question_bank`
- `legacy_staging_student_answer`

目标：

- 为真正编写导入脚本提供明确规格
- 避免实施时边写边改字段口径
- 确保清洗过程稳定、幂等、可回溯

关联文件：

- [LEGACY_BATCH1_SYNC_FLOW.md](/E:/education-platform/deploy/LEGACY_BATCH1_SYNC_FLOW.md)
- [LEGACY_STAGING_SCHEMA.md](/E:/education-platform/deploy/LEGACY_STAGING_SCHEMA.md)
- [LEGACY_BATCH1_DEV_TASKS.md](/E:/education-platform/deploy/LEGACY_BATCH1_DEV_TASKS.md)

## 一、脚本组织建议

建议将第一批脚本拆成 4 个步骤脚本：

1. `sync_catalogs`
2. `sync_questions`
3. `sync_answers`
4. `promote_batch1`

职责分别为：

- `sync_catalogs`
  旧目录表 -> 中间目录表
- `sync_questions`
  旧题库题目 -> 中间题库表
- `sync_answers`
  旧作答记录 -> 中间作答表
- `promote_batch1`
  中间表 -> 正式业务表

这样拆分的好处：

- 出错时容易定位
- 可单独重跑
- 每一步都能独立校验

## 二、脚本公共要求

### 1. 幂等性

脚本必须支持重复执行，不产生重复脏数据。

建议方式：

- 按来源唯一键查重
- 采用 `insert ... on duplicate key update`
- 或先查后更新

### 2. 可追溯

每条写入必须保留：

- `source_system`
- 来源主键
- 原始 payload

### 3. 可统计

每次执行应输出：

- 读取条数
- 新增条数
- 更新条数
- 跳过条数
- 异常条数

### 4. 可中断恢复

建议支持：

- 全量执行
- 按时间窗执行
- 按主键范围执行

## 三、脚本 1：目录清洗 `sync_catalogs`

### 输入

- 旧表 `tk_choice1`

### 输出

- 中间表 `legacy_staging_question_catalog`

### 读取字段

- `id`
- `id1`
- `chapter`
- `title`
- `username`
- `count`
- `type`
- `limit`
- `time`

### 清洗规则

1. `source_id` <- `id`
2. `source_catalog_id` <- `id1`
3. `source_chapter_code` <- `chapter`
4. `catalog_name` <- `title.trim()`
5. `question_count` <- `count`，为空时置 0
6. `question_type` 固定写 `choice`
7. `owner_username` <- `username`
8. `raw_type` <- `type`
9. `raw_limit` <- `limit`
10. `raw_payload` 保存整行原始 JSON

### 过滤规则

以下数据跳过：

- `title` 为空
- `id1` 为空
- `chapter` 为空

### 唯一键建议

- `source_system + source_catalog_id + source_chapter_code`

### 日志建议

输出：

- 有效目录数
- 空标题数
- 重复目录数

## 四、脚本 2：题库清洗 `sync_questions`

### 输入

- 旧表 `tk_choice2`

### 输出

- 中间表 `legacy_staging_question_bank`

### 读取字段

- `id`
- `id1`
- `id2`
- `chapter`
- `question`
- `ans`
- `jx`
- `notice`
- `kb`
- `link`

### 清洗规则

1. `source_question_id` <- `id`
2. `source_catalog_id` <- `id1`
3. `source_question_no` <- `id2`
4. `question_type` 固定写 `choice`
5. `chapter_code` <- `chapter`
6. `raw_content` <- `question`
7. `standard_answer` <- `ans.trim()`
8. `analysis` <- `jx`
9. `knowledge_point` <- `kb`
10. `raw_payload` 保存整行原始 JSON

### 题干与选项拆分规则

输入样式通常类似：

```text
按照“先进先出”原则组织数据的数据结构是
A:队列
B:栈
C:双向链表
D:二叉树
```

拆分规则建议：

1. 先按换行切分
2. 第一段或非选项段拼为 `question_stem`
3. 识别前缀：
   - `A:`
   - `B:`
   - `C:`
   - `D:`
   - 扩展可兼容 `A：`
4. 解析成：

```json
[
  {"label":"A","content":"队列"},
  {"label":"B","content":"栈"}
]
```

### 异常处理

如果选项解析失败：

- `question_stem` 写原始全文
- `options_json` 置空
- 该题记录为“解析失败但保留”

如果答案为空：

- 进入异常清单
- 默认不写正式业务表

### 唯一键建议

- `source_system + source_question_id`

### 日志建议

输出：

- 总题目数
- 成功拆分选项数
- 解析失败数
- 空答案数

## 五、脚本 3：作答清洗 `sync_answers`

### 输入

- 旧表 `dt_choice`

### 输出

- 中间表 `legacy_staging_student_answer`

### 读取字段

- `id`
- `id1`
- `id2`
- `username`
- `ans`
- `score`
- `zy_id`
- `time`

### 清洗规则

1. `source_record_id` <- `id`
2. `student_no` <- `username.trim()`
3. `answer_type` 固定写 `choice`
4. `source_catalog_id` <- `id1`
5. `source_question_no` <- `id2`
6. `assignment_source_id` <- `zy_id`
7. `answer_content` <- `ans.trim()`
8. `standardized_score` <- 数值化 `score`
9. `is_correct` <- `score > 0`
10. `submit_time` <- `time`
11. `raw_payload` 保存整行原始 JSON

### 关联规则

在中间层阶段可以先不强制写入 `source_question_id`，
后续通过：

- `source_catalog_id + source_question_no`

去关联 `legacy_staging_question_bank`

### 异常处理

以下数据进入异常清单：

- 学号为空
- 题号为空
- 时间格式异常

以下数据保留但标记异常：

- 答案为空
- 分数为负值

### 唯一键建议

- `source_system + source_record_id`

### 日志建议

输出：

- 总作答数
- 正确作答数
- 错误作答数
- 空答案数
- 异常记录数

## 六、脚本 4：正式表同步 `promote_batch1`

### 输入

- `legacy_staging_question_catalog`
- `legacy_staging_question_bank`
- `legacy_staging_student_answer`

### 输出

- `edu_question_catalog`
- `edu_question_bank`
- `edu_student_answer_record`

### 步骤

#### 1. 同步目录

规则：

- 生成 `catalog_code`
- 若存在则更新
- 若不存在则新增

建议编码：

- `PICLASS-C-{source_catalog_id}-{source_chapter_code}`

#### 2. 同步题目

规则：

- 生成 `question_code`
- 回连 `catalog_id`
- 题目状态默认 `0`

建议编码：

- `PICLASS-Q-{source_question_id}`

#### 3. 同步作答

规则：

- 先通过 `source_catalog_id + source_question_no` 找到正式 `question_id`
- 找不到题目的作答不进入正式表
- 已存在 `source_record_id` 的只更新，不重复插入

## 七、异常输出建议

建议每个脚本都生成异常结果文件或异常表，至少分类记录：

- `catalog_missing_fields`
- `question_parse_failed`
- `question_missing_answer`
- `answer_missing_student`
- `answer_missing_question`

## 八、最小验证样例

每个脚本执行后，建议都做人工抽样：

### `sync_catalogs`

- 抽 5 条目录
- 确认章节编码、目录名、题量一致

### `sync_questions`

- 抽 10 道题
- 确认题干、选项、答案、解析一致

### `sync_answers`

- 抽 10 条作答
- 确认学号、作答答案、得分、正误一致

### `promote_batch1`

- 抽 5 条目录、10 道题、10 条作答
- 确认正式表和中间层映射一致

## 九、实施建议

真正写脚本时，推荐优先使用：

- Spring Boot 独立同步 service
- 或单独脚本工具模块

不建议：

- 直接在页面触发导入
- 在 Controller 内嵌大段清洗逻辑

## 十、当前状态

本文档只完成脚本规格设计，不包含：

- 实际 Python/Java 清洗代码
- 实际导入执行
- 异常输出文件实现

下一步最适合继续：

- 输出第一批实际代码改造清单
- 或直接开始从“最安全的一步”实施：建中间层与正式业务表
