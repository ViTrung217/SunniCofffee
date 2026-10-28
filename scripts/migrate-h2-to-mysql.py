#!/usr/bin/env python3
"""Back up a stopped H2 database, then copy/verify all business rows into empty MySQL."""
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import uuid

root = Path(__file__).resolve().parent.parent
os.chdir(root)
p = argparse.ArgumentParser()
p.add_argument('--source', type=Path, default=root / '.runtime/sunicoffee.mv.db')
p.add_argument('--mysql-config', type=Path, default=root / '.runtime/mysql.properties')
p.add_argument('--report', type=Path, default=root / '.runtime/mysql-migration-verified.json')
args = p.parse_args()
source = args.source.resolve()
if not source.is_file() or not source.name.endswith('.mv.db'):
    p.error('Source must be an existing H2 .mv.db file')
if not args.mysql_config.is_file():
    p.error('Missing MySQL properties; run scripts/setup-mysql.py first')
if args.report.exists():
    p.error('Report exists; choose a new --report path')
if shutil.which('lsof'):
    opened = subprocess.run(['lsof', '-t', str(source)], capture_output=True, text=True)
    if opened.stdout.strip():
        raise SystemExit('Dừng ứng dụng đang mở file H2 trước khi sao lưu và chuyển dữ liệu.')
backup = root / '.backups' / ('mysql-migration-' + datetime.date.today().strftime('%Y%m%d') + '-' + uuid.uuid4().hex[:8])
backup.mkdir(parents=True, mode=0o700)
snapshot = backup / 'source.mv.db'
shutil.copy2(source, snapshot)
os.chmod(snapshot, 0o600)
checksum = hashlib.sha256(source.read_bytes()).hexdigest()
if hashlib.sha256(snapshot.read_bytes()).hexdigest() != checksum:
    raise SystemExit('H2 backup checksum mismatch; migration not started')
classpath_file = root / '.runtime/migration-classpath.txt'
if not classpath_file.exists() or not (root / 'target/classes').is_dir():
    subprocess.run(['mvn', '-B', '-DskipTests', 'package', 'dependency:build-classpath', '-Dmdep.outputFile=' + str(classpath_file)], check=True)
classpath = str(root / 'target/classes') + os.pathsep + classpath_file.read_text().strip()
subprocess.run(['java', '-cp', classpath, str(root / 'scripts/java/H2ToMysql.java'),
                str(snapshot)[:-6], str(args.mysql_config.resolve()), str(args.report.resolve())], check=True)
manifest = {'source_backup': str(snapshot.relative_to(root)), 'source_sha256': checksum,
            'report': str(args.report), 'source_original_unchanged': hashlib.sha256(source.read_bytes()).hexdigest() == checksum}
if not manifest['source_original_unchanged']:
    raise SystemExit('Original H2 changed during migration; do not switch the application')
(backup / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
print('Đã chuyển và đối chiếu dữ liệu. Bản sao H2: ' + str(snapshot.relative_to(root)))
