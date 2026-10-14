#!/usr/bin/env python3
"""Configure SePay locally; only reads bank accounts, never initiates a transfer."""
import getpass
import json
import os
from pathlib import Path
import re
import tempfile
import socket
import ssl
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[1]


def property_value(value):
    escaped = str(value).replace('\\', '\\\\').replace('\n', '\\n').replace('\r', '\\r').encode('ascii', 'backslashreplace').decode('ascii')
    return re.sub(r'\\x([0-9a-fA-F]{2})', r'\\u00\1', escaped)


def main():
    mode = input('Môi trường sandbox hoặc live [sandbox]: ').strip().lower() or 'sandbox'
    if mode not in ('sandbox', 'live'):
        raise ValueError('Chỉ chấp nhận sandbox hoặc live.')
    token = getpass.getpass('API token SePay (ẩn khi nhập): ').strip()
    if not re.fullmatch(r'[A-Za-z0-9]{64}', token):
        raise ValueError('Token API v2 cần 64 ký tự chữ/số.')
    host = 'userapi-sandbox.sepay.vn' if mode == 'sandbox' else 'userapi.sepay.vn'
    accounts = []
    for page in range(1, 11):
        req = urllib.request.Request(
            f'https://{host}/v2/bank-accounts?active=1&per_page=100&page={page}',
            headers={'Authorization': 'Bearer ' + token, 'Accept': 'application/json', 'User-Agent': 'SuniCoffee-Setup/1.0'})
        try:
            with urllib.request.urlopen(req, timeout=15) as response:
                payload = json.load(response)
        except urllib.error.HTTPError as error:
            messages = {
                401: 'HTTP 401: Token không hợp lệ, bị tắt hoặc không đúng môi trường live/sandbox.',
                403: 'HTTP 403: SePay từ chối truy cập. Kiểm tra token, quyền truy cập và mạng/VPN.',
                429: 'HTTP 429: Quá nhiều yêu cầu. Chờ một phút rồi chạy lại.',
            }
            message = messages.get(error.code, f'HTTP {error.code}: SePay chưa đáp ứng yêu cầu. Hãy thử lại sau.')
            error.close()
            raise ValueError(message) from None
        except urllib.error.URLError as error:
            if isinstance(error.reason, ssl.SSLCertVerificationError):
                raise ValueError('Lỗi chứng chỉ HTTPS của Python. Cần cập nhật chứng chỉ tin cậy; không tắt xác minh SSL.') from None
            if isinstance(error.reason, (TimeoutError, socket.timeout)):
                raise ValueError('Kết nối SePay quá thời gian chờ. Kiểm tra mạng rồi thử lại.') from None
            raise ValueError('Không kết nối được SePay qua HTTPS. Kiểm tra mạng, DNS hoặc VPN.') from None
        except (TimeoutError, socket.timeout):
            raise ValueError('Kết nối SePay quá thời gian chờ. Kiểm tra mạng rồi thử lại.') from None
        except json.JSONDecodeError:
            raise ValueError('SePay trả phản hồi không phải JSON. Hãy thử lại sau.') from None
        if payload.get('status') != 'success' or not isinstance(payload.get('data'), list):
            raise ValueError('Phản hồi danh sách tài khoản không hợp lệ.')
        accounts.extend(payload['data'])
        more = payload.get('meta', {}).get('pagination', {}).get('has_more')
        if more is False:
            break
        if more is not True or page == 10:
            raise ValueError('Không đọc được đầy đủ danh sách tài khoản.')
    accounts = [a for a in accounts if str(a.get('bank_bin')) == '970422'
                or str(a.get('bank_short_name', '')).upper() in ('MB', 'MBBANK')]
    if not accounts:
        raise ValueError('Không tìm thấy tài khoản MBBank trong môi trường đã chọn.')
    for i, account in enumerate(accounts, 1):
        print(f"{i}. MBBank ••••{str(account['account_number'])[-4:]} — {account['account_holder_name']}")
    index = int(input('Chọn tài khoản [1]: ').strip() or '1') - 1
    if index < 0 or index >= len(accounts):
        raise ValueError('Lựa chọn tài khoản không hợp lệ.')
    account = accounts[index]
    if not re.fullmatch(r'[0-9a-fA-F-]{36}', str(account['id'])):
        raise ValueError('UUID tài khoản không hợp lệ.')
    number = str(account['account_number'])
    if not re.fullmatch(r'[A-Za-z0-9]{1,64}', number):
        raise ValueError('Số tài khoản không hợp lệ.')
    print('Dùng tài khoản MBBank gốc, không dùng VA. Nếu bạn đã cấu hình VA, hãy dừng và điền mẫu cấu hình riêng.')
    if input('Nhập lưu hoặc luu để lưu cấu hình tài khoản này: ').strip().lower() not in ('luu', 'lưu'):
        print('Chưa lưu cấu hình.'); return
    values = {'enabled': 'true', 'sandbox': str(mode == 'sandbox').lower(), 'api-token': token,
              'bank-account-id': account['id'], 'account-number': number, 'qr-account': number,
              'bank': '970422', 'holder': account['account_holder_name'], 'description-prefix': '', 'va': ''}
    target = ROOT / '.runtime' / 'sepay.properties'
    target.parent.mkdir(exist_ok=True)
    if target.exists():
        raise ValueError('Đã có .runtime/sepay.properties; hãy chỉnh file hiện có để tránh ghi đè cấu hình.')
    fd, temporary = tempfile.mkstemp(prefix='.sepay-', dir=target.parent)
    try:
        with os.fdopen(fd, 'w', encoding='ascii') as output:
            output.write('# Private local configuration; do not commit.\n')
            for key, value in values.items():
                output.write(f'app.sepay.{key}={property_value(value)}\n')
        os.link(temporary, target)  # Fail rather than overwrite a concurrently created file.
    finally:
        os.unlink(temporary)
    print(f'Đã lưu cấu hình {mode}, quyền 600. Khởi động lại ứng dụng để nạp cấu hình.')


if __name__ == '__main__':
    try:
        main()
    except (KeyboardInterrupt, EOFError):
        print('\nĐã dừng; chưa hoàn tất cấu hình.')
    except ValueError as error:
        print('Không cấu hình được:', error)
        raise SystemExit(1)
    except Exception:
        print('Không cấu hình được. Kiểm tra mạng, quyền file và token đúng môi trường; không gửi token vào chat.')
        raise SystemExit(1)
