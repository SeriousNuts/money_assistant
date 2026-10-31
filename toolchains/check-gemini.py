#!/usr/bin/env python3
"""Check Gemini access with a synthetic blank image; never prints or saves the key."""
import base64
import getpass
import json
import re
import struct
import sys
import urllib.error
import urllib.request
import zlib


def chunk(kind, data):
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)


def blank_png():
    width = height = 32
    rows = (b"\0" + b"\xff\xff\xff" * width) * height
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">2I5B", width, height, 8, 2, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(rows)) + chunk(b"IEND", b""))


def main():
    if not sys.stdin.isatty():
        print("Запустите в обычном терминале: python3 toolchains/check-gemini.py", file=sys.stderr)
        return 2
    key = getpass.getpass("Новый ключ Gemini из AI Studio (ввод скрыт): ").strip()
    if not key:
        print("Ключ не введён.")
        return 2
    schema = {"type": "OBJECT", "properties": {"items": {"type": "ARRAY", "items": {
        "type": "OBJECT", "properties": {"name": {"type": "STRING"}, "price": {"type": "STRING"}},
        "required": ["name", "price"]}}}, "required": ["items"]}
    body = json.dumps({"contents": [{"parts": [
        {"text": "Это пустое тестовое изображение без чека. Верни пустой список items."},
        {"inline_data": {"mime_type": "image/png", "data": base64.b64encode(blank_png()).decode("ascii")}}
    ]}], "generationConfig": {"responseMimeType": "application/json", "responseSchema": schema,
                             "maxOutputTokens": 8192}}).encode("utf-8")
    request = urllib.request.Request(
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent",
        data=body, headers={"Content-Type": "application/json", "x-goog-api-key": key})
    try:
        with urllib.request.urlopen(request, timeout=70) as response:
            result = json.load(response)
            text = result["candidates"][0]["content"]["parts"][0]["text"]
            items = json.loads(text)["items"]
            print("HTTP 200: доступ к модели, изображение и JSON-формат ответа работают.")
            print("Позиций на пустой картинке:", len(items))
            return 0
    except urllib.error.HTTPError as error:
        try:
            info = json.load(error).get("error", {})
        except (ValueError, OSError):
            info = {}
        status = info.get("status", "")
        reason = next((entry.get("reason", "") for entry in info.get("details", []) if "reason" in entry), "")
        # Do not print provider messages: they can contain request data or credentials.
        safe = lambda value: value if isinstance(value, str) and re.fullmatch(r"[A-Z_]{1,50}", value) else "UNKNOWN"
        print("HTTP", error.code, "статус:", safe(status), "причина:", safe(reason))
        return 1
    except (OSError, ValueError, KeyError, IndexError) as error:
        print("Сбой сети или формата ответа:", type(error).__name__)
        return 1


if __name__ == "__main__":
    sys.exit(main())
