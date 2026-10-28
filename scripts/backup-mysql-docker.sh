#!/usr/bin/env bash
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd "$project_dir"
umask 077
output=${1:-.backups/mysql-$(date -u +%Y%m%dT%H%M%SZ).sql}
[[ ! -e "$output" ]] || { echo 'Không ghi đè bản sao lưu đã tồn tại.' >&2; exit 1; }
mkdir -p -- "$(dirname -- "$output")"
temporary=$(mktemp "$(dirname -- "$output")/.mysql-backup.XXXXXX")
trap 'rm -f -- "$temporary"' EXIT
docker compose exec -T mysql mysqldump --defaults-extra-file=/run/secrets/mysql_root_client \
  --single-transaction --hex-blob --routines --triggers --no-tablespaces --set-gtid-purged=OFF \
  --column-statistics=0 sunicoffee > "$temporary"
ln -- "$temporary" "$output"
echo "Đã sao lưu MySQL: $output"
