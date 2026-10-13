#!/usr/bin/env python3
"""Validate a reviewed product collection. Read only; never imports or rewrites data."""
import datetime as dt
import json
import re
import sys
from pathlib import Path


def validate(data):
    errors = []

    def check(ok, path, message):
        if not ok:
            errors.append(f"{path}: {message}")

    def text(obj, key, maximum, path, required=True):
        value = obj.get(key)
        check(isinstance(value, str) and (not required or bool(value.strip()))
              and len(value) <= maximum, f"{path}.{key}", f"cần chuỗi {'không trống, ' if required else ''}tối đa {maximum} ký tự")
        return value if isinstance(value, str) else ""

    def date(obj, key, path):
        try:
            raw = obj.get(key)
            assert isinstance(raw, str) and re.fullmatch(r"\d{4}-\d{2}-\d{2}", raw)
            value = dt.date.fromisoformat(raw)
            assert value <= dt.date.today()
        except (AssertionError, TypeError, ValueError):
            errors.append(f"{path}.{key}: cần ngày YYYY-MM-DD hợp lệ, không ở tương lai")

    if not isinstance(data, dict):
        return ["root: cần đối tượng JSON"]
    check(type(data.get("version")) is int and data["version"] == 1, "version", "phải là 1")
    category_codes, category_names, skus = set(), set(), set()
    for group in ("categories", "products"):
        rows = data.get(group)
        if not isinstance(rows, list) or not rows:
            errors.append(f"{group}: cần danh sách không trống")
            continue
        for i, row in enumerate(rows):
            path = f"{group}[{i}]"
            if not isinstance(row, dict):
                errors.append(f"{path}: cần đối tượng JSON")
                continue
            if group == "categories":
                code = text(row, "code", 30, path)
                name = text(row, "name", 100, path)
                text(row, "description", 1000, path, False)
                check(bool(re.fullmatch(r"[A-Z0-9_-]{2,30}", code)), path + ".code", "mã gồm 2–30 chữ in hoa, số, dấu gạch")
                check(code not in category_codes, path + ".code", "mã bị trùng")
                check(name.casefold() not in category_names, path + ".name", "tên bị trùng")
                category_codes.add(code)
                category_names.add(name.casefold())
                continue
            sku = text(row, "sku", 50, path)
            check(bool(re.fullmatch(r"[A-Z0-9_-]{2,50}", sku)) and not sku.startswith("DEMO-"), path + ".sku", "SKU hợp lệ, không dùng tiền tố DEMO-")
            check(sku not in skus, path + ".sku", "SKU bị trùng")
            skus.add(sku)
            text(row, "name", 160, path)
            text(row, "description", 2000, path)
            text(row, "sales_unit", 30, path)
            category = text(row, "category_code", 30, path)
            check(category in category_codes, path + ".category_code", "danh mục chưa khai báo")
            for key, low, high in (("price_vnd", 1, 999_999_999_999), ("opening_stock", 0, 2_147_483_647)):
                value = row.get(key)
                check(type(value) is int and low <= value <= high, f"{path}.{key}", f"cần số nguyên trong {low}..{high}")
            check(type(row.get("active")) is bool, path + ".active", "cần true hoặc false")
            text(row, "source", 2000, path)
            text(row, "verified_by", 190, path)
            date(row, "verified_on", path)
            date(row, "stock_counted_on", path)
            image = text(row, "image_file", 1000, path, False)
            text(row, "image_rights", 1000, path, bool(image))
    return errors


def main():
    if len(sys.argv) != 2:
        print("Cách dùng: python3 scripts/validate-catalog.py /đường/dẫn/catalog.json", file=sys.stderr)
        return 2
    try:
        path = Path(sys.argv[1])
        if path.stat().st_size > 5_000_000:
            raise ValueError()
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError, UnicodeError):
        print("Không đọc được JSON hợp lệ (giới hạn 5 MB).", file=sys.stderr)
        return 2
    errors = validate(data)
    if errors:
        print("Chưa đủ điều kiện nhập:\n- " + "\n- ".join(errors), file=sys.stderr)
        return 1
    print(f"Đạt kiểm tra cấu trúc: {len(data['categories'])} danh mục, {len(data['products'])} sản phẩm. Chưa ghi vào cơ sở dữ liệu; cần đối chiếu nguồn trước khi nhập qua Quản trị.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
