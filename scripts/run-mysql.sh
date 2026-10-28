#!/usr/bin/env bash
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd "$project_dir"
[[ -f target/sunicoffee-1.0.0.jar ]] || { echo 'Chạy mvn -B verify trước khi khởi động.' >&2; exit 1; }
[[ -f .runtime/mysql.properties ]] || { echo 'Chạy python3 scripts/setup-mysql.py và chuyển dữ liệu theo docs/23-mysql-docker.md trước.' >&2; exit 1; }
docker info >/dev/null 2>&1 || { echo 'Mở Docker Desktop trước khi chạy SuniCoffee.' >&2; exit 1; }
docker compose up -d --wait --wait-timeout 180 mysql
exec java -jar target/sunicoffee-1.0.0.jar --debug=false --spring.profiles.active=mysql --server.address=127.0.0.1 "$@"
