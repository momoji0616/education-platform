# 切换到 rag 目录
Set-Location $PSScriptRoot

# 检查 Python 是否安装
try {
    $pythonVersion = python --version 2>&1
    Write-Host "Python 版本: $pythonVersion" -ForegroundColor Green
} catch {
    Write-Host "[错误] 未找到 Python，请先安装 Python 3.7+" -ForegroundColor Red
    exit 1
}

# 检查依赖是否安装
Write-Host "正在检查依赖..." -ForegroundColor Yellow
try {
    python -c "import fastapi" 2>&1 | Out-Null
    Write-Host "依赖检查通过" -ForegroundColor Green
    
    # 检查 sqlite3 版本
    $sqliteVersion = python -c "import sqlite3; print(sqlite3.sqlite_version)" 2>&1
    if ($sqliteVersion -match "^3\.(3[5-9]|[4-9])|^[4-9]") {
        Write-Host "sqlite3 版本: $sqliteVersion" -ForegroundColor Green
    } else {
        Write-Host "[警告] sqlite3 版本可能过低 ($sqliteVersion)，安装 pysqlite3-binary..." -ForegroundColor Yellow
        pip install pysqlite3-binary
    }
} catch {
    Write-Host "[警告] 缺少依赖，正在安装..." -ForegroundColor Yellow
    pip install fastapi uvicorn chromadb sentence-transformers pandas scikit-learn tensorflow pysqlite3-binary
}

# 启动服务
Write-Host ""
Write-Host "正在启动 RAG 服务..." -ForegroundColor Green
Write-Host "服务地址: http://127.0.0.1:8000" -ForegroundColor Cyan
Write-Host "API 文档: http://127.0.0.1:8000/docs" -ForegroundColor Cyan
Write-Host ""
uvicorn main:app --reload --port 8000 --host 0.0.0.0
