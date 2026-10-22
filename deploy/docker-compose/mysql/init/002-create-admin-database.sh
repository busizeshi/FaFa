#!/bin/bash
(
set -euo pipefail

# 官方 MySQL 入口仅在空数据目录执行本脚本，已有卷不能依赖它补建权限。
: "${MYSQL_ADMIN_PASSWORD:?请设置独立管理库密码}"
admin_password=$(printf '%s' "$MYSQL_ADMIN_PASSWORD" | sed "s/'/''/g")
MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --protocol=socket --user=root <<SQL
SET SESSION sql_mode='NO_BACKSLASH_ESCAPES';
CREATE DATABASE IF NOT EXISTS fafa_admin CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
CREATE USER IF NOT EXISTS 'fafa_admin'@'%' IDENTIFIED BY '${admin_password}';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON fafa_admin.* TO 'fafa_admin'@'%';
SQL
)
