#!/usr/bin/env python3
"""Create local Docker MySQL credentials once, without printing or replacing them."""
import os
from pathlib import Path
import secrets
import subprocess

root = Path(__file__).resolve().parent.parent
runtime = root / '.runtime'
secret_dir = runtime / 'docker'
secret_dir.mkdir(parents=True, exist_ok=True)
os.chmod(secret_dir, 0o700)
files = [secret_dir / 'mysql-password', secret_dir / 'mysql-root-password',
         secret_dir / 'mysql-root-client.cnf', runtime / 'mysql.properties']
existing = [p.exists() for p in files]
if any(existing) and not all(existing):
    raise SystemExit('Cấu hình MySQL chưa đầy đủ. Khôi phục các file bí mật đang thiếu; không tự tạo lại mật khẩu của volume đã có.')
if not any(existing):
    try:
        ready = subprocess.run(['docker', 'info'], capture_output=True)
    except FileNotFoundError:
        raise SystemExit('Cài và mở Docker Desktop trước khi tạo cấu hình MySQL.')
    if ready.returncode:
        raise SystemExit('Mở Docker Desktop trước khi tạo cấu hình MySQL.')
    volume = os.environ.get('COMPOSE_PROJECT_NAME', 'sunicoffee') + '_mysql_data'
    if subprocess.run(['docker', 'volume', 'inspect', volume], capture_output=True).returncode == 0:
        raise SystemExit('Volume MySQL đã tồn tại nhưng thiếu file bí mật. Khôi phục cấu hình cũ; không tự tạo lại mật khẩu.')
    password, root_password = secrets.token_hex(32), secrets.token_hex(32)
    contents = [password, root_password, '[client]\nuser=root\npassword=' + root_password + '\nhost=localhost\n',
        'spring.datasource.url=jdbc:mysql://127.0.0.1:3307/sunicoffee?connectionTimeZone=Asia/Ho_Chi_Minh&forceConnectionTimeZoneToSession=true&characterEncoding=UTF-8&sslMode=DISABLED&allowPublicKeyRetrieval=true\n'
        'spring.datasource.username=sunicoffee\nspring.datasource.password=' + password + '\n']
    for path, content in zip(files, contents):
        fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(fd, 'w') as stream:
            stream.write(content)
else:
    for path in files:
        os.chmod(path, 0o600)
for name in ('google', 'chatbot', 'sepay', 'mail'):
    path = runtime / (name + '.properties')
    if not path.exists():
        fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        os.close(fd)
env_file = root / '.env'
current = env_file.read_text() if env_file.exists() else ''
entries = []
for name, value in (('SUNI_UID', os.getuid()), ('SUNI_GID', os.getgid())):
    if not any(line.startswith(name + '=') for line in current.splitlines()):
        entries.append(name + '=' + str(value))
if entries:
    fd = os.open(env_file, os.O_WRONLY | os.O_CREAT | os.O_APPEND, 0o600)
    with os.fdopen(fd, 'w') as stream:
        stream.write(('\n' if current and not current.endswith('\n') else '') + '\n'.join(entries) + '\n')
print('Đã sẵn sàng cấu hình MySQL trong .runtime/; không in mật khẩu và không đổi bí mật hiện có.')
