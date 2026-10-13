#!/usr/bin/env bash
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd "$project_dir"
jar=target/sunicoffee-1.0.0.jar
if [[ ! -f "$jar" ]]; then
  echo 'Chưa có bản đóng gói. Chạy mvn -B verify trong thư mục dự án trước.' >&2
  exit 1
fi
exec java -jar "$jar" --debug=false --spring.profiles.active=local --server.address=127.0.0.1 "$@"
