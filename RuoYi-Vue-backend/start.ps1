# 设置 JAVA_HOME 环境变量
$env:JAVA_HOME = "C:\Program Files\Java\jdk1.8.0_202"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# 切换到项目目录
Set-Location $PSScriptRoot

# 运行 Spring Boot 应用
Write-Host "正在启动 RuoYi 后端服务..." -ForegroundColor Green
mvn spring-boot:run
