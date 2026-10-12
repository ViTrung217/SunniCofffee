#!/usr/bin/env python3
"""Offline validation only. Never writes or regenerates the frozen manifest."""
import hashlib
import json
import sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = "evaluation/chatbot/v1/"
PROTECTED = {
    BASE + "questions.json", BASE + "fixtures.json", BASE + "PROTOCOL.md",
    BASE + "QUESTIONS.md", "data/knowledge/knowledge.v1.json",
    "data/knowledge/sources.v1.json", "data/knowledge/README.md",
    "docs/ai/kho-tri-thuc-v1.md", "docs/ai/pham-vi-chatbot-v1.md",
    "scripts/verify-chatbot-baseline.py",
}


def read(path):
    return json.loads((ROOT / path).read_text(encoding="utf-8"))


def require(condition, message):
    if not condition:
        raise ValueError(message)


def unique(rows):
    ids = [r["id"] for r in rows]
    require(len(ids) == len(set(ids)), "Duplicate IDs")
    return set(ids)


def verify():
    lock = read(BASE + "LOCK.json")
    require(lock["status"] == "frozen", "Baseline is not frozen")
    require(set(lock["sha256"]) == PROTECTED, "Unexpected protected file set")
    for path, expected in lock["sha256"].items():
        actual = hashlib.sha256((ROOT / path).read_bytes()).hexdigest()
        require(actual == expected, f"Frozen content changed: {path}")

    sources = read("data/knowledge/sources.v1.json")["sources"]
    source_ids = unique(sources)
    for s in sources:
        require(bool(s["title"] and s["locator"] and s["reviewed_on"]), "Incomplete source")
        location = s["location"]
        require(location.startswith("https://") or (ROOT / location).is_file(), f"Missing source: {location}")

    entries = read("data/knowledge/knowledge.v1.json")["items"]
    entry_ids = unique(entries)
    require(len(entries) >= 50, "Need at least 50 knowledge entries")
    require(Counter(e["group"] for e in entries) == {"san_pham": 15, "pha_che": 15, "nghiep_vu": 25}, "Wrong knowledge groups")
    for e in entries:
        require(e["content"].strip() and e["source_locator"].strip(), f"Empty entry: {e['id']}")
        require(e["source_refs"] and set(e["source_refs"]) <= source_ids, f"Unknown source: {e['id']}")

    suite = read(BASE + "questions.json")
    cases = suite["cases"]
    require(unique(cases) == {f"Q{i:02}" for i in range(1, 31)}, "Need Q01–Q30 exactly")
    require(suite["status"] == "frozen", "Questions not frozen")
    require(Counter(c["group"] for c in cases) == suite["groups"] == {
        "tu_van_san_pham": 8, "kien_thuc_pha_che": 8,
        "du_lieu_don_hang": 9, "ngoai_pham_vi": 5,
    }, "Wrong evaluation groups")
    fixture = read(BASE + "fixtures.json")
    require(fixture["synthetic"] is True, "Fixture must be synthetic")
    scenarios = fixture["scenarios"]
    for c in cases:
        require(c["scenario"] in scenarios, f"Unknown scenario: {c['id']}")
        require(c["question"].strip() and c["expected_answer"].strip(), f"Missing Q/A: {c['id']}")
        require(c["required_points"] and c["forbidden_points"], f"Missing rubric: {c['id']}")
        require(c["knowledge_refs"] and set(c["knowledge_refs"]) <= entry_ids, f"Unknown knowledge: {c['id']}")
        require(type(c["critical"]) is bool, f"Invalid critical flag: {c['id']}")
    unique(fixture["orders"])
    for order in fixture["orders"]:
        total = sum(i["quantity"] * i["unit_price_vnd"] for i in order["items"]) + order["shipping_fee_vnd"]
        require(total == order["total_vnd"], f"Wrong total: {order['id']}")
    print(f"PASS: {len(entries)} knowledge entries; {len(sources)} sources; 30 questions (8/8/9/5); fixture totals; {len(PROTECTED)} SHA-256 locks.")
    print("This validates baseline integrity, not chatbot quality or live website availability.")


if __name__ == "__main__":
    try:
        verify()
    except (ValueError, KeyError, TypeError, OSError) as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        sys.exit(1)
