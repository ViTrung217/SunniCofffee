#!/usr/bin/env python3
"""Configure Google login locally without printing credentials or putting them in shell history."""
import getpass
import os
from pathlib import Path
import re
import tempfile


def main():
    client_id = input('Google Client ID: ').strip()
    secret = getpass.getpass('Google Client Secret (ẩn khi nhập): ').strip()
    if not re.fullmatch(r'[A-Za-z0-9_-]+\.apps\.googleusercontent\.com', client_id):
        raise ValueError('Client ID cần có đuôi .apps.googleusercontent.com.')
    if not re.fullmatch(r'[A-Za-z0-9_-]{10,200}', secret):
        raise ValueError('Client Secret không đúng định dạng; hãy sao chép lại từ Google Cloud.')
    root = Path(__file__).resolve().parent.parent / '.runtime'
    root.mkdir(mode=0o700, exist_ok=True)
    fd, temporary = tempfile.mkstemp(prefix='.google-', dir=root)
    try:
        with os.fdopen(fd, 'w', encoding='utf-8') as out:
            out.write('app.google.enabled=true\napp.google.client-id=' + client_id + '\napp.google.client-secret=' + secret + '\n')
        os.replace(temporary, root / 'google.properties')
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)
    print('Đã lưu .runtime/google.properties (quyền 600). Khởi động lại ứng dụng rồi mở http://localhost:8080/login.')


if __name__ == '__main__':
    try:
        main()
    except (ValueError, OSError, EOFError, KeyboardInterrupt) as error:
        print(str(error) if isinstance(error, ValueError) else 'Chưa lưu cấu hình. Hãy kiểm tra quyền file và thử lại.')
        raise SystemExit(1)
