# 环境变量与部署约定

本项目当前采用“前端 -> Spring Boot -> FastAPI”调用链：

- 前端只访问 Spring Boot 业务接口
- Spring Boot 通过 `education.ai.base-url` 代理调用 FastAPI
- FastAPI 仅负责 RAG、成绩预测、AI 批改等 AI 能力

## 1. 需要配置的变量

### Spring Boot

由以下配置读取：

- [application-dev.yml](/E:/education-platform/backend/ruoyi-admin/src/main/resources/application-dev.yml)
- [application-prod.yml](/E:/education-platform/backend/ruoyi-admin/src/main/resources/application-prod.yml)
- [application-dev.yml](/E:/education-platform/backend/zhiyu/src/main/resources/application-dev.yml)
- [application-prod.yml](/E:/education-platform/backend/zhiyu/src/main/resources/application-prod.yml)

核心变量：

```env
EDUCATION_AI_BASE_URL=http://127.0.0.1:8000
```

说明：

- 开发环境默认走本地 FastAPI：`http://127.0.0.1:8000`
- 生产环境建议通过环境变量覆盖，不要把正式地址硬编码进仓库

### FastAPI

建议在 [ai_service/.env.example](/E:/education-platform/ai_service/.env.example) 基础上创建自己的 `.env`：

```env
DASHSCOPE_API_KEY=your_dashscope_api_key
QWEN_API_KEY=your_qwen_api_key
AI_DB_HOST=127.0.0.1
AI_DB_PORT=3306
AI_DB_NAME=education_ai
AI_DB_USER=root
AI_DB_PASSWORD=your_db_password
```

说明：

- `DASHSCOPE_API_KEY` / `QWEN_API_KEY` 用于通义千问等模型能力
- 数据库变量仅在 AI 服务需要数据库持久化时使用

### 前端

前端继续只保留业务后端入口，例如：

- [frontend/.env.development](/E:/education-platform/frontend/.env.development)
- [frontend/.env.production](/E:/education-platform/frontend/.env.production)

当前不再建议使用旧的 `VITE_AI_BASE_API` 直连 FastAPI。

## 2. 推荐启动方式

### 开发环境

1. 启动 FastAPI，并准备 `.env`
2. 设置 `EDUCATION_AI_BASE_URL=http://127.0.0.1:8000`
3. 启动 Spring Boot
4. 启动前端

### 生产环境

1. 将 `EDUCATION_AI_BASE_URL` 指向正式 AI 服务地址
2. 在服务器环境变量中注入模型密钥
3. 前端只发布业务后端入口，不暴露 AI 直连地址

## 3. 安全建议

- 不要提交真实 API Key 到仓库
- 不要在前端环境变量里放模型密钥
- 如果真实密钥已经写进本地 `.env`，上线前建议更换一次
