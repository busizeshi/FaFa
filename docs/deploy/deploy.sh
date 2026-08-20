#!/bin/bash

# FaFa Docker 服务部署脚本
# 使用方法: ./deploy.sh

set -e

DEPLOY_DIR="/home/busizeshi/docker"
BOLD="\033[1m"
GREEN="\033[0;32m"
YELLOW="\033[1;33m"
RED="\033[0;31m"
NC="\033[0m" # No Color

echo -e "${BOLD}=== FaFa Docker 服务部署脚本 ===${NC}\n"

# 检查 Docker 环境
check_docker() {
    echo -e "${YELLOW}检查 Docker 环境...${NC}"
    
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}错误: Docker 未安装${NC}"
        exit 1
    fi
    
    if ! command -v docker compose &> /dev/null && ! command -v docker-compose &> /dev/null; then
        echo -e "${RED}错误: Docker Compose 未安装${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ Docker 环境检查通过${NC}\n"
}

# 创建目录结构
create_directories() {
    echo -e "${YELLOW}创建目录结构...${NC}"
    
    mkdir -p $DEPLOY_DIR
    cd $DEPLOY_DIR
    
    mkdir -p mysql/{data,config,logs}
    mkdir -p redis/{data,config}
    mkdir -p minio/{data,config}
    mkdir -p qdrant/{data,config}
    mkdir -p rocketmq/{namesrv/{logs,store},broker/{logs,store}}
    
    echo -e "${GREEN}✓ 目录创建完成${NC}\n"
}

# 设置目录权限
set_permissions() {
    echo -e "${YELLOW}设置目录权限...${NC}"
    
    chmod -R 755 $DEPLOY_DIR
    
    echo -e "${GREEN}✓ 权限设置完成${NC}\n"
}

# 检查配置文件
check_config_files() {
    echo -e "${YELLOW}检查配置文件...${NC}"
    
    if [ ! -f "$DEPLOY_DIR/docker-compose.yml" ]; then
        echo -e "${RED}错误: docker-compose.yml 不存在${NC}"
        echo -e "请将 docker-compose.yml 上传到 $DEPLOY_DIR"
        exit 1
    fi
    
    if [ ! -f "$DEPLOY_DIR/redis/config/redis.conf" ]; then
        echo -e "${RED}错误: redis.conf 不存在${NC}"
        echo -e "请将 redis.conf 上传到 $DEPLOY_DIR/redis/config/"
        exit 1
    fi
    
    echo -e "${GREEN}✓ 配置文件检查通过${NC}\n"
}

# 拉取 Docker 镜像
pull_images() {
    echo -e "${YELLOW}拉取 Docker 镜像（可能需要几分钟）...${NC}"
    
    cd $DEPLOY_DIR
    docker compose pull
    
    echo -e "${GREEN}✓ 镜像拉取完成${NC}\n"
}

# 启动服务
start_services() {
    echo -e "${YELLOW}启动所有服务...${NC}"
    
    cd $DEPLOY_DIR
    docker compose up -d
    
    echo -e "${GREEN}✓ 服务启动完成${NC}\n"
}

# 等待服务就绪
wait_for_services() {
    echo -e "${YELLOW}等待服务就绪（约30秒）...${NC}"
    
    sleep 30
    
    echo -e "${GREEN}✓ 服务初始化完成${NC}\n"
}

# 检查服务状态
check_services() {
    echo -e "${YELLOW}检查服务状态...${NC}\n"
    
    cd $DEPLOY_DIR
    docker compose ps
    
    echo ""
}

# 显示访问信息
show_info() {
    echo -e "\n${BOLD}${GREEN}=== 部署完成 ===${NC}\n"
    echo -e "${BOLD}服务访问信息:${NC}"
    echo -e "  MySQL:              localhost:3306"
    echo -e "  Redis:              localhost:6379"
    echo -e "  MinIO Console:      http://localhost:9001"
    echo -e "  MinIO API:          http://localhost:9000"
    echo -e "  Qdrant Dashboard:   http://localhost:6333/dashboard"
    echo -e "  RocketMQ Dashboard: http://localhost:8080"
    echo -e ""
    echo -e "${BOLD}默认账号密码请查看 README.md${NC}\n"
    echo -e "${YELLOW}提示: 使用 'docker compose logs -f' 查看实时日志${NC}"
    echo -e "${YELLOW}提示: 使用 'docker compose stop' 停止所有服务${NC}\n"
}

# 主流程
main() {
    check_docker
    create_directories
    set_permissions
    check_config_files
    pull_images
    start_services
    wait_for_services
    check_services
    show_info
}

# 执行主流程
main
