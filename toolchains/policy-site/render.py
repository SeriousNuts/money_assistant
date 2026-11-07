#!/usr/bin/env python3
"""Render the in-app Russian policy as the same static HTTPS page."""
from html import escape
from pathlib import Path

root = Path(__file__).resolve().parents[2]
text = (root / "app/src/modern/res/raw/privacy.txt").read_text(encoding="utf-8")
target = root / "build/verification/policy-site/site/privacy/index.html"
target.parent.mkdir(parents=True, exist_ok=True)
target.write_text("""<!doctype html>
<html lang="ru">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Политика конфиденциальности Money Assistant</title>
  <link rel="canonical" href="https://moneyassistantpolicy.duckdns.org/privacy/">
  <style>
    body { max-width: 50rem; margin: 2rem auto; padding: 0 1rem; color: #202124;
           background: #fff; font: 1rem/1.6 system-ui, sans-serif; }
    pre { font: inherit; white-space: pre-wrap; overflow-wrap: anywhere; }
  </style>
</head>
<body><main><pre>""" + escape(text) + """</pre></main></body>
</html>
""", encoding="utf-8")
print(target)
