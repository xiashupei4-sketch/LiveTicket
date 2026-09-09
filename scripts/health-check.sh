#!/usr/bin/env bash
# LiveTicket 健康检查
# 用法: ./scripts/health-check.sh
set -uo pipefail

cd "$(dirname "$0")/.."

fail=0

check() {
  local name="$1" url="$2"
  if curl -sf -o /dev/null -m 5 "$url"; then
    echo "OK      $name  ($url)"
  else
    echo "FAIL    $name  ($url)"
    fail=1
  fi
}

check_tcp() {
  local name="$1" host="$2" port="$3"
  if (echo > "/dev/tcp/$host/$port") >/dev/null 2>&1; then
    echo "OK      $name  (tcp $host:$port)"
  else
    echo "FAIL    $name  (tcp $host:$port)"
    fail=1
  fi
}

echo "== LiveTicket 健康检查 =="

# 容器方式（docker compose ps），非容器环境自动跳过
if command -v docker >/dev/null 2>&1 && docker compose ps >/dev/null 2>&1; then
  echo "-- 容器状态 --"
  docker compose ps --format "table {{.Name}}\t{{.Service}}\t{{.Status}}"
fi

echo "-- HTTP 服务 --"
check "前端 (nginx)"    "http://localhost/"
check "后端 API"        "http://localhost:8080/api/events/hot?limit=1"
check "Knife4j 文档"    "http://localhost:8080/doc.html"

echo "-- 中间件 --"
check_tcp "MySQL"     127.0.0.1 3306
check_tcp "Redis"     127.0.0.1 6379
check_tcp "RabbitMQ"  127.0.0.1 5672

if [ "$fail" -eq 0 ]; then
  echo "== 全部通过 =="
  exit 0
else
  echo "== 存在失败项 =="
  exit 1
fi
