# 项目答辩 PPT 提纲

本文档用于答辩、汇报或项目展示，内容基于当前仓库实际功能与已完成改造整理。

## 1. 封面

- 项目名称：基于 RAG 的个性化智能教育辅助系统
- 副标题：面向高校师生的智能教育辅助平台
- 作者信息：姓名、学院、专业、指导老师

## 2. 项目背景与意义

### 可讲要点

- 响应 AI 赋能教育、培育新质生产力的政策背景
- 当前高校教育系统普遍存在“重信息管理、轻智能融合”问题
- 学生需要个性化学习支持，教师需要减负增效工具
- 本项目目标是实现“AI 赋能学生学业、AI 减负教师教学”

### 建议页面标题

- 项目背景
- 研究意义

## 3. 项目目标

### 可讲要点

- 面向高校师生双重服务对象
- 构建学生端与教师端全场景功能体系
- 融合 RAG、神经网络成绩预测、大语言模型
- 实现学业支持、教学辅助、数据管理一体化

### 一句话总结

通过 AI 技术提升学生学习质量、降低教师重复劳动、优化教学决策。

## 4. 系统总体架构

### 可讲要点

- 前端：Vue 3 + Vite
- 后端：Spring Boot
- AI 服务：FastAPI
- AI 调用链：前端 -> Spring Boot -> FastAPI
- 主控端与 Pad 端严格分离

### 当前架构划分

- 主控端：管理者使用
- 教师 Pad：教师日常教学使用
- 学生 Pad：学生日常学习使用

### 建议展示图

可直接参考 [README.md](/E:/education-platform/README.md) 中的 Mermaid 架构图思路。

## 5. 角色与权限设计

### 可讲要点

- 管理者：全局成绩、任务下发、入口管理
- 教师：作业、考试、批改、学情分析、AI 助教
- 学生：作业、成绩、学习规划、诊断、资料整理、刷题、AI 助学
- 数据隔离原则：
  - 管理者看全局
  - 教师看本班
  - 学生只看本人

### 建议页面标题

- 角色权限设计

## 6. 学生端功能展示

### 建议拆成 4-6 页

#### 6.1 学习规划

- 个性化学习规划生成
- 基于成绩、作业、考试表现生成学习建议
- 页面参考：
  - [plan.vue](/E:/education-platform/frontend/src/views/education/student/plan.vue)

#### 6.2 学业诊断

- 多维度学业诊断报告
- 薄弱点识别、风险判断、改进建议
- 页面参考：
  - [report.vue](/E:/education-platform/frontend/src/views/education/student/report.vue)

#### 6.3 资料整理

- 课程资料智能整理与知识点提取
- 知识库上传、资料归纳、提问引导
- 页面参考：
  - [materials.vue](/E:/education-platform/frontend/src/views/education/student/materials.vue)

#### 6.4 智能刷题

- 智能习题生成与个性化刷题推荐
- 按成绩与作业表现推荐练习
- 页面参考：
  - [practice.vue](/E:/education-platform/frontend/src/views/education/student/practice.vue)

#### 6.5 学生 AI 助手

- 学业问答
- 知识检索
- 学习建议
- 页面参考：
  - [ai.vue](/E:/education-platform/frontend/src/views/education/student/ai.vue)

## 7. 教师端功能展示

### 建议拆成 4-5 页

#### 7.1 试卷生成

- 智能题库与试卷生成
- 页面参考：
  - [paper.vue](/E:/education-platform/frontend/src/views/education/teacher/paper.vue)

#### 7.2 教学问答

- 教学问答与政策咨询助手
- 页面参考：
  - [qa.vue](/E:/education-platform/frontend/src/views/education/teacher/qa.vue)

#### 7.3 学情分析

- 班级均分、及格率、优秀率、风险提示
- 页面参考：
  - [analysis.vue](/E:/education-platform/frontend/src/views/education/teacher/analysis.vue)

#### 7.4 AI 批改

- 作业 / 实验报告智能批改
- 支持单张批改与批量批改
- 页面参考：
  - [grading.vue](/E:/education-platform/frontend/src/views/education/teacher/grading.vue)

#### 7.5 教师 AI 聚合入口

- 教师端 AI 能力统一入口
- 页面参考：
  - [ai.vue](/E:/education-platform/frontend/src/views/education/teacher/ai.vue)

## 8. 主控端功能展示

### 可讲要点

- 全局成绩总览
- 教师任务创建
- 学生作业任务创建
- 主控端进入教师 Pad / 学生 Pad

### 页面参考

- [index.vue](/E:/education-platform/frontend/src/views/education/admin/index.vue)

## 9. 核心技术方案

### RAG

- 上传教学资料
- 检索增强生成
- 降低大模型幻觉

### 成绩预测

- 使用 TensorFlow / Keras 模型训练与推理
- 支持学生成绩预测与学习建议生成

### AI 批改

- 教师参考样卷
- 单张/批量图片批改
- 输出分数、评语、标注结果

### 工程架构

- Spring Boot 负责业务与鉴权
- FastAPI 只负责 AI 能力
- 前端统一 API 与路由收口

## 10. 本轮重构与优化成果

这一页很关键，建议单独讲。

### 可讲要点

- 主控端与 Pad 端完成拆分
- 教师端与学生端页面独立化
- 路由与 API 按角色重构
- Pad 大控制器拆分为多个控制器
- AI 从前端直连改为后端代理
- 学生成绩与权限隔离问题已收口
- 文档、环境变量、部署说明补齐

### 对应文档

- [DELIVERY_SUMMARY.md](/E:/education-platform/deploy/DELIVERY_SUMMARY.md)

## 11. 项目创新点

### 可讲要点

- RAG 与教育资料场景深度结合
- 教师端与学生端双角色 AI 赋能
- 教育业务与 AI 服务边界清晰
- 兼顾实际业务流程与智能功能落地

## 12. 当前不足与后续计划

### 可讲要点

- 部分页面仍可继续优化交互与展示细节
- 后端需在完整 JDK 环境中补编译与测试验证
- AI 服务返回结构和错误码可进一步统一
- 后续可继续扩展知识图谱、实验报告深度分析、正式部署方案

## 13. 结论

### 建议总结话术

本项目已完成从传统教育信息管理向“教育业务 + AI 智能辅助”融合平台的阶段性建设，形成了主控端、教师端、学生端、业务后端和 AI 服务分层协同的系统架构，具备较好的展示价值、扩展价值和落地潜力。

## 14. 答辩加分建议

### PPT 展示顺序建议

1. 背景与意义
2. 目标与架构
3. 学生端功能
4. 教师端功能
5. AI 技术实现
6. 重构优化成果
7. 创新点与后续计划

### 演示建议

- 先从主控端进入
- 再演示教师端发布作业、AI 批改、学情分析
- 最后演示学生端学习规划、学业诊断、资料整理、刷题推荐

### 回答老师提问时可强调

- 项目不是单纯聊天机器人，而是教育业务驱动的 AI 系统
- 已考虑角色权限、数据隔离、系统分层
- AI 已通过后端代理收口，职责边界更清晰
