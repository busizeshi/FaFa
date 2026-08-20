@echo off
REM FaFa Docker 服务管理脚本 (Windows)
REM 用于将文件上传到远程服务器

setlocal enabledelayedexpansion

echo ========================================
echo FaFa Docker 部署文件上传脚本
echo ========================================
echo.

REM 设置变量
set REMOTE_HOST=your_server_ip
set REMOTE_USER=busizeshi
set REMOTE_DIR=/home/busizeshi/docker
set LOCAL_DIR=%~dp0

REM 检查 scp 是否可用
where scp >nul 2>nul
if %errorlevel% neq 0 (
    echo [错误] 未找到 scp 命令，请安装 OpenSSH 客户端
    echo.
    echo 安装方法：
    echo 1. Windows 10/11: 设置 ^> 应用 ^> 可选功能 ^> 添加功能 ^> OpenSSH 客户端
    echo 2. 或使用 WinSCP、FileZilla 等工具手动上传文件
    pause
    exit /b 1
)

echo 请输入服务器 IP 地址:
set /p REMOTE_HOST=

echo.
echo 正在上传文件到 %REMOTE_USER%@%REMOTE_HOST%:%REMOTE_DIR%
echo.

REM 上传 docker-compose.yml
echo [1/3] 上传 docker-compose.yml...
scp "%LOCAL_DIR%docker-compose.yml" %REMOTE_USER%@%REMOTE_HOST%:%REMOTE_DIR%/
if %errorlevel% neq 0 (
    echo [错误] docker-compose.yml 上传失败
    pause
    exit /b 1
)

REM 上传 redis.conf
echo [2/3] 上传 redis.conf...
scp "%LOCAL_DIR%redis\config\redis.conf" %REMOTE_USER%@%REMOTE_HOST%:%REMOTE_DIR%/redis/config/
if %errorlevel% neq 0 (
    echo [错误] redis.conf 上传失败
    pause
    exit /b 1
)

REM 上传 deploy.sh
echo [3/3] 上传 deploy.sh...
scp "%LOCAL_DIR%deploy.sh" %REMOTE_USER%@%REMOTE_HOST%:%REMOTE_DIR%/
if %errorlevel% neq 0 (
    echo [错误] deploy.sh 上传失败
    pause
    exit /b 1
)

echo.
echo ========================================
echo 文件上传完成！
echo ========================================
echo.
echo 接下来请登录服务器执行以下命令：
echo.
echo   ssh %REMOTE_USER%@%REMOTE_HOST%
echo   cd %REMOTE_DIR%
echo   chmod +x deploy.sh
echo   ./deploy.sh
echo.
pause
