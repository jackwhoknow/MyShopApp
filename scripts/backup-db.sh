#!/usr/bin/env bash
# MySQL 每日备份脚本：默认备份 docker compose 中的 mysql 服务，也可指定本地 mysqldump
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-./backups}"
KEEP_DAYS="${KEEP_DAYS:-7}"
DB_NAME="${DB_NAME:-myshop}"
DB_USERNAME="${DB_USERNAME:-root}"
DB_PASSWORD="${DB_PASSWORD:?请通过环境变量 DB_PASSWORD 指定数据库密码}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
TARGET="${BACKUP_DIR}/${DB_NAME}-${TIMESTAMP}.sql.gz"

mkdir -p "${BACKUP_DIR}"

if [[ "${USE_LOCAL:-false}" == "true" ]]; then
  # 直接使用本机 mysqldump
  MYSQL_PWD="${DB_PASSWORD}" mysqldump \
    -h "${DB_HOST:-127.0.0.1}" -P "${DB_PORT:-3306}" -u "${DB_USERNAME}" \
    --single-transaction --routines --triggers --set-gtid-purged=OFF "${DB_NAME}" \
    | gzip > "${TARGET}"
else
  # 通过 docker compose 在 MySQL 容器内执行，避免本机安装客户端
  docker compose exec -T mysql mysqldump \
    -u "${DB_USERNAME}" -p"${DB_PASSWORD}" \
    --single-transaction --routines --triggers --set-gtid-purged=OFF "${DB_NAME}" \
    | gzip > "${TARGET}"
fi

echo "备份完成：${TARGET}"

# 清理超过保留天数的旧备份
find "${BACKUP_DIR}" -name "${DB_NAME}-*.sql.gz" -type f -mtime "+${KEEP_DAYS}" -delete
echo "已清理 ${KEEP_DAYS} 天前的备份文件"
