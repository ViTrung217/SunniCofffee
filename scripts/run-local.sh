#!/usr/bin/env bash
# Kept as the familiar local entry point; the runtime database is now Docker MySQL.
set -euo pipefail
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
exec bash "$script_dir/run-mysql.sh" "$@"
