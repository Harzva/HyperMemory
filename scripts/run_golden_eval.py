#!/usr/bin/env python3
"""Validate the offline golden QA suite contract.

The live model/vector evaluation is intentionally kept out of CI so pull
requests do not require API keys or a running Milvus stack. This check protects
the curated regression set itself: every case must have a stable id, supported
mode, meaningful question, expected keywords, and explicit source expectation.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path


def fail(message: str) -> None:
    raise SystemExit(f"golden eval failed: {message}")


def main() -> None:
    path = Path(sys.argv[1] if len(sys.argv) > 1 else "eval/golden/golden_cases.json")
    if not path.exists():
        fail(f"{path} does not exist")

    suite = json.loads(path.read_text(encoding="utf-8"))
    allowed_modes = set(suite.get("allowed_modes", []))
    cases = suite.get("cases", [])

    if not suite.get("suite"):
        fail("suite name is required")
    if not allowed_modes:
        fail("allowed_modes must not be empty")
    if len(cases) < 2:
        fail("at least two golden cases are required")

    seen_ids: set[str] = set()
    for index, case in enumerate(cases, start=1):
        case_id = case.get("id")
        mode = case.get("mode")
        question = case.get("question")
        keywords = case.get("expected_keywords")

        if not case_id or not isinstance(case_id, str):
            fail(f"case #{index} is missing a string id")
        if case_id in seen_ids:
            fail(f"duplicate case id: {case_id}")
        seen_ids.add(case_id)

        if mode not in allowed_modes:
            fail(f"{case_id} uses unsupported mode {mode!r}")
        if not question or len(question.strip()) < 12:
            fail(f"{case_id} question is too short")
        if not isinstance(keywords, list) or len(keywords) < 2:
            fail(f"{case_id} must define at least two expected keywords")
        if not all(isinstance(keyword, str) and keyword.strip() for keyword in keywords):
            fail(f"{case_id} contains an empty expected keyword")
        if "expected_sources" not in case or not isinstance(case["expected_sources"], bool):
            fail(f"{case_id} must set expected_sources to true or false")

    print(f"validated {len(cases)} golden QA cases for {suite['suite']}")


if __name__ == "__main__":
    main()
