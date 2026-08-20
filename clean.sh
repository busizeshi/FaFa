#!/bin/bash

###############################################################################
# Docker 环境完全清理脚本
# 服务器: 192.168.1.14
# 部署目录: /home/busizeshi/docker
# 警告: 此脚本会删除所有 Docker 容器、镜像和数据，不可恢复！
###############################################################################

set -e

echo "=========================================="
echo "  Docker 环境完全清理脚本"
echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查是否为 root 或有 sudo 权限
if [ "$EUID" -ne 0 ] && ! sudo -n true 2>/dev/null; then 
    echo -e "${RED}错误: 需要 root 权限或 sudo 权限${NC}"
    exit 1
fi

# 最终确认
echo -e "${RED}⚠️  警告: 此操作将会：${NC}"
echo "  1. 停止并删除所有 Docker 容器"
echo "  2. 删除所有 Docker 镜像"
echo "  3. 删除所有 Docker 卷"
echo "  4. 删除所有 Docker 网络"
echo "  5. 删除 /home/busizeshi/docker 目录及其所有内容"
echo ""
echo -e "${RED}所有数据将永久丢失，无法恢复！${NC}"
echo ""
read -p "确定要继续吗？输入 'YES' 继续: " confirmation

if [ "$confirmation" != "YES" ]; then
    echo -e "${YELLOW}操作已取消${NC}"
    exit 0
fi

echo ""
echo "=========================================="
echo "  开始清理..."
echo "=========================================="
echo ""

# 步骤 1: 停止所有正在运行的容器
echo -e "${YELLOW}[1/8] 停止所有正在运行的容器...${NC}"
if [ "$(docker ps -q)" ]; then
    docker stop $(docker ps -q)
    echo -e "${GREEN}✓ 所有容器已停止${NC}"
else
    echo -e "${GREEN}✓ 没有正在运行的容器${NC}"
fi
echo ""

# 步骤 2: 删除所有容器
echo -e "${YELLOW}[2/8] 删除所有容器...${NC}"
if [ "$(docker ps -aq)" ]; then
    docker rm -f $(docker ps -aq)
    echo -e "${GREEN}✓ 所有容器已删除${NC}"
else
    echo -e "${GREEN}✓ 没有容器需要删除${NC}"
fi
echo ""

# 步骤 3: 删除所有镜像
echo -e "${YELLOW}[3/8] 删除所有镜像...${NC}"
if [ "$(docker images -q)" ]; then
    docker rmi -f $(docker images -q)
    echo -e "${GREEN}✓ 所有镜像已删除${NC}"
else
    echo -e "${GREEN}✓ 没有镜像需要删除${NC}"
fi
echo ""

# 步骤 4: 删除所有卷
echo -e "${YELLOW}[4/8] 删除所有 Docker 卷...${NC}"
if [ "$(docker volume ls -q)" ]; then
    docker volume rm $(docker volume ls -q) 2>/dev/null || true
    echo -e "${GREEN}✓ 所有卷已删除${NC}"
else
    echo -e "${GREEN}✓ 没有卷需要删除${NC}"
fi
echo ""

# 步骤 5: 删除所有网络（保留默认网络）
echo -e "${YELLOW}[5/8] 删除自定义 Docker 网络...${NC}"
if [ "$(docker network ls -q -f type=custom)" ]; then
    docker network rm $(docker network ls -q -f type=custom) 2>/dev/null || true
    echo -e "${GREEN}✓ 所有自定义网络已删除${NC}"
else
    echo -e "${GREEN}✓ 没有自定义网络需要删除${NC}"
fi
echo ""

# 步骤 6: 清理 Docker 系统（删除未使用的数据）
echo -e "${YELLOW}[6/8] 清理 Docker 系统缓存...${NC}"
docker system prune -a -f --volumes
echo -e "${GREEN}✓ Docker 系统已清理${NC}"
echo ""

# 步骤 7: 删除部署目录
echo -e "${YELLOW}[7/8] 删除部署目录 /home/busizeshi/docker ...${NC}"
DEPLOY_DIR="/home/busizeshi/docker"

if [ -d "$DEPLOY_DIR" ]; then
    echo "  准备删除目录: $DEPLOY_DIR"
    echo "  目录大小: $(du -sh $DEPLOY_DIR 2>/dev/null | cut -f1)"
    echo ""
    
    # 列出主要子目录
    echo "  目录内容:"
    ls -la "$DEPLOY_DIR" 2>/dev/null | head -20
    echo ""
    
    # 删除目录
    rm -rf "$DEPLOY_DIR"
    echo -e "${GREEN}✓ 目录已删除${NC}"
else
    echo -e "${GREEN}✓ 目录不存在，无需删除${NC}"
fi
echo ""

# 步骤 8: 验证清理结果
echo -e "${YELLOW}[8/8] 验证清理结果...${NC}"
echo ""

echo "Docker 容器数量: $(docker ps -a | wc -l)"
echo "Docker 镜像数量: $(docker images | wc -l)"
echo "Docker 卷数量: $(docker volume ls | wc -l)"
echo "Docker 网络数量: $(docker network ls | wc -l)"
echo ""

if [ ! -d "$DEPLOY_DIR" ]; then
    echo -e "${GREEN}✓ 部署目录已清理${NC}"
else
    echo -e "${RED}✗ 部署目录仍然存在${NC}"
fi
echo ""

# 显示 Docker 磁盘使用情况
echo "Docker 磁盘使用情况:"
docker system df
echo ""

echo "=========================================="
echo -e "${GREEN}  清理完成！${NC}"
echo "=========================================="
echo ""
echo "清理摘要:"
echo "  - 所有容器已删除"
echo "  - 所有镜像已删除"
echo "  - 所有卷已删除"
echo "  - 所有自定义网络已删除"
echo "  - 部署目录已删除"
echo "  - Docker 系统缓存已清理"
echo ""
echo "下一步操作:"
echo "  1. 如需重新部署，请重新创建 /home/busizeshi/docker 目录"
echo "  2. 重新拉取所需的 Docker 镜像"
echo "  3. 重新创建 docker-compose.yml 配置文件"
echo "  4. 使用 docker-compose up -d 启动服务"
echo ""
echo "完成时间: $(date '+%Y-%m-%d %H:%M:%S')"
