#!/usr/bin/env bash
# MySQL 恢复脚本：默认恢复到 docker compose 中的 mysql 服务
# 用法：DB_PASSWORD=root密码 ./scripts/restore-db.sh ./backups/myshop-20260101-120000.sql.gz
set -euo pipefail

BACKUP_FILE="${1:?请指定备份文件，例如 ./backups/myshop-20260101-120000.sql.gz}"
DB_NAME="${DB_NAME:-myshop}"
SERVICE="${SERVICE:-mysql}"
DB_PASSWORD="${DB_PASSWORD:?请通过环境变量 DB_PASSWORD 指定 MySQL root 密码}"

if [[ ! -f "${BACKUP_FILE}" ]]; then
  echo "备份文件不存在：${BACKUP_FILE}" >&2
  exit 1
fi

if [[ "${FORCE:-false}" != "true" ]]; then
  read -r -p "恢复会覆盖数据库 [${DB_NAME}] 的现有数据，确认继续请输入 YES: " answer
  [[ "${answer}" == "YES" ]] || { echo "已取消恢复"; exit 1; }
fi

CONTAINER_ID="$(docker compose ps -q "${SERVICE}")"
[[ -n "${CONTAINER_ID}" ]] || { echo "未找到运行中的 compose 服务：${SERVICE}" >&2; exit 1; }

REMOTE_FILE="/tmp/myshop-restore.sql"
if [[ "${BACKUP_FILE}" == *.gz ]]; then
  gzip -dc "${BACKUP_FILE}" > "${REMOTE_FILE}.plain"
  SOURCE_FILE="${REMOTE_FILE}.plain"
else
  SOURCE_FILE="${BACKUP_FILE}"
fi

echo "上传备份文件到容器 ${CONTAINER_ID}"
docker cp "${SOURCE_FILE}" "${CONTAINER_ID}:${REMOTE_FILE}"
trap 'docker exec "${CONTAINER_ID}" rm -f "${REMOTE_FILE}" || true; rm -f "${REMOTE_FILE}.plain"' EXIT

echo "开始恢复数据库 ${DB_NAME}"
docker exec -i "${CONTAINER_ID}" sh -c \
  "mysql -u root -p'${DB_PASSWORD}' --default-character-set=utf8mb4 ${DB_NAME} < ${REMOTE_FILE}"
echo "恢复完成：${BACKUP_FILE}"
