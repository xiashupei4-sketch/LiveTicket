#!/usr/bin/env python3
"""
LiveTicket 秒杀压测数据生成脚本

功能:
  1. 注册 500 个唯一压测用户 load001..load500 (密码 123456)
  2. 逐个登录获取 JWT
  3. 写入 load-test/load-users.csv (列: username,password,token)

用法:
  python generate_load_users.py [--base http://127.0.0.1:8080] [--count 500]

说明:
  - 已存在的用户跳过注册直接登录（幂等，可重复执行）
"""
import argparse
import csv
import json
import sys
import time
import urllib.request
import urllib.error


def http_json(base: str, method: str, path: str, body: dict | None = None, timeout: float = 10.0):
    url = base.rstrip("/") + path
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "ignore")
        try:
            return e.code, json.loads(raw)
        except json.JSONDecodeError:
            return e.code, {"raw": raw}


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default="http://127.0.0.1:8080")
    ap.add_argument("--count", type=int, default=500)
    ap.add_argument("--password", default="123456")
    ap.add_argument("--out", default="load-users.csv")
    args = ap.parse_args()

    rows = []
    ok = fail = skipped = 0
    t0 = time.time()

    for i in range(1, args.count + 1):
        username = f"load{i:03d}"
        # 注册（已存在则跳过）
        code, resp = http_json(args.base, "POST", "/api/auth/register",
                               {"username": username, "password": args.password,
                                "nickname": f"压测用户{i:03d}"})
        if code == 200 and resp.get("code") == 0:
            ok += 1
        elif resp.get("code") == 20002:
            skipped += 1
        else:
            print(f"[WARN] register {username}: http={code} resp={resp}")
            fail += 1
            continue

        # 登录
        code, resp = http_json(args.base, "POST", "/api/auth/login",
                               {"username": username, "password": args.password})
        token = (resp.get("data") or {}).get("token")
        if code == 200 and token:
            rows.append((username, args.password, token))
        else:
            print(f"[WARN] login {username}: http={code} resp={resp}")
            fail += 1

        if i % 50 == 0:
            print(f"progress {i}/{args.count} elapsed={time.time()-t0:.1f}s")

    with open(args.out, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["username", "password", "token"])
        w.writerows(rows)

    print(f"done: registered={ok} skipped={skipped} failed={fail} csv_rows={len(rows)} file={args.out}")
    return 1 if fail else 0


if __name__ == "__main__":
    sys.exit(main())
