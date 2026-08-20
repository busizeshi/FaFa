# PowerShell 自动化清理脚本
# 服务器: 192.168.1.14
# 用户: busizeshi

$password = "JWDdmm@2552"
$server = "busizeshi@192.168.1.14"

Write-Host "=== 开始连接服务器并清理 Docker 环境 ===" -ForegroundColor Green
Write-Host ""

# 创建临时脚本文件
$scriptContent = @'
#!/bin/bash
echo "=== 开始清理 Docker 环境 ==="
echo ""

echo "[1/8] 停止所有运行中的容器..."
docker stop $(docker ps -q) 2>/dev/null || echo "没有运行中的容器"
echo ""

echo "[2/8] 删除所有容器..."
docker rm -f $(docker ps -aq) 2>/dev/null || echo "没有容器需要删除"
echo ""

echo "[3/8] 删除所有镜像..."
docker rmi -f $(docker images -q) 2>/dev/null || echo "没有镜像需要删除"
echo ""

echo "[4/8] 删除所有卷..."
docker volume rm $(docker volume ls -q) 2>/dev/null || echo "没有卷需要删除"
echo ""

echo "[5/8] 删除自定义网络..."
docker network prune -f 2>/dev/null || true
echo ""

echo "[6/8] 清理 Docker 系统..."
docker system prune -a -f --volumes
echo ""

echo "[7/8] 删除部署目录..."
echo "JWDdmm@2552" | sudo -S rm -rf /home/busizeshi/docker
echo "部署目录已删除"
echo ""

echo "[8/8] 验证清理结果..."
echo "容器数量: $(docker ps -a | wc -l)"
echo "镜像数量: $(docker images | wc -l)"
echo "卷数量: $(docker volume ls | wc -l)"
echo "目录状态: $(ls -d /home/busizeshi/docker 2>&1)"
echo ""

echo "=== 清理完成 ==="
'@

# 保存到本地临时文件
$tempScriptPath = "D:\dev\Java\FaFa\temp_clean.sh"
$scriptContent | Out-File -FilePath $tempScriptPath -Encoding UTF8 -NoNewline

Write-Host "步骤 1: 上传清理脚本到服务器..." -ForegroundColor Yellow

# 使用 PLINK 或 SCP 上传（需要手动输入密码）
Write-Host "请在以下命令中输入密码: $password" -ForegroundColor Cyan
scp $tempScriptPath ${server}:/home/busizeshi/temp_clean.sh

Write-Host ""
Write-Host "步骤 2: 执行清理脚本..." -ForegroundColor Yellow
Write-Host "请在以下命令中输入密码: $password" -ForegroundColor Cyan

# 执行脚本
ssh $server "chmod +x /home/busizeshi/temp_clean.sh && /home/busizeshi/temp_clean.sh"

Write-Host ""
Write-Host "步骤 3: 清理临时文件..." -ForegroundColor Yellow
ssh $server "rm -f /home/busizeshi/temp_clean.sh"

# 删除本地临时文件
Remove-Item $tempScriptPath -Force

Write-Host ""
Write-Host "=== 所有操作完成 ===" -ForegroundColor Green
