#!/usr/bin/env bash
set -euo pipefail
# Only restores to a NEW database with a _restore_check suffix.
config=${1:?Pass an absolute MySQL client option file}
database=${2:?Pass a NEW database name ending in _restore_check}
backup=${3:?Pass backup SQL path}
[[ "$config" = /* && "$database" =~ ^[a-zA-Z0-9_]+_restore_check$ && -f "$backup" ]] || { echo 'Invalid config, target or backup' >&2; exit 1; }
mysql --defaults-extra-file="$config" -e "CREATE DATABASE \`$database\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
mysql --defaults-extra-file="$config" "$database" < "$backup"
mysql --defaults-extra-file="$config" "$database" -e 'SELECT COUNT(*) AS orders_count FROM customer_orders; SELECT COUNT(*) AS movements_count FROM stock_movements; SELECT COUNT(*) AS images_count FROM product_images; SELECT COUNT(*) AS events_count FROM order_events;'
echo 'Restore completed into the isolated check database. Compare counts and exercise the application before considering a production recovery.'
