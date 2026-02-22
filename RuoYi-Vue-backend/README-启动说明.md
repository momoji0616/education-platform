# RuoYi 后端启动说明

## 前置要求

### 1. Java 环境
- **JDK 1.8**（不是 JRE）
- 已设置 `JAVA_HOME` 环境变量指向 JDK 安装目录
- 例如：`C:\Program Files\Java\jdk1.8.0_202`

### 2. MySQL 数据库
- MySQL 5.7+ 或 MySQL 8.0+
- 已创建数据库 `ry-vue`
- 已导入初始化 SQL 脚本（`sql/ry_20250522.sql`）
- 数据库配置在 `application-druid.yml` 中：
  ```yaml
  url: jdbc:mysql://localhost:3306/ry-vue?...
  username: root
  password: 123456
  ```

### 3. Redis 服务（必需）
- Redis 3.0+ 
- 默认端口：6379
- 无密码（或根据配置修改）

## Redis 安装方法

### Windows 安装 Redis

#### 方法 1：使用 WSL（推荐）
```bash
# 在 WSL 中安装
sudo apt update
sudo apt install redis-server
sudo service redis-server start
```

#### 方法 2：使用 Windows 版本
1. 下载 Redis for Windows：
   - 官方不提供 Windows 版本，可以使用：
   - Memurai（商业版，有免费版本）
   - 或使用 WSL 运行 Linux 版本的 Redis

2. 或使用 Docker：
   ```bash
   docker run -d -p 6379:6379 --name redis redis:latest
   ```

#### 方法 3：使用 Chocolatey
```powershell
choco install redis-64
```

### 启动 Redis

**Windows 服务方式：**
```powershell
# 如果 Redis 安装为服务
net start redis
```

**命令行方式：**
```bash
redis-server
```

**Docker 方式：**
```bash
docker start redis
```

### 验证 Redis 是否运行
```powershell
# 检查端口
netstat -ano | findstr :6379

# 或使用 Redis 客户端测试
redis-cli ping
# 应该返回 PONG
```

## 启动步骤

### 1. 安装项目依赖
```powershell
cd E:\tensorflow_education\tensorflow_education\RuoYi-Vue-backend
mvn clean install -DskipTests
```

### 2. 确保服务已启动
- ✅ MySQL 服务运行中（端口 3306）
- ✅ Redis 服务运行中（端口 6379）

### 3. 启动应用

**方式 1：使用启动脚本（推荐）**
```powershell
.\start.bat
# 或
.\start.ps1
```

**方式 2：手动启动**
```powershell
cd ruoyi-admin
$env:JAVA_HOME = "C:\Program Files\Java\jdk1.8.0_202"
mvn spring-boot:run
```

**方式 3：直接运行 JAR**
```powershell
cd ruoyi-admin
$env:JAVA_HOME = "C:\Program Files\Java\jdk1.8.0_202"
java -jar target/ruoyi-admin.jar
```

## 常见问题

### 问题 1：无法连接到 Redis
**错误信息：** `Unable to connect to Redis; Unable to connect to localhost:6379`

**解决方法：**
1. 检查 Redis 是否已安装
2. 启动 Redis 服务
3. 验证端口 6379 是否被占用

### 问题 2：无法连接到 MySQL
**错误信息：** `Communications link failure`

**解决方法：**
1. 检查 MySQL 服务是否运行
2. 验证数据库 `ry-vue` 是否已创建
3. 检查 `application-druid.yml` 中的数据库配置

### 问题 3：JAVA_HOME 未设置
**错误信息：** `No compiler is provided in this environment`

**解决方法：**
1. 安装 JDK（不是 JRE）
2. 设置 `JAVA_HOME` 环境变量
3. 将 `%JAVA_HOME%\bin` 添加到 PATH

## 访问地址

启动成功后，访问：
- 后端 API：http://localhost:8080
- Swagger 文档：http://localhost:8080/swagger-ui/index.html
- Druid 监控：http://localhost:8080/druid/index.html

## 默认账号

- 用户名：`admin`
- 密码：`admin123`（首次登录后需要修改）
