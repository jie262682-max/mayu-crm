#!/bin/sh
# 码域CRM Docker 一键部署（含按正确顺序初始化数据库）
# 用法：./docker/deploy-docker.sh
# AGPL-3.0 © 2026 合肥市码域科技
set -e
cd "$(dirname "$0")/.."

SQL_ORDER="freecrm-schema freecrm-crm freecrm-m3 freecrm-oa freecrm-eam \
           freecrm-m11 freecrm-m15-menu freecrm-eam-menu freecrm-m16 \
           freecrm-w2 freecrm-w3 freecrm-w3-menu freecrm-w6 freecrm-demo-data"

echo "==> 1/4 启动 MySQL/Redis ..."
docker compose up -d mysql redis

echo "==> 2/4 等待 MySQL 就绪 ..."
until docker exec maoyu-mysql mysql -uroot -pmaoyu@2026 -e "SELECT 1" >/dev/null 2>&1; do
  sleep 3; echo "   ... 等待中"
done

echo "==> 3/4 按顺序初始化数据库（幂等，可重复执行） ..."
for f in $SQL_ORDER; do
  echo "   导入 $f.sql"
  docker exec -i maoyu-mysql mysql -uroot -pmaoyu@2026 --default-character-set=utf8mb4 freecrm < sql/$f.sql 2>/dev/null
done

echo "==> 4/4 构建并启动后端与前端（首次构建约 5-10 分钟） ..."
docker compose up -d --build

cat <<'EOF'

✅ 部署完成
   PC 端   : http://服务器IP:8090
   手机 H5 : http://服务器IP:8090/m/
   默认账号 : admin / admin123（请立即修改）

常用命令：
   docker compose logs -f server     # 后端日志
   docker compose restart server     # 重启后端
   docker compose down               # 停止（数据保留在卷）
   docker compose down -v            # 停止并清空数据
EOF
