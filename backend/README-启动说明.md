# 后端启动说明

本文档说明本项目 Spring Boot 后端的本地启动方式。当前后端作为业务与权限中心，负责：

- 用户、角色、权限
- 作业、考试、成绩、任务
- 聊天、论坛
- AI 服务代理 `/education/ai/*`

## 1. 前置要求

### Java

- JDK 8
- 必须是 JDK，不是 JRE
- 正确设置 `JAVA_HOME`

### 数据库

- MySQL 5.7+ 或 8.0+
- 已创建数据库 `ry-vue`
- 已导入若依基础 SQL

### Redis

- Redis 已启动
- 默认端口 `6379`

### AI 服务

后端当前通过环境变量或 profile 配置读取 AI 地址：

```env
EDUCATION_AI_BASE_URL=http://127.0.0.1:8000
```

相关配置文件：

- [application.yml](/E:/education-platform/backend/ruoyi-admin/src/main/resources/application.yml)
- [application-dev.yml](/E:/education-platform/backend/ruoyi-admin/src/main/resources/application-dev.yml)
- [application-prod.yml](/E:/education-platform/backend/ruoyi-admin/src/main/resources/application-prod.yml)
- [application-dev.yml](/E:/education-platform/backend/zhiyu/src/main/resources/application-dev.yml)
- [application-prod.yml](/E:/education-platform/backend/zhiyu/src/main/resources/application-prod.yml)

## 2. 启动顺序

建议顺序：

1. MySQL
2. Redis
3. FastAPI AI 服务
4. Spring Boot 后端

## 3. 启动命令

### 编译聚合工程

```powershell
cd E:\education-platform\backend
mvn clean install -DskipTests
```

### 启动管理端入口

```powershell
cd E:\education-platform\backend\ruoyi-admin
mvn spring-boot:run
```

如果你使用 JAR 运行：

```powershell
cd E:\education-platform\backend\ruoyi-admin
java -jar target/ruoyi-admin.jar
```

## 4. 当前后端模块说明

### 启动入口

- [ruoyi-admin](/E:/education-platform/backend/ruoyi-admin)

### 教育业务模块

- [zhiyu](/E:/education-platform/backend/zhiyu)

主要控制器包括：

- [EduPadController.java](/E:/education-platform/backend/zhiyu/src/main/java/com/ruoyi/student/controller/EduPadController.java)
- [EduPadTeacherController.java](/E:/education-platform/backend/zhiyu/src/main/java/com/ruoyi/student/controller/EduPadTeacherController.java)
- [EduPadStudentController.java](/E:/education-platform/backend/zhiyu/src/main/java/com/ruoyi/student/controller/EduPadStudentController.java)
- [EduPadChatController.java](/E:/education-platform/backend/zhiyu/src/main/java/com/ruoyi/student/controller/EduPadChatController.java)
- [EduPadForumController.java](/E:/education-platform/backend/zhiyu/src/main/java/com/ruoyi/student/controller/EduPadForumController.java)
- [EduAiController.java](/E:/education-platform/backend/zhiyu/src/main/java/com/ruoyi/student/controller/EduAiController.java)

## 5. 常见问题

### `No compiler is provided in this environment`

原因：

- 当前环境只有 JRE，没有 JDK

处理：

- 安装 JDK 8
- 检查 `JAVA_HOME`
- 确认 `javac -version` 可用

### 无法连接 Redis

处理：

- 确认 Redis 已启动
- 检查 `localhost:6379`
- 核对后端 Redis 配置

### 无法调用 AI 接口

处理：

1. 先确认 FastAPI 已启动
2. 确认 `EDUCATION_AI_BASE_URL` 是否正确
3. 检查 `/education/ai/*` 代理接口是否已登录访问

## 6. 验证地址

- 后端 API：`http://localhost:8080`
- Swagger：`http://localhost:8080/swagger-ui/index.html`
- Druid：`http://localhost:8080/druid/index.html`
