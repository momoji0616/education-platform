@echo off
REM 设置 JAVA_HOME 环境变量
set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_202
set PATH=%JAVA_HOME%\bin;%PATH%

REM 切换到项目目录
cd /d %~dp0

REM 检查 Redis 是否运行
echo 正在检查 Redis 服务...
netstat -ano | findstr :6379 >nul
if %errorlevel% neq 0 (
    echo [警告] Redis 服务未运行！
    echo 请先启动 Redis 服务，然后重新运行此脚本。
    echo.
    echo 如果已安装 Redis，可以使用以下命令启动：
    echo   redis-server
    echo.
    echo 如果未安装 Redis，请参考 README.md 中的安装说明。
    pause
    exit /b 1
)

REM 检查 MySQL 是否运行
echo 正在检查 MySQL 服务...
netstat -ano | findstr :3306 >nul
if %errorlevel% neq 0 (
    echo [警告] MySQL 服务未运行！
    echo 请确保 MySQL 服务已启动。
    pause
)

REM 运行 Spring Boot 应用
echo.
echo 正在启动 RuoYi 后端服务...
echo 请确保以下服务已启动：
echo   - MySQL (端口 3306)
echo   - Redis (端口 6379)
echo.
cd ruoyi-admin
mvn spring-boot:run

pause
