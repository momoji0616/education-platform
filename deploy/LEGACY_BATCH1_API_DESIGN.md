# 第一批旧数据接入接口设计

本文档定义第一批旧数据接入完成后，后端应提供给前端页面的接口设计。

第一批数据范围：

- `edu_question_catalog`
- `edu_question_bank`
- `edu_student_answer_record`

优先服务页面：

- [practice.vue](/E:/education-platform/frontend/src/views/education/student/practice.vue)
- [report.vue](/E:/education-platform/frontend/src/views/education/student/report.vue)
- [analysis.vue](/E:/education-platform/frontend/src/views/education/teacher/analysis.vue)

设计原则：

- 继续遵守主控端与 Pad 端分离
- 学生接口只返回本人数据
- 教师接口只返回本人班级数据
- 管理者不直接进入 Pad 端做题分析接口
- 前端接口统一放 `frontend/src/api/education`

## 一、接口分组建议

建议新增或扩展如下 API 模块：

- `frontend/src/api/education/student.js`
- `frontend/src/api/education/teacher.js`

后端建议新增 service/controller 能力：

- `student` 学业诊断相关接口
- `question` 题库与推荐相关接口
- `teacher` 班级学情分析相关接口

## 二、学生端接口

### 1. 获取我的练习推荐

建议路径：

- `GET /education/student/recommendations/practice`

用途：

- 给 [practice.vue](/E:/education-platform/frontend/src/views/education/student/practice.vue) 返回真实推荐题和推荐方向

建议入参：

- `courseName` 可选
- `chapterCode` 可选
- `limit` 可选

建议返回：

- 推荐强度
- 推荐题量
- 推荐方向
- 推荐题列表
- 推荐依据

建议返回结构：

```json
{
  "level": "基础巩固",
  "questionCount": 6,
  "focus": "数组与线性表",
  "reason": "最近 20 次练习中，该章节正确率偏低",
  "questions": [
    {
      "questionId": 1001,
      "questionType": "choice",
      "chapterCode": "2",
      "chapterName": "线性表",
      "difficultyLevel": "medium",
      "questionStem": "......"
    }
  ]
}
```

### 2. 获取我的诊断概览

建议路径：

- `GET /education/student/diagnosis/overview`

用途：

- 给 [report.vue](/E:/education-platform/frontend/src/views/education/student/report.vue) 返回总体诊断卡片

建议返回：

- 总作答数
- 总正确率
- 最近 7 天正确率
- 风险等级
- 主要薄弱章节数

### 3. 获取我的章节诊断

建议路径：

- `GET /education/student/diagnosis/chapters`

用途：

- 返回学生按章节的正确率、错题数和风险项

建议返回结构：

```json
[
  {
    "courseName": "数据结构",
    "chapterCode": "2",
    "chapterName": "线性表",
    "answerCount": 36,
    "correctRate": 58.3,
    "wrongCount": 15,
    "riskLevel": "high"
  }
]
```

### 4. 获取我的高频错题

建议路径：

- `GET /education/student/diagnosis/wrong-questions`

用途：

- 供诊断页和刷题页联动

建议入参：

- `limit` 可选

建议返回：

- 题目 ID
- 题干
- 所属章节
- 错误次数
- 标准答案
- 解析

### 5. 获取我的知识点薄弱项

建议路径：

- `GET /education/student/diagnosis/knowledge-points`

用途：

- 输出可用于学习规划和 RAG 提示的知识点弱项

## 三、教师端接口

### 1. 获取班级章节正确率

建议路径：

- `GET /education/teacher/analysis/chapters`

用途：

- 给 [analysis.vue](/E:/education-platform/frontend/src/views/education/teacher/analysis.vue) 返回班级章节维度表现

建议入参：

- `classCode` 可选
- `courseName` 可选

建议返回：

- 章节名称
- 作答人数
- 作答次数
- 平均正确率
- 风险等级

### 2. 获取班级高频错题

建议路径：

- `GET /education/teacher/analysis/hot-wrong-questions`

用途：

- 返回班级共性错题排行

建议返回：

- 题目 ID
- 题干摘要
- 章节
- 错误人数
- 错误次数
- 标准答案
- 解析

### 3. 获取班级知识点热力

建议路径：

- `GET /education/teacher/analysis/knowledge-points`

用途：

- 输出班级知识点薄弱分布

建议返回：

- 知识点名称
- 平均正确率
- 错误次数
- 风险等级

### 4. 获取班级总体诊断概览

建议路径：

- `GET /education/teacher/analysis/overview`

用途：

- 返回教师端顶部概览卡片

建议返回：

- 班级人数
- 作答总数
- 平均正确率
- 低正确率章节数
- 高风险知识点数

## 四、通用题库接口

### 1. 查询题库目录

建议路径：

- `GET /education/question/catalogs`

用途：

- 给学生刷题过滤器和教师分析筛选项使用

建议返回：

- 课程名
- 章节名
- 章节编码
- 题目数

### 2. 按章节查询题目

建议路径：

- `GET /education/question/bank`

建议入参：

- `courseName`
- `chapterCode`
- `questionType`
- `limit`

用途：

- 给练习推荐、章节浏览、后续组卷页使用

## 五、权限边界

### 学生接口

必须：

- 仅允许当前登录学生访问
- 后端按登录身份自动绑定 `student_no`
- 前端不能传任意学生学号查询别人数据

### 教师接口

必须：

- 仅允许当前登录教师访问
- 后端按教师已绑定班级过滤
- 教师不能直接查全校数据

### 通用题库接口

允许：

- 老师和学生都可访问

不允许：

- 管理者通过 Pad 端路径直接复用教师/学生接口

## 六、接口与页面映射

### 学生端 [practice.vue](/E:/education-platform/frontend/src/views/education/student/practice.vue)

优先调用：

- `/education/student/recommendations/practice`
- `/education/question/catalogs`

### 学生端 [report.vue](/E:/education-platform/frontend/src/views/education/student/report.vue)

优先调用：

- `/education/student/diagnosis/overview`
- `/education/student/diagnosis/chapters`
- `/education/student/diagnosis/wrong-questions`
- `/education/student/diagnosis/knowledge-points`

### 教师端 [analysis.vue](/E:/education-platform/frontend/src/views/education/teacher/analysis.vue)

优先调用：

- `/education/teacher/analysis/overview`
- `/education/teacher/analysis/chapters`
- `/education/teacher/analysis/hot-wrong-questions`
- `/education/teacher/analysis/knowledge-points`

## 七、建议的实现顺序

建议先实现：

1. 题库目录查询
2. 学生练习推荐
3. 学生章节诊断
4. 教师章节分析

然后再补：

5. 学生高频错题
6. 学生知识点薄弱项
7. 教师高频错题
8. 教师知识点热力

原因：

- 这样能最快把三个页面从“模板逻辑”切到“真实数据驱动”

## 八、当前状态

本文档只完成接口设计，不包含：

- Controller 代码
- Service 代码
- SQL 查询实现
- 前端 API 文件改造

下一步最适合继续：

- 设计第一批后端 service 查询逻辑
- 或者设计第一批前端 API 模块清单
