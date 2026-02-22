@echo off
REM 切换到 rag 目录
cd /d %~dp0

REM 检查 Python 是否安装
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 Python，请先安装 Python 3.7+
    pause
    exit /b 1
)

REM 检查依赖是否安装
echo 正在检查依赖...
python -c "import fastapi" >nul 2>&1
if %errorlevel% neq 0 (
    echo [警告] 缺少依赖，正在安装...
    pip install fastapi uvicorn chromadb sentence-transformers pandas scikit-learn tensorflow pysqlite3-binary
) else (
    REM 检查 sqlite3 版本
    python -c "import sqlite3; print(sqlite3.sqlite_version)" 2>nul | findstr /R "3\.[3-9][5-9]\|3\.[4-9][0-9]\|[4-9]\." >nul
    if %errorlevel% neq 0 (
        echo [警告] sqlite3 版本可能过低，安装 pysqlite3-binary...
        pip install pysqlite3-binary
    )
)

REM 启动服务
echo.
echo 正在启动 RAG 服务...
echo 服务地址: http://127.0.0.1:8000
echo API 文档: http://127.0.0.1:8000/docs
echo.
uvicorn main:app --reload --port 8000 --host 0.0.0.0

pause
