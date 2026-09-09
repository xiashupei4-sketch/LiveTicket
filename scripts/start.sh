#!/usr/bin/env bash
# LiveTicket 一键启动（Docker Compose）
# 用法: ./scripts/start.sh
set -euo pipefail

cd "$(dirname "$0")/.."

echo "[LiveTicket] docker compose up -d --build ..."
docker compose up -d --build

echo "[LiveTicket] 等待服务健康 ..."
docker compose up -d --wait

echo "[LiveTicket] 全部服务已启动:"
docker compose ps

echo ""
echo "  前端:              http://localhost"
echo "  后端 API:          http://localhost:8080/api"
echo "  Knife4j 文档:      http://localhost:8080/doc.html"
echo "  RabbitMQ 管理:     http://localhost:15672  (liveticket / liveticket123)"
echo "  演示账号:          demo01 / 123456"
