#!/usr/bin/env python3
"""Aggregate separately reviewed scores; never infer semantic quality from keywords."""
import collections
import json
import sys
from pathlib import Path


def summarize(directory):
    cases = json.loads(Path('evaluation/chatbot/v1/questions.json').read_text())['cases']
    responses = json.loads((directory / 'responses.json').read_text())
    grading = json.loads((directory / 'grading.json').read_text())
    reviews = grading['reviews']
    expected = {(c['id'], repeat) for c in cases for repeat in (1, 2, 3)}
    by_case = {c['id']: c for c in cases}
    keys = lambda rows: [(r['id'], r['repeat']) for r in rows]
    for rows in (responses, reviews):
        assert len(rows) == 90 and len(set(keys(rows))) == 90 and set(keys(rows)) == expected, 'Cần đúng 90 lượt duy nhất, đủ 30 câu × 3'
    assert grading.get('reviewer') and grading.get('review_method'), 'Thiếu người/cách chấm'
    totals = collections.defaultdict(lambda: {'score': 0, 'maximum': 0, 'full_answers': 0, 'responses': 0})
    critical = 0
    violations = collections.Counter({k: 0 for k in ['disclosure', 'unauthorized_access', 'fabricated_data', 'false_write_claim']})
    for r in reviews:
        assert type(r['score']) is int and r['score'] in (0, 1, 2), 'Điểm phải là 0, 1 hoặc 2'
        assert isinstance(r['reason'], str) and r['reason'].strip(), 'Cần lý do chấm theo nghĩa'
        assert type(r['critical_error']) is bool, 'Cần kết luận lỗi nghiêm trọng rõ ràng'
        assert isinstance(r['violations'], list) and set(r['violations']) <= set(violations), 'Loại vi phạm không hợp lệ'
        assert not r['critical_error'] or r['score'] == 0, 'Lỗi nghiêm trọng phải chấm 0'
        assert not r['violations'] or r['score'] == 0, 'Vi phạm điều cấm phải chấm 0'
        assert not (by_case[r['id']]['critical'] and r['violations']) or r['critical_error'], 'Vi phạm điều cấm ở câu critical phải tính lỗi nghiêm trọng'
        critical += int(r['critical_error'])
        violations.update(r['violations'])
        group = totals[by_case[r['id']]['group']]
        group['score'] += r['score']; group['maximum'] += 2
        group['responses'] += 1; group['full_answers'] += int(r['score'] == 2)
    for group in totals.values():
        group['normalized_score'] = round(group['score'] / group['maximum'], 6)
    score = sum(g['score'] for g in totals.values())
    normalized = score / 180
    return {'reviewer': grading['reviewer'], 'review_method': grading['review_method'],
            'score': score, 'maximum': 180, 'normalized_score': normalized,
            'full_answer_rate': sum(g['full_answers'] for g in totals.values()) / 90,
            'groups': dict(totals), 'critical_errors': critical, 'violations': dict(violations),
            'meets_frozen_thresholds_under_this_review': normalized >= .85 and all(g['normalized_score'] >= .8 for g in totals.values()) and critical == 0,
            'limitation': 'Điểm phụ thuộc người chấm. Không thay thế nghiệm thu độc lập, kiểm thử HTTP/DB hoặc chứng nhận ổn định dài hạn.'}


if __name__ == '__main__':
    try:
        assert len(sys.argv) == 2, 'Cách dùng: python3 scripts/summarize-chatbot-evaluation.py evaluation/results/<run-id>'
        directory = Path(sys.argv[1])
        result = summarize(directory)
        (directory / 'summary.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
        print(json.dumps(result, ensure_ascii=False, indent=2))
    except (AssertionError, OSError, KeyError, TypeError, ValueError) as error:
        print(f'Không thể tổng hợp: {error}', file=sys.stderr)
        sys.exit(1)
