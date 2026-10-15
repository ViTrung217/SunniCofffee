#!/usr/bin/env python3
import importlib.util
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('configure_google', Path(__file__).with_name('configure-google.py'))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class ConfigurationTest(unittest.TestCase):
    def test_private_file_and_preserves_other_credentials(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / '.runtime').mkdir()
            other = root / '.runtime' / 'sepay.properties'
            other.write_text('untouched')
            with patch.object(module, '__file__', str(root / 'scripts' / 'configure-google.py')), patch('builtins.input', return_value='123-local.apps.googleusercontent.com'), patch.object(module.getpass, 'getpass', return_value='GOCSPX-test-secret-only'), patch('builtins.print') as printed:
                module.main()
            saved = root / '.runtime' / 'google.properties'
            self.assertEqual(saved.stat().st_mode & 0o777, 0o600)
            self.assertIn('app.google.enabled=true', saved.read_text())
            self.assertEqual(other.read_text(), 'untouched')
            self.assertNotIn('GOCSPX-test-secret-only', str(printed.call_args_list))

    def test_rejects_property_injection(self):
        with patch('builtins.input', return_value='123.apps.googleusercontent.com'), patch.object(module.getpass, 'getpass', return_value='secret\napp.other=true'):
            with self.assertRaises(ValueError):
                module.main()


if __name__ == '__main__':
    unittest.main()
