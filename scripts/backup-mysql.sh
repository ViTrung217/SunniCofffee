#!/usr/bin/env bash
set -euo pipefail
# Usage: backup-mysql.sh /secure/client.cnf sunicoffee /backups/sunicoffee.sql
config=${1:?Pass an absolute MySQL client option file}
database=${2:?Pass database name}
output=${3:?Pass a new backup file path}
[[ "$config" = /* && "$database" =~ ^[a-zA-Z0-9_]+$ ]] || { echo 'Invalid config path or database name' >&2; exit 1; }
[[ ! -e "$output" ]] || { echo 'Refusing to overwrite an existing backup' >&2; exit 1; }
umask 077
set -o noclobber
mysqldump --defaults-extra-file="$config" --single-transaction --routines --triggers --no-tablespaces --set-gtid-purged=OFF "$database" > "$output"
echo "Backup created: $output"
