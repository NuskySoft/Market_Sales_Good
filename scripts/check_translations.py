#!/usr/bin/env python3
"""Check Spanish and English coverage without an Android SDK. Run from any directory."""
import collections
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'app/src/main/java'
UTILS = SOURCE / 'es/nuskysoftware/marketsales/utils'
TOKEN = re.compile(r'"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*[\s\S]*?\*/')

def uncomment(text):
    return TOKEN.sub(lambda m: '\n' * m[0].count('\n') if m[0].startswith(('//', '/*')) else m[0], text)

def dictionary(language):
    source = uncomment((UTILS / (language + 'Strings.kt')).read_text())
    pairs = re.findall(r'"([^"\n]+)"\s*->\s*"((?:\\.|[^"\\])*)"', source)
    duplicates = [k for k, n in collections.Counter(k for k, _ in pairs).items() if n > 1]
    assert not duplicates, (language, 'duplicate keys', duplicates)
    return dict(pairs)

def main():
    es, en = dictionary('Spanish'), dictionary('English')
    assert es.keys() == en.keys(), ('different key sets', sorted(es.keys() ^ en.keys()))
    for key in es:
        assert es[key] and en[key], ('empty translation', key)
        assert sorted(re.findall(r'\{[^{}]+\}', es[key])) == sorted(re.findall(r'\{[^{}]+\}', en[key])), ('placeholders', key)
    calls = 0
    for path in SOURCE.rglob('*.kt'):
        if path.name in ('SpanishStrings.kt', 'EnglishStrings.kt'):
            continue
        source = uncomment(path.read_text())
        # Only translation helpers, never Bundle.getString or Firestore map getters.
        names = ['StringResourceManager.getString']
        for helper in ('t', 'tr', 'tr1', 'text'):
            if re.search(r'fun\s+' + helper + r'\(', source):
                names.append(helper)
        for name in names:
            for match in re.finditer(r'(?<![\w.])' + re.escape(name) + r'\(\s*"([^"\n]+)"', source):
                key = match[1]
                assert key in es, (str(path.relative_to(ROOT)), source[:match.start()].count('\n') + 1, 'missing key', key)
                calls += 1
        for match in re.finditer(r'StringResourceManager.getString\(\s*"[^"\n]+"\s*,\s*"([^"\n]+)"', source):
            assert match[1] in ('es', 'en'), (str(path), 'invalid language argument', match[1])
    print(json.dumps({'result': 'PASS', 'keys_per_language': len(es), 'literal_calls_checked': calls}, ensure_ascii=False))

if __name__ == '__main__':
    main()
