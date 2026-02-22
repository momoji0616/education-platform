@echo off
echo ========================================
echo Redis 启动脚本
echo ========================================
echo.

REM 检查 Redis 是否已运行
netstat -ano | findstr :6379 >nul
if %errorlevel% equ 0 (
    echo [信息] Redis 已经在运行中！
    pause
    exit /b 0
)

echo [信息] 正在尝试启动 Redis...
echo.

REM 方法 1：尝试作为 Windows 服务启动
net start redis >nul 2>&1
if %errorlevel% equ 0 (
    echo [成功] Redis 服务已启动！
    pause
    exit /b 0
)

REM 方法 2：尝试直接运行 redis-server
where redis-server >nul 2>&1
if %errorlevel% equ 0 (
    echo [信息] 找到 redis-server，正在启动...
    start "Redis Server" redis-server
    timeout /t 2 >nul
    netstat -ano | findstr :6379 >nul
    if %errorlevel% equ 0 (
        echo [成功] Redis 已启动！
        pause
        exit /b 0
    )
)

REM 方法 3：尝试使用 Docker
docker ps -a | findstr redis >nul 2>&1
if %errorlevel% equ 0 (
    echo [信息] 找到 Redis Docker 容器，正在启动...
    docker start redis >nul 2>&1
    if %errorlevel% equ 0 (
        echo [成功] Redis Docker 容器已启动！
        pause
        exit /b 0
    )
)

echo [错误] 无法启动 Redis！
echo.
echo 请选择以下方式之一安装和启动 Redis：
echo.
echo 1. 使用 WSL（推荐）：
echo    wsl
echo    sudo apt install redis-server
echo    sudo service redis-server start
echo.
echo 2. 使用 Docker：
echo    docker run -d -p 6379:6379 --name redis redis:latest
echo.
echo 3. 使用 Chocolatey：
echo    choco install redis-64
echo.
echo 4. 下载 Windows 版本的 Redis（如 Memurai）
echo.
pause
