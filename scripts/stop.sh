#!/usr/bin/env bash
# LiveTicket 停止
# 用法: ./scripts/stop.sh           # 停止容器（保留数据卷）
#       ./scripts/stop.sh --down    # 停止并删除容器与网络（保留数据卷）
#       ./scripts/stop.sh --purge   # 停止并删除容器、网络与数据卷（危险：清空数据）
set -euo pipefail

cd "$(dirname "$0")/.."

case "${1:-}" in
  --purge)
    echo "[LiveTicket] docker compose down -v ..."
    docker compose down -v
    ;;
  --down)
    echo "[LiveTicket] docker compose down ..."
    docker compose down
    ;;
  "")
    echo "[LiveTicket] docker compose stop ..."
    docker compose stop
    ;;
  *)
    echo "用法: $0 [--down|--purge]"
    exit 1
    ;;
esac

echo "[LiveTicket] 已停止。"
