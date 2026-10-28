#!/usr/bin/env bash
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd "$project_dir"
exec java -jar target/sunicoffee-1.0.0.jar --debug=false --spring.profiles.active=local --server.address=127.0.0.1 "$@"
