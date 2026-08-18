#!/bin/bash

# FaFa Docker 服务管理脚本
# 使用方法: ./manage.sh [start|stop|restart|status|logs|backup]

DEPLOY_DIR="/home/busizeshi/docker"
BACKUP_DIR="/home/busizeshi/backups"
DATE=$(date +%Y%m%d_%H%M%S)

BOLD="\033[1m"
GREEN="\033[0;32m"
YELLOW="\033[1;33m"
RED="\033[0;31m"
NC="\033[0m"

cd $DEPLOY_DIR

case "$1" in
    start)
        echo -e "${GREEN}启动所有服务...${NC}"
        docker compose up -d
        echo -e "${GREEN}服务已启动${NC}"
        docker compose ps
        ;;
        
    stop)
        echo -e "${YELLOW}停止所有服务...${NC}"
        docker compose stop
        echo -e "${GREEN}服务已停止${NC}"
        ;;
        
    restart)
        echo -e "${YELLOW}重启所有服务...${NC}"
        docker compose restart
        echo -e "${GREEN}服务已重启${NC}"
        docker compose ps
        ;;
        
    status)
        echo -e "${BOLD}服务状态:${NC}"
        docker compose ps
        echo ""
        echo -e "${BOLD}资源使用情况:${NC}"
        docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}"
        ;;
        
    logs)
        if [ -z "$2" ]; then
            echo -e "${YELLOW}查看所有服务日志...${NC}"
            docker compose logs --tail=100 -f
        else
            echo -e "${YELLOW}查看 $2 服务日志...${NC}"
            docker compose logs --tail=100 -f $2
        fi
        ;;
        
    backup)
        echo -e "${YELLOW}开始备份数据...${NC}"
        mkdir -p $BACKUP_DIR
        
        # 备份 MySQL
        echo "备份 MySQL..."
        docker exec fafa-mysql mysqldump -uroot -pfafa_root_123456 --all-databases > $BACKUP_DIR/mysql_$DATE.sql
        gzip $BACKUP_DIR/mysql_$DATE.sql
        
        # 备份 Redis
        echo "备份 Redis..."
        docker exec fafa-redis redis-cli BGSAVE
        sleep 2
        cp $DEPLOY_DIR/redis/data/dump.rdb $BACKUP_DIR/redis_$DATE.rdb
        
        # 备份 MinIO
        echo "备份 MinIO..."
        tar -czf $BACKUP_DIR/minio_$DATE.tar.gz -C $DEPLOY_DIR/minio/data .
        
        # 备份 Qdrant
        echo "备份 Qdrant..."
        tar -czf $BACKUP_DIR/qdrant_$DATE.tar.gz -C $DEPLOY_DIR/qdrant/data .
        
        echo -e "${GREEN}备份完成，文件保存在: $BACKUP_DIR${NC}"
        ls -lh $BACKUP_DIR/*$DATE*
        ;;
        
    clean)
        echo -e "${YELLOW}清理日志文件...${NC}"
        find $DEPLOY_DIR/mysql/logs -name "*.log" -mtime +7 -delete
        find $DEPLOY_DIR/rocketmq -name "*.log" -mtime +7 -delete
        echo -e "${GREEN}日志清理完成${NC}"
        ;;
        
    update)
        echo -e "${YELLOW}更新镜像并重启服务...${NC}"
        docker compose pull
        docker compose up -d
        echo -e "${GREEN}更新完成${NC}"
        ;;
        
    *)
        echo "FaFa Docker 服务管理脚本"
        echo ""
        echo "使用方法: $0 [command] [options]"
        echo ""
        echo "命令:"
        echo "  start          启动所有服务"
        echo "  stop           停止所有服务"
        echo "  restart        重启所有服务"
        echo "  status         查看服务状态"
        echo "  logs [service] 查看日志 (可指定服务名)"
        echo "  backup         备份所有数据"
        echo "  clean          清理过期日志"
        echo "  update         更新镜像并重启"
        echo ""
        echo "示例:"
        echo "  $0 start"
        echo "  $0 logs mysql"
        echo "  $0 backup"
        exit 1
        ;;
esac
