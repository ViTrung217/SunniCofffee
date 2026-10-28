#!/usr/bin/env bash
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd "$project_dir"
backup=${1:?Truyền đường dẫn bản sao lưu SQL}
database=${2:-sunicoffee_$(date -u +%Y%m%dT%H%M%SZ)_restore_check}
[[ -f "$backup" && "$database" =~ ^sunicoffee_[a-zA-Z0-9_]+_restore_check$ ]] || { echo 'Chỉ khôi phục vào database mới có hậu tố _restore_check trong container kiểm thử.' >&2; exit 1; }
docker compose --profile test up -d --wait --wait-timeout 180 mysql-test
docker compose exec -T mysql-test mysql --defaults-extra-file=/run/secrets/mysql_root_client \
  -e "CREATE DATABASE \`$database\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_cs; GRANT ALL PRIVILEGES ON \`$database\`.* TO 'sunicoffee'@'%';"
docker compose exec -T mysql-test mysql --defaults-extra-file=/run/secrets/mysql_root_client "$database" < "$backup"
docker compose exec -T mysql-test mysql --defaults-extra-file=/run/secrets/mysql_root_client "$database" \
  -e 'SELECT COUNT(*) AS products FROM products; SELECT COUNT(*) AS orders FROM customer_orders; SELECT COUNT(*) AS stock_movements FROM stock_movements;'
echo "Đã khôi phục kiểm tra tại $database trên cổng 13306; database chính không bị thay đổi."
