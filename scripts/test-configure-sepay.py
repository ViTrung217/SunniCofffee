#!/usr/bin/env python3
"""Offline checks; no SePay request or real credential."""
import contextlib
import importlib.util
import io
import json
from pathlib import Path
import stat
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('configure_sepay', Path(__file__).with_name('configure-sepay.py'))
config = importlib.util.module_from_spec(spec)
spec.loader.exec_module(config)
TOKEN = 'a' * 64
BANK = {'id': '12345678-1234-1234-1234-123456789012', 'bank_bin': '970422',
        'account_number': '000123456789', 'account_holder_name': 'TEST ACCOUNT'}
PAYLOAD = {'status': 'success', 'data': [BANK], 'meta': {'pagination': {'has_more': False}}}


class ConfigTest(unittest.TestCase):
    def test_private_configuration_and_read_only_request(self):
        with tempfile.TemporaryDirectory() as root:
            stream = io.StringIO()
            with patch.object(config, 'ROOT', Path(root)), patch('builtins.input', side_effect=['live', '1', 'lưu']), \
                 patch.object(config.getpass, 'getpass', return_value=TOKEN), \
                 patch.object(config.urllib.request, 'urlopen', return_value=io.BytesIO(json.dumps(PAYLOAD).encode())) as http, \
                 contextlib.redirect_stdout(stream):
                config.main()
            file = Path(root) / '.runtime/sepay.properties'
            self.assertEqual(stat.S_IMODE(file.stat().st_mode), 0o600)
            text = file.read_text()
            self.assertIn('app.sepay.account-number=000123456789', text)
            self.assertIn('app.sepay.sandbox=false', text)
            self.assertNotIn(TOKEN, stream.getvalue())
            req = http.call_args.args[0]
            self.assertEqual(req.get_method(), 'GET')
            self.assertEqual(req.get_header('User-agent'), 'SuniCoffee-Setup/1.0')
            self.assertTrue(req.full_url.startswith('https://userapi.sepay.vn/v2/bank-accounts?'))
            self.assertNotIn(TOKEN, req.full_url)

    def test_does_not_overwrite_existing_configuration(self):
        with tempfile.TemporaryDirectory() as root:
            target = Path(root) / '.runtime/sepay.properties'
            target.parent.mkdir()
            target.write_text('existing')
            with patch.object(config, 'ROOT', Path(root)), patch('builtins.input', side_effect=['sandbox', '1', 'luu']), \
                 patch.object(config.getpass, 'getpass', return_value=TOKEN), \
                 patch.object(config.urllib.request, 'urlopen', return_value=io.BytesIO(json.dumps(PAYLOAD).encode())), \
                 contextlib.redirect_stdout(io.StringIO()):
                with self.assertRaises(ValueError):
                    config.main()
            self.assertEqual(target.read_text(), 'existing')

    def test_provider_errors_are_actionable_without_secret_or_response_body(self):
        failures = [
            (config.urllib.error.HTTPError('https://example.invalid', 401, TOKEN, {}, io.BytesIO(TOKEN.encode())), 'HTTP 401'),
            (config.urllib.error.HTTPError('https://example.invalid', 403, TOKEN, {}, io.BytesIO(TOKEN.encode())), 'HTTP 403'),
            (config.urllib.error.HTTPError('https://example.invalid', 429, TOKEN, {}, io.BytesIO(TOKEN.encode())), 'HTTP 429'),
            (config.urllib.error.URLError(config.ssl.SSLCertVerificationError(TOKEN)), 'chứng chỉ'),
            (config.urllib.error.URLError(TOKEN), 'DNS'),
            (TimeoutError(TOKEN), 'thời gian chờ'),
        ]
        for error, expected in failures:
            with self.subTest(expected=expected), patch('builtins.input', return_value='live'), \
                 patch.object(config.getpass, 'getpass', return_value=TOKEN), \
                 patch.object(config.urllib.request, 'urlopen', side_effect=error):
                with self.assertRaises(ValueError) as result:
                    config.main()
                self.assertIn(expected, str(result.exception))
                self.assertNotIn(TOKEN, str(result.exception))

    def test_invalid_token_never_sent(self):
        with patch('builtins.input', return_value='sandbox'), patch.object(config.getpass, 'getpass', return_value='invalid'), \
             patch.object(config.urllib.request, 'urlopen') as http:
            with self.assertRaises(ValueError):
                config.main()
            http.assert_not_called()


if __name__ == '__main__':
    unittest.main()
